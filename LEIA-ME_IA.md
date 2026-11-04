# Sua IA V6.1 em Python no Connecta

Esta entrega já inclui a entrada Android adaptada do arquivo `ialibras_v6_1_automatica.py` que você enviou, o núcleo `libras_core.py` e os pesos V5 preparados do seu GitHub. O app chama Python para normalizar os pontos, segmentar movimentos, executar as árvores/GRU, confirmar previsões e montar a frase.

## Usar agora

1. Extraia o ZIP e abra `Connecta_IA_Python` no Android Studio, na pasta que contém settings.gradle.
2. Instale Python 3.11 de 64 bits no computador e selecione JDK 17 como Gradle JDK. Aguarde a sincronização baixar as dependências.
3. Execute o módulo app ou gere outro APK. Use a mesma chave de assinatura da instalação anterior se quiser preservar o banco de dados do aparelho.
4. Entre no app, abra o tradutor, toque em Iniciar Tradução e permita a câmera. Não precisa de servidor, token, computador ligado durante o uso ou conexão entre redes.

A detecção dos pontos das mãos é feita pelo MediaPipe Android. Sua IA Python recebe esses pontos; não abre a webcam ou janelas de OpenCV. Ao iniciar no app, a frase começa a ser gravada, equivalente a apertar ESPAÇO na versão do PC. Ao parar ou sair, o texto confirmado é salvo no histórico SQLite.

Letras/números precisam de mão em repouso e confirmação por consenso. Gestos são avaliados depois de terminar o movimento. As regras de confiança e repetição são as do realtime() enviado: consenso estático mínimo 0,84; dinâmica mínimo 0,90 e margem 0,25; repetição da mesma classe tem intervalo mínimo de 1,2 s e entre inclusões 0,45 s. A frase é formada com espaços e letras minúsculas, como o add_to_sentence original. Há limite de 2000 caracteres por sessão no app.

## Onde estão os arquivos

- `app/src/main/python/usuario_ia/ialibras_v6_1_automatica.py`: sua lógica de inferência adaptada ao Android, com carregar/reconhecer/encerrar.
- `app/src/main/python/usuario_ia/minha_ia.py`: entrada que encaminha essas funções ao módulo V6.1.
- `app/src/main/python/usuario_ia/libras_core.py`: cópia do núcleo do repositório, sem alteração.
- `app/src/main/python/usuario_ia/pesos_android.py`: leitor e executor Python/NumPy dos pesos exportados.
- `app/src/main/python/usuario_ia/modelos`: árvores, GRU e labels já preparados.
- `referencia_pc/ialibras_v6_1_automatica.py`: seu arquivo recebido, preservado byte a byte, para consulta/treinamento no PC.
- `ORIGEM_IA.json`: identificação das fontes e dos modelos.

Seu arquivo recebido tem o mesmo código do GitHub, após normalizar as quebras de linha. O treinamento, teclado e janelas continuam sendo recursos de computador; não são executados no app.

## Seus modelos do computador

Você enviou o código .py, mas não enviou novos arquivos de pesos neste anexo. Os pesos incluídos vêm do repositório Gabiicortez/Connecta, commit 9c82405e535479c38107e19435e0e5b215fe3242.

Se os modelos no seu computador forem mais recentes, prepare-os com a ferramenta incluída antes de gerar o APK:

1. Mantenha juntos, na pasta da IA do computador:
   - libras_static_v5.pkl
   - libras_static_labels_v5.json
   - libras_dynamic_v5.keras
   - libras_dynamic_labels_v5.json
   - hand_landmarker.task
   - libras_core.py
2. Use Python com as dependências de `ferramentas_ia/requirements_pc.txt`. Pode usar um ambiente de exportação dedicado para não mudar seu ambiente de treinamento.
3. Na pasta raiz do app, execute (ajuste C:\caminho\teste_IAConnecta):

```powershell
py -3.11 -m venv .venv_exportacao
.venv_exportacao\Scripts\python.exe -m pip install -r ferramentas_ia/requirements_pc.txt
.venv_exportacao\Scripts\python.exe ferramentas_ia/preparar_modelos.py --origem C:\caminho\teste_IAConnecta
```

O comando exporta os pesos e labels diretamente para a pasta usada pelo app e copia o núcleo Python. Só substitui os arquivos depois de validar a arquitetura e as funções esperadas. Depois gere outro APK. `preparar_windows.bat` é um atalho para quem já instalou essas dependências no Python 3.11 do sistema: arraste a pasta da IA para ele.

A ferramenta usa somente os modelos V5. Os arquivos antigos libras_static.pkl e libras_gru.keras não devem ser renomeados como V5. Mantenha seus modelos antigos no PC se ainda precisar deles. Não coloque venv, .git, .idea, __pycache__ ou dataset_libras no projeto Android.

## Por que há pesos .bin

O app não tenta carregar o scikit-learn/TensorFlow de computador dentro do Android. A ferramenta exporta as árvores e os kernels/bias da GRU, sem retreinar. Os arquivos .bin são os dados do seu modelo; a inferência roda no arquivo Python pesos_android.py com NumPy. Não há classificador Java substituto nesta versão.

Mudanças nos pesos V5 mantendo a mesma arquitetura podem ser exportadas. Mudanças nas equações de inferência, nas funções do núcleo, nas entradas (126 atributos e 48 frames) ou nas camadas suportadas exigem nova adaptação e testes. Substituir o arquivo de desktop inteiro sobre a entrada adaptada desfaz a integração. Uma mudança de código Python exige recompilar o APK, assim como uma atualização dos pesos nesta versão.

## Validação e limites

Consulte VALIDACAO_IA.txt. A compilação e as comparações de inferência no computador passaram. Ainda é necessário testar câmera, velocidade, memória e compatibilidade das bibliotecas nativas no celular real. As classes reconhecidas são apenas as treinadas nos seus modelos; não se trata de tradução geral de Libras. A precisão em novos usuários não foi medida nesta entrega.

O runtime incorporado usa Python 3.11, NumPy 1.26.2 e Chaquopy 17.0.0. O APK suporta celulares arm64-v8a e emuladores x86_64, com Android API 24 ou superior. O projeto não solicita permissão INTERNET. A sincronização e a compilação no PC precisam de internet para baixar bibliotecas.
