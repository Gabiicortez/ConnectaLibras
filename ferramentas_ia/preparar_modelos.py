"""Exporta os pesos V5 do Connecta para o motor Java local. Não retreina modelos.
Uso: python exportar_android.py --origem CAMINHO_REPO --destino ../app/src/main/assets/ia
Execute somente com os seus próprios arquivos de modelo (joblib carrega código Python).
"""
import argparse, hashlib, json, shutil, struct, subprocess, tempfile, os, ast
from pathlib import Path
import numpy as np
import joblib


def integer(out, *v): out.write(struct.pack('>'+str(len(v))+'i',*v))
def floats(out, array): out.write(np.asarray(array,dtype='>f4').tobytes())


def _exportar(source, destination):
    source=Path(source).resolve();destination=Path(destination).resolve();destination.mkdir(parents=True,exist_ok=True)
    static=joblib.load(source/'libras_static_v5.pkl')
    static_labels=json.loads((source/'libras_static_labels_v5.json').read_text(encoding='utf-8'))
    dynamic_labels=json.loads((source/'libras_dynamic_labels_v5.json').read_text(encoding='utf-8'))
    if static.__class__.__name__!='ExtraTreesClassifier' or static.n_features_in_!=126 or not np.array_equal(static.classes_,np.arange(len(static_labels))):
        raise ValueError('Modelo estático incompatível com o motor Java V1')
    with (destination/'static.bin').open('wb') as out:
        integer(out,0x43535431,126,len(static_labels),len(static.estimators_))
        for estimator in static.estimators_:
            tree=estimator.tree_;pairs=[];nodes=[]
            for node in range(tree.node_count):
                offset=len(pairs)
                if tree.children_left[node]==-1:
                    values=np.asarray(tree.value[node]).reshape(-1);values=values/values.sum()
                    pairs.extend((i,float(v)) for i,v in enumerate(values) if v!=0)
                nodes.append((int(tree.feature[node]),int(tree.children_left[node]),int(tree.children_right[node]),float(tree.threshold[node]),offset,len(pairs)-offset))
            integer(out,tree.node_count,len(pairs))
            for feature,left,right,threshold,offset,count in nodes:
                integer(out,feature,left,right);out.write(struct.pack('>d',threshold));integer(out,offset,count)
            for category,prob in pairs:integer(out,category);floats(out,[prob])
    import tensorflow as tf
    model=tf.keras.models.load_model(source/'libras_dynamic_v5.keras',compile=False,safe_mode=True)
    if tuple(model.input_shape[1:])!=(48,126) or model.output_shape[-1]!=len(dynamic_labels):raise ValueError('Entrada dinâmica incompatível')
    layers=[]
    for layer in model.layers:
        name=layer.__class__.__name__;config=layer.get_config()
        if name in ('InputLayer','Dropout'):continue
        if name=='GRU':
            if not(config['activation']=='tanh' and config['recurrent_activation']=='sigmoid' and config['reset_after'] and config['use_bias']) or any(config[k] for k in ('go_backwards','stateful','return_state')):
                raise ValueError('Configuração GRU não suportada')
            weights=layer.get_weights();layers.append((1,int(config['return_sequences']),weights))
        elif name=='Dense':
            if config['activation'] not in ('relu','softmax') or not config['use_bias']:raise ValueError('Dense incompatível')
            layers.append((2,1 if config['activation']=='relu' else 2,layer.get_weights()))
        else:raise ValueError('Camada não suportada: '+name)
    if [(a,b) for a,b,_ in layers]!=[(1,1),(1,0),(2,1),(2,2)]:raise ValueError('Arquitetura precisa ser GRU(sequência), GRU, Dense(relu), Dense(softmax)')
    with (destination/'dynamic.bin').open('wb') as out:
        integer(out,0x43445931,48,126,len(dynamic_labels),len(layers))
        for kind,activation,weights in layers:
            kernel=weights[0];units=kernel.shape[1]//3 if kind==1 else kernel.shape[1]
            integer(out,kind,kernel.shape[0],units,activation)
            for weight in weights:floats(out,weight)
    for name in ('hand_landmarker.task','libras_static_labels_v5.json','libras_dynamic_labels_v5.json'):shutil.copyfile(source/name,destination/name)
    try:commit=subprocess.check_output(['git','-C',str(source),'rev-parse','HEAD'],text=True).strip()
    except (OSError,subprocess.CalledProcessError):commit='local'
    files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(destination.iterdir()) if p.is_file() and p.name!='manifest.json'}
    manifest={'format':1,'source':'https://github.com/Gabiicortez/Connecta','commit':commit,'features':126,'sequence_length':48,'static_classes':len(static_labels),'dynamic_classes':len(dynamic_labels),'sha256':files}
    (destination/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
    print('Exportado:',destination,'\nVersão:',commit)
    return static,model


def exportar(source, destination):
    # Valide modelos e a interface do núcleo antes de substituir os arquivos.
    source=Path(source).resolve()
    core_bytes=(source/'libras_core.py').read_bytes()
    tree=ast.parse(core_bytes.decode('utf-8-sig'))
    names={node.name for node in tree.body if isinstance(node,(ast.FunctionDef,ast.ClassDef))}
    if not {'GestureRouter','StaticConsensus','normalize_static','normalize_dynamic'} <= names:
        raise ValueError('libras_core.py não oferece a interface esperada pelo app')
    destination=Path(destination).resolve();destination.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='connecta-export-',dir=destination.parent.parent.parent) as tmp:
        models=_exportar(source,tmp)
        destination.mkdir(parents=True,exist_ok=True)
        stage=Path(tmp)
        (stage/'libras_core.py').write_bytes(core_bytes)
        for name in ('static.bin','dynamic.bin','libras_static_labels_v5.json','libras_dynamic_labels_v5.json','manifest.json'):
            os.replace(stage/name,destination/name)
        os.replace(stage/'libras_core.py',destination.parent/'libras_core.py')
        detector=Path(__file__).resolve().parents[1]/'app/src/main/assets/ia/hand_landmarker.task'
        detector.parent.mkdir(parents=True,exist_ok=True)
        os.replace(stage/'hand_landmarker.task',detector)
    print('Assets atualizados em:',destination)
    return models


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--origem',required=True);p.add_argument('--destino',default=str(Path(__file__).resolve().parents[1]/'app/src/main/python/usuario_ia/modelos'));args=p.parse_args();exportar(args.origem,args.destino)
