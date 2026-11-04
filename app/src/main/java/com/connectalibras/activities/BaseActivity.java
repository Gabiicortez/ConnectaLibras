package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.*;
public abstract class BaseActivity extends AppCompatActivity {
 protected ConnectaStore store; protected ConnectaStore.User current; protected SessionManager session;
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 protected interface Task<T>{T run() throws Exception;} protected interface Done<T>{void accept(T value);}
 @Override protected void onCreate(Bundle b){super.onCreate(b);session=new SessionManager(this);}
 /** Adds a persistent back control above every screen, including programmatic forms. */
 @Override public void onContentChanged(){
  super.onContentChanged();
  ViewGroup content=findViewById(android.R.id.content);
  if(content==null || content.getChildCount()==0)return;
  View screen=content.getChildAt(0);
  if(screen.getId()==R.id.containerComVoltar)return;
  LinearLayout shell=new LinearLayout(this);
  shell.setId(R.id.containerComVoltar);
  shell.setOrientation(LinearLayout.VERTICAL);
  shell.setBackgroundColor(android.graphics.Color.rgb(248,250,252));
  LinearLayout bar=new LinearLayout(this);
  bar.setOrientation(LinearLayout.HORIZONTAL);
  bar.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);
  int spacing=(int)(8*getResources().getDisplayMetrics().density+0.5f);
  bar.setPadding(spacing,0,spacing,0);
  Button back=new Button(this);
  back.setId(R.id.btnVoltarGuia);
  back.setText("\u2190 Voltar");
  back.setContentDescription("Voltar para a página anterior");
  back.setAllCaps(false);
  back.setMinHeight(spacing*6);
  back.setTextColor(android.graphics.Color.rgb(15,118,110));
  back.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(248,250,252)));
  // Home returns to login and clears the authenticated navigation/session.
  back.setOnClickListener(v->{
   if(this instanceof HomeActivity)logout();
   else getOnBackPressedDispatcher().onBackPressed();
  });
  // Keep the inset wrapper but remove the entire back bar on the login page.
  if(this instanceof LoginActivity)bar.setVisibility(View.GONE);
  bar.addView(back,new LinearLayout.LayoutParams(-2,-2));
  content.removeView(screen);
  shell.addView(bar,new LinearLayout.LayoutParams(-1,-2));
  shell.addView(screen,new LinearLayout.LayoutParams(-1,0,1f));
  content.addView(shell,new ViewGroup.LayoutParams(-1,-1));
  // Existing back controls are replaced by the common header.
  View oldBack=screen.findViewById(R.id.btnVoltar);
  if(oldBack!=null)oldBack.setVisibility(View.GONE);
  oldBack=screen.findViewById(R.id.btnVoltarHistorico);
  if(oldBack!=null)oldBack.setVisibility(View.GONE);
  androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(shell,(v,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
   return androidx.core.view.WindowInsetsCompat.CONSUMED;
  });
  androidx.core.view.ViewCompat.requestApplyInsets(shell);
 }
 protected <T> void runTask(View trigger,Task<T> work,Done<T> done){if(trigger!=null)trigger.setEnabled(false);
 WORK.execute(()->{try{T result=work.run();runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){if(trigger!=null)trigger.setEnabled(true);done.accept(result);}});}
 catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){if(trigger!=null)trigger.setEnabled(true);message(e instanceof android.database.sqlite.SQLiteConstraintException?"Usuário, e-mail ou chave já cadastrado, ou dados inválidos.":"Não foi possível concluir: "+e.getMessage());}});}});}
 protected void requireUser(Runnable ready){runTask(null,()->{store=ConnectaStore.get(this);return store.user(session.userId());},u->{if(u==null){session.logout();reset(LoginActivity.class);}else{current=u;ready.run();}});}
 protected void go(Class<?> c){startActivity(new Intent(this,c));}
 protected void reset(Class<?> c){Intent i=new Intent(this,c);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();}
 protected void logout(){session.logout();reset(LoginActivity.class);}
 protected void message(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 protected void dialog(String title,String text){new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("OK",null).show();}
 protected boolean emailValid(String e){return android.util.Patterns.EMAIL_ADDRESS.matcher(e.trim()).matches();}
 protected LinearLayout form(String title){ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(32,32,32,32);box.setBackgroundColor(android.graphics.Color.rgb(248,250,252));scroll.addView(box);setContentView(scroll);TextView t=new TextView(this);t.setText(title);t.setTextSize(28);box.addView(t);return box;}
 protected Button addButton(LinearLayout box,String text,Runnable action){Button b=new Button(this);b.setText(text);box.addView(b,new ViewGroup.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());if(text.equals("Voltar"))b.setVisibility(View.GONE);return b;}
 @Override protected void onResume(){super.onResume();if(current!=null&&current.id!=session.userId())reset(LoginActivity.class);}
}
