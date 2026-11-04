package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class EditarPerfilActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_editar_perfil);requireUser(()->{
 EditText nome=findViewById(R.id.edtNome),email=findViewById(R.id.edtEmail);nome.setText(current.fullName);email.setText(current.email);Button salvar=findViewById(R.id.btnSalvar);
 salvar.setOnClickListener(v->{String n=nome.getText().toString().trim(),e=email.getText().toString().trim();if(n.isEmpty()||!emailValid(e)){message("Preencha nome e e-mail válido.");return;}
 EditText password=new EditText(this);password.setInputType(129);password.setHint("Senha atual");new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Confirme sua senha").setView(password).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(di,w)->{String s=password.getText().toString();runTask(salvar,()->{if(store.login(current.email,s)==null)throw new IllegalArgumentException("Senha atual incorreta");store.updateUser(current.id,n,e,"");return true;},ok->{message("Perfil atualizado.");finish();});}).show();});
 });}
}
