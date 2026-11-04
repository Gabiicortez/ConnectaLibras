"""Ponte Android/Python. Não contém reconhecimento nem modelos substitutos."""
import hashlib
import importlib
import json
import math
from pathlib import Path


class Sessao:
    def __init__(self, dados_dir):
        try:
            self.modulo = importlib.import_module('usuario_ia.minha_ia')
        except ModuleNotFoundError as exc:
            raise RuntimeError(
                'Módulo Python não encontrado: ' + str(exc.name) + '. '
                'Confira usuario_ia/minha_ia.py e requirements_android.txt. '
                'Dependências do PC precisam ser compatíveis com Android.'
            ) from exc
        self.fechada = False
        self.texto = ''
        self.rotulo = ''
        self.confianca = 0.0
        for name in ('carregar', 'reconhecer'):
            if not callable(getattr(self.modulo, name, None)):
                raise TypeError('Sua IA precisa implementar a função ' + name + '. Veja COLOCAR_MINHA_IA.md.')
        pasta = Path(self.modulo.__file__).parent
        try:
            info = self.modulo.carregar({'modelos_dir': str(pasta/'modelos'), 'dados_dir': str(dados_dir)})
            if not isinstance(info, dict):
                raise TypeError('carregar(contexto) deve retornar um dicionário com nome e versao.')
            self.nome = _texto(info.get('nome', 'Minha IA Python'), 'nome', 120)
            self.versao = _texto(info.get('versao', 'sem-versao'), 'versao', 120)
            try:
                assinatura = hashlib.sha256(Path(self.modulo.__file__).read_bytes()).hexdigest()[:12]
            except OSError:
                assinatura = 'fonte'
            self.identificador = 'python:' + assinatura + ':' + self.versao
        except BaseException:
            try:
                self.encerrar()
            except Exception:
                pass  # Preserve o erro original de carregamento.
            raise

    def info_json(self):
        return json.dumps({'nome': self.nome, 'versao': self.identificador}, ensure_ascii=False)

    def reconhecer_json(self, rgb_java, largura, altura, pontos_java, tempo_ms, modo, maos):
        if self.fechada:
            raise RuntimeError('Sessão da IA já encerrada.')
        if modo not in ('AUTO', 'ESTATICO', 'DINAMICO'):
            raise ValueError('Modo inválido.')
        # memoryview preserva os valores 0..255 dos bytes Java, inclusive bytes negativos.
        rgb = memoryview(rgb_java).tobytes()
        pontos = [float(v) for v in pontos_java]
        if len(pontos) != 126 or not all(math.isfinite(v) for v in pontos):
            raise ValueError('Esperado um vetor finito de 126 pontos.')
        if largura <= 0 or altura <= 0 or len(rgb) != largura*altura*3:
            raise ValueError('Frame RGB inválido.')
        frame = {'rgb': rgb, 'largura': int(largura), 'altura': int(altura),
                 'pontos': pontos, 'tempo_s': float(tempo_ms)/1000,
                 'modo': modo, 'maos_detectadas': bool(maos)}
        result = self.modulo.reconhecer(frame)
        anterior = self.texto
        if isinstance(result, str):
            result = {'texto': result}
        if result is not None:
            if not isinstance(result, dict) or 'texto' not in result:
                raise TypeError('reconhecer(frame) deve retornar uma string, None ou dict com texto.')
            texto = _texto(result['texto'], 'texto', 2000)
            rotulo = _texto(result.get('rotulo', ''), 'rotulo', 200)
            confianca = result.get('confianca', 0.0)
            if isinstance(confianca, bool) or not isinstance(confianca, (int, float)):
                raise TypeError('confianca deve ser um número entre 0 e 1.')
            confianca = float(confianca)
            if not math.isfinite(confianca) or not 0 <= confianca <= 1:
                raise ValueError('confianca deve ser finita e estar entre 0 e 1.')
            self.texto, self.rotulo, self.confianca = texto, rotulo, confianca
        return json.dumps({'texto': self.texto, 'rotulo': self.rotulo,
                           'confianca': self.confianca, 'maos': bool(maos),
                           'novo': self.texto != anterior}, ensure_ascii=False, allow_nan=False)

    def encerrar(self):
        if not self.fechada:
            self.fechada = True
            callback = getattr(self.modulo, 'encerrar', None)
            if callable(callback):
                callback()


def _texto(value, campo, limite):
    if not isinstance(value, str):
        raise TypeError(campo + ' deve ser texto.')
    if len(value) > limite:
        raise ValueError(campo + ' ultrapassa ' + str(limite) + ' caracteres.')
    return value
