package au.com.codeka.warworlds.client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

import au.com.codeka.warworlds.common.Log;

public class MainActivity extends AppCompatActivity {
  private static final Log log = new Log("MainActivity");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    TextView tv = Preconditions.checkNotNull((TextView) findViewById(R.id.msg));
    WebSocketFactory factory = new WebSocketFactory();
    try {
      WebSocket ws = factory.createSocket("ws://192.168.1.3:8080/conn");
      ws.addListener(new ServerWebSocketListener());
      //ws.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
      ws.connectAsynchronously();
    } catch (IOException e) {
      tv.setText(e.getMessage());
    }
  }
}
