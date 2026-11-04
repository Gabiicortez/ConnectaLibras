package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class ConfiguracoesActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_configuracoes);requireUser(()->{
 Switch notifications=findViewById(R.id.switchNotificacoes);notifications.setChecked(current.notifications);
 notifications.setOnCheckedChangeListener((button,checked)->runTask(notifications,()->{store.notifications(current.id,checked);return true;},ok->message("Preferência salva neste aparelho.")));
 findViewById(R.id.btnAlterarSenha).setOnClickListener(v->go(AlterarSenhaActivity.class));findViewById(R.id.btnSair).setOnClickListener(v->logout());
 findViewById(R.id.btnPrivacidade).setOnClickListener(v->dialog("Privacidade","Os dados ficam no banco SQLite privado deste aplicativo, neste aparelho. Não há sincronização em nuvem. Limpar os dados ou desinstalar o app remove os registros locais."));
 findViewById(R.id.btnSobre).setOnClickListener(v->dialog("Connecta Libras","Tradutor de Libras em desenvolvimento. Cadastro, perfil e histórico usam SQLite local. O código original não inclui reconhecimento de sinais nem modelo treinado. A opção de notificações salva sua preferência; não existe serviço de envio configurado."));
 });}
}
