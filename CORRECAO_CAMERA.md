# Correção: imagem da câmera reciclada

Esta versão corrige a ordem de leitura dos pixels em LocalRecognizer.java. Extraia o ZIP, abra o projeto e gere/instale outro APK. Apenas baixar o ZIP não atualiza o app instalado.

Se você já alterou o projeto no seu computador, pode substituir somente `app/src/main/java/com/connectalibras/ia/local/LocalRecognizer.java` pelo arquivo deste ZIP e gerar outro APK com a mesma assinatura. Mantenha seu banco e modelos locais.

A correção foi testada na JVM com uma simulação do descarte do bitmap. Teste novamente a câmera no celular depois de instalar. Python permanece 3.11.
