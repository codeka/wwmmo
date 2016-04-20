package au.com.codeka.warworlds.server.websock;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * WebSocket servlet which waits for connections from clients.
 */
public class GameWebSocketServlet  extends WebSocketServlet {
  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setIdleTimeout(10000);
    factory.register(GameSocket.class);
  }
}
