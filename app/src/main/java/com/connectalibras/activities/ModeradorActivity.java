package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class ModeradorActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);LinearLayout box=form("Moderação");requireUser(()->{if(!current.role.equals("moderador")){message("Acesso restrito ao moderador.");finish();return;}
 TextView identity=new TextView(this);identity.setText("Moderador: @"+current.name+" • ID "+current.id);box.addView(identity);
 TextView members=new TextView(this);box.addView(members);runTask(null,()->store.members(current.id),rows->members.setText(android.text.TextUtils.join("\n\n",rows)));
 TextView explanation=new TextView(this);explanation.setText("\nCadastrar sinal revisado no dicionário local (não treina a IA)");box.addView(explanation);
 EditText key=new EditText(this),word=new EditText(this),description=new EditText(this);key.setHint("Chave única");word.setHint("Palavra");description.setHint("Descrição revisada");box.addView(key);box.addView(word);box.addView(description);
 Button save=addButton(box,"Cadastrar sinal",()->{});save.setOnClickListener(v->{String k=key.getText().toString().trim(),w=word.getText().toString().trim(),de=description.getText().toString().trim();if(k.isEmpty()||w.isEmpty()||de.isEmpty()){message("Preencha todos os campos.");return;}runTask(save,()->{store.addSign(current.id,k,w,de);return true;},ok->{message("Sinal salvo.");key.setText("");word.setText("");description.setText("");});});addButton(box,"Abrir dicionário",()->go(DicionarioActivity.class));addButton(box,"Voltar",this::finish);
 });}
}
