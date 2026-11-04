package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class CadastroActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_cadastro);
 EditText nome=findViewById(R.id.edtNome),user=findViewById(R.id.edtUsuario),email=findViewById(R.id.edtEmail),senha=findViewById(R.id.edtSenha),confirm=findViewById(R.id.edtConfirmarSenha);RadioGroup tipo=findViewById(R.id.radioTipoUsuario);Button cadastrar=findViewById(R.id.btnCadastrar);
 cadastrar.setOnClickListener(v->{String n=nome.getText().toString().trim(),u=user.getText().toString().trim(),e=email.getText().toString().trim(),s=senha.getText().toString();
 if(n.isEmpty()||!u.matches("[A-Za-z0-9_.-]{3,32}")||!emailValid(e)){message("Informe nome, usuário de 3 a 32 letras, números, ponto, hífen ou sublinhado e e-mail válido.");return;}
 if(tipo.getCheckedRadioButtonId()==-1){message("Selecione seu tipo de usuário.");return;}
 if(s.length()<6||!s.equals(confirm.getText().toString())){message("A senha deve ter no mínimo 6 caracteres e confirmação igual.");return;}
 String t=tipo.getCheckedRadioButtonId()==R.id.rbLibras?"libras":"ouvinte";
 runTask(cadastrar,()->{ConnectaStore.get(this).register(u,n,e,s,t);return true;},ok->{message("Conta criada. Faça seu login.");finish();});});
 }
}
