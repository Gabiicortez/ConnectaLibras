package com.connectalibras.activities;
import com.connectalibras.R;
import com.connectalibras.database.ConnectaStore;
import com.connectalibras.utils.SessionManager;
import android.os.Bundle;
import android.widget.*;

public class SplashActivity extends BaseActivity {
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_splash);
 runTask(null,()->{store=ConnectaStore.get(this);return store.user(session.userId());},u->reset(u==null?LoginActivity.class:HomeActivity.class));}
}
