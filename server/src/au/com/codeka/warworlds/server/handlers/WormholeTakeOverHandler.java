package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

/** Handler class that lets you take over a wormhole. */
public class WormholeTakeOverHandler extends RequestHandler {
  @Override
  protected void post() throws RequestException {
    int starID = Integer.parseInt(getUrlParameter("starid"));
    // TODO: implement me.
  }
}
