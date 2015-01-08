package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

/** Handler class that destroys a wormhole. */
public class WormholeDestroyHandler extends RequestHandler {
  @Override
  protected void post() throws RequestException {
    int starID = Integer.parseInt(getUrlParameter("starid"));
    // TODO: implement me.
  }
}
