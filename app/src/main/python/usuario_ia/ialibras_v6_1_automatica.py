"""Entrada Android adaptada do ialibras_v6_1_automatica.py enviado pelo usuário.

Conserva GestureRouter, StaticConsensus, normalização e regras de realtime().
A câmera/teclado/janelas do PC são substituídos pelos frames e botões do app.
O original sem alterações está em referencia_pc na raiz do projeto.
"""
import hashlib
import os
import json
from pathlib import Path
os.environ.setdefault('OPENBLAS_NUM_THREADS','1')
import numpy as np
from .libras_core import GestureRouter, StaticConsensus, normalize_static, normalize_dynamic
from .pesos_android import Floresta, RedeGRU

_motor = None


class LibrasAndroid:
    def __init__(self, folder):
        folder = Path(folder)
        manifest = json.loads((folder/'manifest.json').read_text(encoding='utf-8'))
        if manifest['format'] != 1:
            raise ValueError('Versão de pesos não suportada. Use preparar_modelos.py.')
        names = ('static.bin','dynamic.bin','libras_static_labels_v5.json','libras_dynamic_labels_v5.json')
        for name in names:
            if hashlib.sha256((folder/name).read_bytes()).hexdigest() != manifest['sha256'][name]:
                raise ValueError('Pesos incompletos ou alterados: '+name)
        self.static_labels = json.loads((folder/'libras_static_labels_v5.json').read_text(encoding='utf-8'))
        self.dynamic_labels = json.loads((folder/'libras_dynamic_labels_v5.json').read_text(encoding='utf-8'))
        self.static_model = Floresta(folder/'static.bin')
        self.dynamic_model = RedeGRU(folder/'dynamic.bin')
        if len(self.static_labels) != self.static_model.classes or len(self.dynamic_labels) != self.dynamic_model.classes:
            raise ValueError('Modelos e labels não correspondem.')
        if not all(isinstance(v,str) and v for v in self.static_labels+self.dynamic_labels):
            raise ValueError('Labels inválidos.')
        self.router = GestureRouter()
        # Mesmo limiar da versão V5 no realtime() do arquivo enviado.
        self.smoother = StaticConsensus(min_confidence=0.84)
        self.words = []
        self.label = ''
        self.confidence = 0.0
        self.last_added_label = None
        self.last_added_time = 0.0
        self.no_hand_streak = 0
        self.version = manifest.get('commit','local')

    def add_to_sentence(self, label, score, kind, now):
        if score < (0.84 if kind == 'GESTO/PALAVRA' else 0.82):
            return
        if label.upper() in ('NEUTRO','NENHUM','SEM_SINAL'):
            return
        if label == self.last_added_label and now-self.last_added_time < 1.2:
            return
        if now-self.last_added_time < 0.45:
            return
        text = label.replace('_',' ').lower()
        if len(' '.join(self.words))+len(text)+(1 if self.words else 0) <= 2000:
            self.words.append(text)
        self.last_added_label = label
        self.last_added_time = now

    def reconhecer(self, frame):
        raw = np.asarray(frame['pontos'],np.float32)
        now = frame['tempo_s']
        mode = frame['modo']
        if frame['maos_detectadas']:
            self.no_hand_streak = 0
            event = self.router.feed(raw,now,mode)
            if event and event.kind == 'static':
                probs = self.static_model.predict_proba(normalize_static(event.features))
                result = self.smoother.update(self.static_labels,probs)
                if result:
                    label, score = result
                    self.label = label
                    self.confidence = float(score)
                    self.add_to_sentence(label.upper(),float(score),'LETRA/NUMERO',now)
            elif event and event.kind == 'dynamic':
                self.smoother.reset()
                probs = self.dynamic_model.predict_proba(normalize_dynamic(event.features))
                winner = int(np.argmax(probs))
                top = float(probs[winner])
                second = float(np.partition(probs,-2)[-2]) if len(probs)>1 else 0.0
                label = self.dynamic_labels[winner]
                if top >= 0.90 and top-second >= 0.25 and label.upper() not in ('NEUTRO','NENHUM','SEM_SINAL'):
                    self.label = label.replace('_',' ')
                    self.confidence = top
                    self.add_to_sentence(label,top,'GESTO/PALAVRA',now)
                else:
                    self.label = 'Sinal não confirmado'
                    self.confidence = 0.0
        else:
            self.no_hand_streak += 1
            if self.no_hand_streak >= 2:
                self.router.feed(None,now,mode)
                self.smoother.reset()
        return {'texto': ' '.join(self.words), 'rotulo': self.label, 'confianca': float(self.confidence)}


def carregar(contexto):
    global _motor
    _motor = LibrasAndroid(contexto['modelos_dir'])
    return {'nome': 'Sua IA Libras V6.1 (Python)', 'versao': 'v6.1-'+_motor.version[:12]}


def reconhecer(frame):
    if _motor is None:
        raise RuntimeError('Carregue os modelos antes de reconhecer.')
    return _motor.reconhecer(frame)


def encerrar():
    global _motor
    _motor = None
