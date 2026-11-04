"""Testes do contrato de integração. Não contêm modelo nem reconhecimento de Libras."""
import importlib
import json
import sys
import types
import unittest
from array import array
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'app/src/main/python'))
from connecta_bridge import Sessao


class BridgeTest(unittest.TestCase):
    def setUp(self):
        import usuario_ia.minha_ia
        self.original = sys.modules['usuario_ia.minha_ia']
        self.closed = 0
        self.frame = None
        module = types.ModuleType('usuario_ia.minha_ia')
        module.__file__ = str(ROOT/'app/src/main/python/usuario_ia/minha_ia.py')
        module.carregar = lambda ctx: {'nome': 'Teste de integração', 'versao': 'teste'}
        def reconhecer(frame):
            self.frame = frame
            return self.result
        def encerrar():
            self.closed += 1
        module.reconhecer, module.encerrar = reconhecer, encerrar
        sys.modules['usuario_ia.minha_ia'] = module
        self.result = None
        self.module = module

    def tearDown(self):
        sys.modules['usuario_ia.minha_ia'] = self.original

    def feed(self, session):
        return json.loads(session.reconhecer_json(array('b', [0, -1, -128]), 1, 1, [0]*126, 1000, 'AUTO', False))

    def test_signed_bytes_and_full_sentence(self):
        s = Sessao('/dados');self.result = {'texto': 'resultado do teste', 'rotulo': 'teste', 'confianca': .91}
        r = self.feed(s)
        self.assertEqual(self.frame['rgb'], bytes([0,255,128]))
        self.assertEqual(self.frame['tempo_s'], 1)
        self.assertEqual(r['texto'], 'resultado do teste');self.assertTrue(r['novo'])
        self.assertFalse(self.feed(s)['novo'])
        self.result = None;self.assertEqual(self.feed(s)['texto'], 'resultado do teste')
        s.encerrar();s.encerrar();self.assertEqual(self.closed, 1)
        with self.assertRaises(RuntimeError):self.feed(s)

    def test_model_load_error_is_preserved(self):
        def fail(ctx):
            raise RuntimeError('Pesos ausentes')
        self.module.carregar = fail
        with self.assertRaisesRegex(RuntimeError, 'Pesos ausentes'):Sessao('/dados')

    def test_bad_contract(self):
        self.module.reconhecer = None
        with self.assertRaisesRegex(TypeError, 'reconhecer'):Sessao('/dados')

    def test_bad_results(self):
        s = Sessao('/dados')
        for bad in ({}, 4, {'texto': 1}, {'texto':'x', 'confianca':float('nan')}, {'texto':'x', 'confianca':2}, {'texto':'x', 'confianca':True}, {'texto':'x'*2001}):
            self.result = bad
            with self.assertRaises((ValueError,TypeError)):self.feed(s)

    def test_string_result(self):
        s = Sessao('/dados');self.result = 'texto completo'
        self.assertEqual(self.feed(s)['texto'],'texto completo')

    def test_bad_frames(self):
        s = Sessao('/dados')
        for features in ([0]*125, [float('inf')]*126):
            with self.assertRaises(ValueError):s.reconhecer_json(b'123',1,1,features,100,'AUTO',False)
        with self.assertRaises(ValueError):s.reconhecer_json(b'12',1,1,[0]*126,100,'AUTO',False)

    def test_cleanup_when_loading_fails(self):
        self.module.carregar = lambda ctx: 'contrato inválido'
        with self.assertRaisesRegex(TypeError,'dicionário'):Sessao('/dados')
        self.assertEqual(self.closed, 1)


if __name__ == '__main__':unittest.main()
