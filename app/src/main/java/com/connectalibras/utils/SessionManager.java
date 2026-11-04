package com.connectalibras.utils;
import android.content.Context;
public final class SessionManager {
 private final android.content.SharedPreferences prefs;
 public SessionManager(Context c){prefs=c.getApplicationContext().getSharedPreferences("connecta_local_session",Context.MODE_PRIVATE);}
 public long userId(){return prefs.getLong("usuario_id",-1);}
 public void login(long id){prefs.edit().putLong("usuario_id",id).apply();}
 public void logout(){prefs.edit().clear().apply();}
}
