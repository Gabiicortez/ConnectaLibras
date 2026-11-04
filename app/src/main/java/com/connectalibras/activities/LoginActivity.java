package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class LoginActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_login);
 EditText email=findViewById(R.id.edtEmail),senha=findViewById(R.id.edtSenha);Button entrar=findViewById(R.id.btnEntrar);
 findViewById(R.id.txtCadastrar).setOnClickListener(v->go(CadastroActivity.class));
 findViewById(R.id.txtEsqueciSenha).setOnClickListener(v->dialog("Recuperação de senha","Este banco é local e não envia e-mails. Se ainda estiver conectado, altere sua senha nas configurações. Não há recuperação por e-mail configurada."));
 entrar.setOnClickListener(v->{String login=email.getText().toString().trim(),password=senha.getText().toString();if(login.isEmpty()||password.isEmpty()){message("Preencha usuário/e-mail e senha.");return;}
 runTask(entrar,()->ConnectaStore.get(this).login(login,password),u->{if(u==null)message("Usuário ou senha incorretos.");else{session.login(u.id);reset(HomeActivity.class);}});});
 }
}
