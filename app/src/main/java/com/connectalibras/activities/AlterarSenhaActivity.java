package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class AlterarSenhaActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_alterar_senha);requireUser(()->{
 EditText old=findViewById(R.id.edtSenhaAtual),next=findViewById(R.id.edtNovaSenha),confirm=findViewById(R.id.edtConfirmarSenha);Button save=findViewById(R.id.btnSalvarSenha);
 save.setOnClickListener(v->{String o=old.getText().toString(),n=next.getText().toString();if(o.isEmpty()||n.length()<6||!n.equals(confirm.getText().toString())){message("Informe a senha atual e uma nova senha com no mínimo 6 caracteres e confirmação igual.");return;}
 runTask(save,()->{store.changePassword(current.id,o,n);return true;},ok->{message("Senha alterada.");finish();});});
 });}
}
