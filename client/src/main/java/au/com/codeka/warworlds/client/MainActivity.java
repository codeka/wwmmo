package au.com.codeka.warworlds.client;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

import au.com.codeka.warworlds.client.welcome.WarmWelcomeFragment;
import au.com.codeka.warworlds.client.welcome.WelcomeFragment;
import au.com.codeka.warworlds.common.Log;

public class MainActivity extends AppCompatActivity {
  private static final Log log = new Log("MainActivity");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (savedInstanceState == null) {
/*
      final SharedPreferences prefs = Util.getSharedPreferences();
      if (!prefs.getBoolean("WarmWelcome", false)) {
        // if we've never done the warm-welcome, do it now
        log.info("Starting Warm Welcome");
        startActivity(new Intent(this, WarmWelcomeActivity.class));
        return;
      }

      if (RealmContext.i.getCurrentRealm() == null) {
        log.info("No realm selected, switching to RealmSelectActivity");
        startActivity(new Intent(this, RealmSelectActivity.class));
        return;
      }
*/
/*
      WarmWelcomeFragment warmWelcomeFragment = new WarmWelcomeFragment();
      getSupportFragmentManager()
          .beginTransaction()
          .add(R.id.fragment_container, warmWelcomeFragment)
          .commit();
*/
      WelcomeFragment welcomeFragment = new WelcomeFragment();
      getSupportFragmentManager()
          .beginTransaction()
          .add(R.id.fragment_container, welcomeFragment)
          .commit();
    }

    WebSocketFactory factory = new WebSocketFactory();
    try {
      WebSocket ws = factory.createSocket("ws://192.168.1.3:8080/conn");
      ws.addListener(new ServerWebSocketListener());
      //ws.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
      ws.connectAsynchronously();
    } catch (IOException e) {
      //tv.setText(e.getMessage());
    }
  }
}
