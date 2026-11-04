package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class DicionarioActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);LinearLayout box=form("Dicionário local");requireUser(()->runTask(null,()->store.signs(current.id,false),rows->{if(rows.isEmpty()){TextView empty=new TextView(this);empty.setText("Nenhum sinal revisado cadastrado.");box.addView(empty);}for(String[] row:rows){TextView text=new TextView(this);text.setText("\n"+row[1]+"\n"+row[2]);text.setTextSize(18);box.addView(text);}addButton(box,"Voltar",this::finish);}));}
}
