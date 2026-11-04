package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class HomeActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_home);
 findViewById(R.id.btnIniciarConversa).setOnClickListener(v->go(ConversaActivity.class));findViewById(R.id.btnConversa).setOnClickListener(v->go(ConversaActivity.class));
 findViewById(R.id.btnHistorico).setOnClickListener(v->go(HistoricoActivity.class));findViewById(R.id.btnPerfil).setOnClickListener(v->go(PerfilActivity.class));findViewById(R.id.btnPerfilNav).setOnClickListener(v->go(PerfilActivity.class));
 findViewById(R.id.btnDicionario).setOnClickListener(v->go(DicionarioActivity.class));
 findViewById(R.id.btnHome).setOnClickListener(v->message("Você está no início."));findViewById(R.id.btnFavoritos).setOnClickListener(v->go(FavoritosActivity.class));
 findViewById(R.id.btnModeracao).setOnClickListener(v->go(ModeradorActivity.class));
 }
 @Override protected void onResume(){super.onResume();requireUser(()->{((TextView)findViewById(R.id.txtBoasVindas)).setText("Olá, "+current.fullName);findViewById(R.id.btnModeracao).setVisibility(current.role.equals("moderador")?android.view.View.VISIBLE:android.view.View.GONE);});}
}
