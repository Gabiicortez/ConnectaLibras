package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.connectalibras.adapters.HistoricoAdapter;
import com.connectalibras.models.Conversa;
public class HistoricoActivity extends BaseActivity {
 protected boolean favoritesOnly(){return false;}
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_historico);
 ((TextView)findViewById(R.id.txtTituloHistorico)).setText(favoritesOnly()?"Traduções favoritas":"Histórico de Traduções");
 findViewById(R.id.btnVoltarHistorico).setOnClickListener(v->finish());findViewById(R.id.btnLimparHistorico).setVisibility(favoritesOnly()?android.view.View.GONE:android.view.View.VISIBLE);
 findViewById(R.id.btnLimparHistorico).setOnClickListener(v->new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("Excluir todo o seu histórico, incluindo favoritos e avaliações?").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(di,w)->runTask(null,()->{store.clearHistory(current.id);return true;},ok->refresh())).show());}
 @Override protected void onResume(){super.onResume();requireUser(this::refresh);}
 private void refresh(){runTask(null,()->store.translations(current.id,favoritesOnly()),rows->{RecyclerView recycler=findViewById(R.id.recyclerHistorico);recycler.setLayoutManager(new LinearLayoutManager(this));
 recycler.setAdapter(new HistoricoAdapter(rows,new HistoricoAdapter.Actions(){public void favorite(Conversa c){runTask(null,()->{store.favoriteTranslation(current.id,c.getId(),!c.isFavorito());return true;},ok->refresh());}public void feedback(Conversa c){showFeedback(c);}}));
 ((TextView)findViewById(R.id.txtHistoricoVazio)).setText(rows.isEmpty()?(favoritesOnly()?"Nenhum favorito. Favorite um registro no histórico.":"Nenhuma tradução salva."):rows.size()+" registro(s)");});}
 private void showFeedback(Conversa c){EditText correction=new EditText(this);correction.setHint("Texto corrigido");new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Avaliar tradução").setView(correction).setNeutralButton("Cancelar",null).setPositiveButton("Correta",(di,w)->saveFeedback(c,true,"")).setNegativeButton("Corrigir",(di,w)->{String value=correction.getText().toString().trim();if(value.isEmpty())message("Informe o texto corrigido.");else saveFeedback(c,false,value);}).show();}
 private void saveFeedback(Conversa c,boolean correct,String value){runTask(null,()->{store.feedback(current.id,c.getId(),correct,value);return true;},ok->message("Avaliação salva."));}
}
