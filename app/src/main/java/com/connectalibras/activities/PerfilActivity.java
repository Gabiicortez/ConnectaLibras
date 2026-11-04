package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class PerfilActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_perfil);
 findViewById(R.id.btnEditarPerfil).setOnClickListener(v->go(EditarPerfilActivity.class));findViewById(R.id.btnConfiguracoes).setOnClickListener(v->go(ConfiguracoesActivity.class));findViewById(R.id.btnHistorico).setOnClickListener(v->go(HistoricoActivity.class));findViewById(R.id.btnSair).setOnClickListener(v->logout());}
 @Override protected void onResume(){super.onResume();requireUser(()->{((TextView)findViewById(R.id.txtNome)).setText(current.fullName);((TextView)findViewById(R.id.txtEmail)).setText(current.email);((TextView)findViewById(R.id.txtTipoUsuario)).setText("@"+current.name+" • "+(current.role.equals("moderador")?"Moderador":current.type.equals("libras")?"Usuário de Libras":"Ouvinte"));});}
}
