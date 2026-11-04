"""Executa os pesos V5 em Python/NumPy, sem sklearn/TensorFlow no Android.

Os arquivos binários são uma exportação dos modelos originais, sem retreinamento.
As árvores preservam limiares float64 e as folhas probabilísticas; a GRU preserva
kernels/bias e as equações Keras reset_after=True (z, r, h).
"""
import struct
import numpy as np


class Leitor:
    def __init__(self, caminho):
        self.data = memoryview(caminho.read_bytes())
        self.pos = 0

    def inteiros(self, n):
        if self.pos+4*n > len(self.data):
            raise ValueError('Arquivo de pesos incompleto.')
        out = struct.unpack_from('>'+'i'*n, self.data, self.pos)
        self.pos += 4*n
        return out

    def matriz(self, shape):
        n = int(np.prod(shape))
        if self.pos+4*n > len(self.data):
            raise ValueError('Matriz de pesos incompleta.')
        out = np.frombuffer(self.data, dtype='>f4', count=n, offset=self.pos).astype(np.float32).reshape(shape)
        self.pos += 4*n
        if not np.isfinite(out).all():
            raise ValueError('Modelo contém pesos não finitos.')
        return out

    def finalizar(self):
        if self.pos != len(self.data):
            raise ValueError('Bytes extras no arquivo de pesos.')


class Floresta:
    def __init__(self, caminho):
        r = Leitor(caminho)
        magic, features, self.classes, trees = r.inteiros(4)
        if magic != 0x43535431 or features != 126 or not 0 < self.classes <= 1000 or not 0 < trees <= 1000:
            raise ValueError('Formato de árvores incompatível.')
        dtype = np.dtype([('feature','>i4'), ('left','>i4'), ('right','>i4'),
                          ('threshold','>f8'), ('offset','>i4'), ('count','>i4')])
        leaf_dtype = np.dtype([('class','>i4'), ('prob','>f4')])
        self.trees = []
        for _ in range(trees):
            nodes, pairs = r.inteiros(2)
            if not 0 < nodes <= 1_000_000 or not 0 < pairs <= 10_000_000:
                raise ValueError('Dimensões de árvore inválidas.')
            needed = nodes*dtype.itemsize + pairs*leaf_dtype.itemsize
            if r.pos+needed > len(r.data):
                raise ValueError('Árvore incompleta.')
            records = np.frombuffer(r.data, dtype=dtype, count=nodes, offset=r.pos)
            r.pos += nodes*dtype.itemsize
            probabilities = np.frombuffer(r.data, dtype=leaf_dtype, count=pairs, offset=r.pos)
            r.pos += pairs*leaf_dtype.itemsize
            # Evita ciclos, índices fora da memória e probabilidades inválidas.
            indices = np.arange(nodes)
            branches = records['left'] >= 0
            if np.any((records['left'][branches] <= indices[branches]) | (records['left'][branches] >= nodes)):
                raise ValueError('Ramo esquerdo inválido.')
            if np.any((records['right'][branches] <= indices[branches]) | (records['right'][branches] >= nodes)):
                raise ValueError('Ramo direito inválido.')
            if np.any((records['feature'][branches] < 0) | (records['feature'][branches] >= 126)):
                raise ValueError('Atributo de árvore inválido.')
            if np.any((records['offset'] < 0) | (records['count'] < 0) | (records['offset']+records['count'] > pairs)):
                raise ValueError('Folha inválida.')
            if not np.isfinite(probabilities['prob']).all() or np.any((probabilities['class'] < 0) | (probabilities['class'] >= self.classes)):
                raise ValueError('Probabilidades inválidas.')
            # Converte para arrays nativos; inferência visita apenas um caminho por árvore.
            self.trees.append((records['feature'].astype(np.int32), records['left'].astype(np.int32),
                               records['right'].astype(np.int32), records['threshold'].astype(np.float64),
                               records['offset'].astype(np.int32), records['count'].astype(np.int32),
                               probabilities['class'].astype(np.int32), probabilities['prob'].astype(np.float32)))
        r.finalizar()

    def predict_proba(self, features):
        x = np.asarray(features, np.float32)
        if x.shape != (126,) or not np.isfinite(x).all():
            raise ValueError('Esperado frame com 126 atributos finitos.')
        out = np.zeros(self.classes, np.float64)
        for feature, left, right, threshold, offset, count, classes, probs in self.trees:
            node = 0
            while left[node] >= 0:
                node = int(left[node] if x[feature[node]] <= threshold[node] else right[node])
            start = int(offset[node]);end = start+int(count[node])
            np.add.at(out, classes[start:end], probs[start:end])
        return np.asarray(out/len(self.trees), np.float32)


class RedeGRU:
    def __init__(self, caminho):
        r = Leitor(caminho)
        magic, frames, features, self.classes, n = r.inteiros(5)
        if magic != 0x43445931 or (frames, features, n) != (48,126,4) or not 0 < self.classes <= 1000:
            raise ValueError('Formato GRU incompatível.')
        self.layers = []
        expected_input = 126
        for index in range(n):
            kind, inputs, units, activation = r.inteiros(4)
            if not 0 < units <= 512 or inputs != expected_input or (kind,activation) != ((1,1),(1,0),(2,1),(2,2))[index]:
                raise ValueError('Arquitetura dinâmica incompatível.')
            if kind == 1:
                weights = (r.matriz((inputs,3*units)), r.matriz((units,3*units)), r.matriz((2,3*units)))
            else:
                weights = (r.matriz((inputs,units)), r.matriz((units,)))
            self.layers.append((kind, units, activation, weights))
            expected_input = units
        if expected_input != self.classes:
            raise ValueError('Saída dinâmica incompatível com labels.')
        r.finalizar()

    @staticmethod
    def _sigmoid(x):
        return 1/(1+np.exp(-np.clip(x,-80,80)))

    def predict_proba(self, sequence):
        x = np.asarray(sequence,np.float32)
        if x.shape != (48,126) or not np.isfinite(x).all():
            raise ValueError('Esperada sequência finita 48x126.')
        for kind, units, activation, weights in self.layers:
            if kind == 1:
                kernel, recurrent, bias = weights
                h = np.zeros(units,np.float32)
                output = []
                for frame in x:
                    a = frame @ kernel + bias[0]
                    r = h @ recurrent + bias[1]
                    z = self._sigmoid(a[:units]+r[:units])
                    reset = self._sigmoid(a[units:2*units]+r[units:2*units])
                    candidate = np.tanh(a[2*units:]+reset*r[2*units:])
                    h = z*h+(1-z)*candidate
                    if activation == 1:
                        output.append(h)
                x = np.asarray(output,np.float32) if activation == 1 else h
            else:
                kernel, bias = weights
                x = x @ kernel + bias
                if activation == 1:
                    x = np.maximum(x,0)
                else:
                    exp = np.exp(x-np.max(x))
                    x = exp/np.sum(exp)
        return np.asarray(x,np.float32)
