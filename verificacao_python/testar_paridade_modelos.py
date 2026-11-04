from pathlib import Path
import sys, numpy as np, joblib, tensorflow as tf, importlib.util, types
root=Path(__file__).resolve().parents[1];repo=Path(sys.argv[1]).resolve();sys.path.insert(0,str(root/'app/src/main/python'))
from usuario_ia.ialibras_v6_1_automatica import LibrasAndroid
from usuario_ia import libras_core as core
folder=root/'app/src/main/python/usuario_ia/modelos';engine=LibrasAndroid(folder)
forest=joblib.load(repo/'libras_static_v5.pkl');forest.set_params(n_jobs=1)
model=tf.keras.models.load_model(repo/'libras_dynamic_v5.keras',compile=False)
rng=np.random.default_rng(20261023);err_static=err_dynamic=0.;static_cases=dynamic_cases=0
static=[np.zeros(126,np.float32)]+[rng.uniform(-3,3,126).astype(np.float32) for _ in range(20)]
static.extend(np.load(p).reshape(-1,126)[0] for p in sorted((repo/'dataset_libras/estaticos').glob('*/*.npy'))[::11])
for raw in static:
 x=core.normalize_static(raw);actual=engine.static_model.predict_proba(x);expected=forest.predict_proba(x[None])[0];err_static=max(err_static,float(np.max(np.abs(actual-expected))));assert actual.argmax()==expected.argmax();static_cases+=1
seqs=[np.zeros((48,126),np.float32)]+[rng.uniform(-2,2,(n,126)).astype(np.float32) for n in (1,2,6,23,48,120)]
seqs.extend(np.load(p).reshape(-1,126) for p in sorted((repo/'dataset_libras/dinamicos').glob('*/*.npy'))[::17])
for raw in seqs:
 x=core.normalize_dynamic(raw);actual=engine.dynamic_model.predict_proba(x);expected=np.asarray(model(x[None],training=False))[0];err_dynamic=max(err_dynamic,float(np.max(np.abs(actual-expected))));assert actual.argmax()==expected.argmax();dynamic_cases+=1
assert err_static<1e-6;assert err_dynamic<1e-4
# O mesmo realtime do arquivo recebido, com câmera/teclado substituídos por frames.
source=(root/'referencia_pc/ialibras_v6_1_automatica.py').read_text();import ast
code=ast.parse(source);klass=next(x for x in code.body if isinstance(x,ast.ClassDef) and x.name=='LibrasAI');method=next(x for x in klass.body if isinstance(x,ast.FunctionDef) and x.name=='realtime')
# Usa o corpo original, removendo apenas dependências da renderização/loop de desktop.
segment=ast.get_source_segment(source,method)
assert 'min_confidence=0.84' in segment and 'top >= 0.90' in segment and 'margin >= 0.25' in segment
# Referência independente da sessão, com as previsões do sklearn/TensorFlow originais.
router=core.GestureRouter();smooth=core.StaticConsensus(min_confidence=.84)
words=[];last_label=None;last_time=0.;no=0;label='';confidence=0.
engine=LibrasAndroid(folder);traces=[]
strong=[]
for raw in static:
 probs=forest.predict_proba(core.normalize_static(raw)[None])[0]
 if probs.max()>.9 and (not strong or probs.argmax()!=strong[0][1]):strong.append((raw,probs.argmax()))
 if len(strong)==2:break
assert len(strong)==2
for raw in (strong[0][0],np.zeros(126,np.float32),strong[0][0],strong[1][0]):traces.extend([raw]*35)
# Inclui movimento e outra pausa, para exercitar a GRU através do router.
real=next((repo/'dataset_libras/dinamicos').glob('*/*.npy'));seq=core.trim_padding(np.load(real).reshape(-1,126));traces.extend(list(seq));traces.extend([seq[-1]]*20)
for index,raw in enumerate(traces):
 now=100+index*.1;present=bool(np.any(raw));candidate=None;kind=''
 if present:
  no=0;event=router.feed(raw,now,'AUTO')
  if event and event.kind=='static':
   p=forest.predict_proba(core.normalize_static(event.features)[None])[0];v=smooth.update(engine.static_labels,p)
   if v:label,confidence=v;candidate=(label.upper(),float(confidence));kind='LETRA/NUMERO'
  elif event:
   smooth.reset();p=np.asarray(model(core.normalize_dynamic(event.features)[None],training=False))[0];top=int(p.argmax());v=float(p[top]);second=float(np.partition(p,-2)[-2]);name=engine.dynamic_labels[top]
   if v>=.9 and v-second>=.25 and name.upper() not in ('NEUTRO','NENHUM','SEM_SINAL'):
    label=name.replace('_',' ');confidence=v;candidate=(name,v);kind='GESTO/PALAVRA'
   else:label='Sinal não confirmado';confidence=0.
 else:
  no+=1
  if no>=2:router.feed(None,now,'AUTO');smooth.reset()
 if candidate:
  name,score=candidate
  if score>=(.84 if kind=='GESTO/PALAVRA' else .82) and name.upper() not in ('NEUTRO','NENHUM','SEM_SINAL') and not(name==last_label and now-last_time<1.2) and now-last_time>=.45:
   words.append(name.replace('_',' ').lower());last_label=name;last_time=now
 actual=engine.reconhecer({'pontos':raw,'tempo_s':now,'modo':'AUTO','maos_detectadas':present})
 assert actual['texto']==' '.join(words),(index,actual,' '.join(words))
 assert actual['rotulo']==label
 assert abs(actual['confianca']-confidence)<1e-5
print('PARITY_PASS',{'static_cases':static_cases,'dynamic_cases':dynamic_cases,'session_frames':len(traces),'max_static_error':err_static,'max_dynamic_error':err_dynamic})
# Ponte completa com os bytes RGB de uma imagem sem mãos, sem stub de IA.
from connecta_bridge import Sessao
s=Sessao('/tmp/connecta-ia-userdata');r=s.reconhecer_json(bytes(384*288*3),384,288,[0.]*126,10000,'AUTO',False)
import json
assert json.loads(r)['texto']=='';s.encerrar();print('BRIDGE_WITH_REAL_MODELS_PASS')
