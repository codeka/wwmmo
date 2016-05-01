package au.com.codeka.warworlds.server.websock;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * WebSocket servlet which waits for connections from clients.
 */
public class GameWebSocketServlet  extends WebSocketServlet {
  private final static Log log = new Log("GameWebSocketServlet");

  @Override
  public void configure(WebSocketServletFactory factory) {
    log.info("GameWebSocketServlet being configured.");
    factory.getPolicy().setIdleTimeout(10000);
    factory.setCreator(new Creator());
  }

  public class Creator implements WebSocketCreator {
    public Creator() {
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
      String cookie = req.getHeader("X-Cookie");
      if (cookie != null && cookie.length() > 0) {
        Account account = DataStore.i.accounts().get(cookie);
        if (account == null) {
          log.warning("No account for cookie, not connecting: %s", cookie);
          sendError(resp, 403, "Invalid cookie specified.");
          return null;
        }

        log.info("Cookie '%s' connected, empire #%d", cookie, account.empire_id);
        return new GameSocket(cookie, account);
      }

      log.warning("Connection attempt with no cookie, dropping.");
      sendError(resp, 403, "No cookie specified.");
      return null;
    }

    private void sendError(ServletUpgradeResponse resp, int code, String msg) {
      try {
        resp.sendError(code, msg);
      } catch (IOException e) {
        // ignore.
      }
    }
  }
}
