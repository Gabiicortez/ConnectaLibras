# Connecta Libras — SQLite local integrado

## Abrir e executar
1. Extraia o ZIP. No Android Studio, abra a pasta `connectaCERTO` que contém `settings.gradle`.
2. Aguarde a sincronização. Use JDK 17 no Gradle, SDK Android 35 e módulo `app`.
3. Execute no emulador ou celular Android 7.0+ (API 24).
4. Entre com usuário **admin** ou e-mail **robertowfs1@gmail.com**, senha inicial **1234**.

Moderador: ID **1**. A senha inicial tem quatro dígitos conforme solicitado;
novos cadastros e alteração de senha pedem seis caracteres ou mais.
Não é necessário rodar Python, API, Firebase ou MongoDB para usar o app.

## O que foi conectado
- Cadastro: nome completo, nome de usuário, tipo Libras/ouvinte, e-mail único e senha com hash.
- Login por usuário ou e-mail; sessão por ID, retomada e saída com limpeza da navegação.
- Perfil lido do banco; atualização de nome/e-mail com confirmação da senha atual.
- Troca de senha verifica a senha antiga e persiste o novo hash.
- Preferência de notificações salva por usuário; não cria serviço de envio de notificações.
- Histórico e traduções favoritas separados por usuário; limpeza com confirmação.
- Avaliações/correções de traduções ligadas ao dono do registro.
- Moderação visível apenas ao moderador, com consulta de contas e cadastro de sinais revisados.
- Dicionário local consulta os sinais cadastrados. Cadastrar sinais não treina o modelo.

## Banco e Python
O banco inicial está em `app/src/main/assets/connecta_inicial.db`.
Na primeira abertura, é copiado para o armazenamento privado do Android como
`databases/connecta_local.db`. Nas seguintes aberturas, o arquivo existente é
preservado. A senha não é redefinida ao fechar e abrir o app.
O banco usa SQL/SQLite, com hash PBKDF2-HMAC-SHA256 e salt aleatório por senha.
Os acessos de banco e hash são executados em uma fila de trabalho fora da interface.

`ferramentas_python` contém `banco.py`, `schema.sql`, uma cópia inicial `connecta.db`
e o validador `validar_integracao.py`. No computador, rode `py banco.py login` para
consultar/testar essa cópia. O Python não roda dentro do Android e a cópia do PC
não sincroniza com o emulador. Cada instalação possui seus próprios dados.
Não importe o banco antigo de outro pacote: este schema acrescenta nome completo,
tipo de usuário, preferência e favoritos de traduções.

Para inspecionar o banco ativo: Android Studio > App Inspection > Database Inspector,
com app em execução em emulador API 26+. Não edite o asset esperando atualizar uma
instalação existente. Limpar os dados/desinstalar apaga os registros locais.
O backup automático foi desativado; não há recuperação de senha por e-mail.
A conta e a senha iniciais são para desenvolvimento local; troque a senha no Perfil/Configurações
antes de distribuir uma versão real. Um banco local não é autenticação compartilhada de servidor.

## IA e teste do banco
As classes CameraAnalyzer, TradutorLibras e TensorFlowHelper vieram vazias e não há
modelo treinado no ZIP. Nenhum reconhecimento foi simulado. Na tela Tradutor, o botão
**Testar histórico com texto manual** grava um registro explicitamente manual.
Depois, confira-o no histórico, favorite e consulte a tela de favoritos.
Para integrar resultados reais no futuro, `ConversaActivity.registrarTraducao(resultado,
versaoModelo)` já grava o texto com origem câmera. Esse método deve receber uma saída
real do reconhecedor; não é um detector por si só.

## Correções de estrutura
Foram implementadas as classes de tela ausentes que já estavam no Manifest.
As logos e os layouts originais foram preservados, com campos e botões necessários.
Foi removida uma definição duplicada de Conversa no pacote errado.
O Gradle Wrapper foi alinhado a 8.9 para AGP 8.7.3; SDK 35 mantido.
A dependência RecyclerView foi declarada para o histórico.
Caches, builds, pastas IDE e local.properties da máquina anterior não fazem parte da entrega.

## Validação e limites
Foram verificadas a sintaxe Java, as referências a IDs/layouts, as classes do Manifest,
as regras e integridade do SQLite, o login Python e isolamento dos dados por usuário.
O ambiente não dispõe de Android SDK: build completo e execução no emulador não foram
validados aqui. O relatório VALIDACAO.txt registra o que efetivamente foi executado.
