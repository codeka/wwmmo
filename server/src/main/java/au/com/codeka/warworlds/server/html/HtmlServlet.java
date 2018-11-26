package au.com.codeka.warworlds.server.html;

import au.com.codeka.warworlds.server.html.account.*;
import com.google.common.collect.Lists;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.handlers.HandlerServlet;
import au.com.codeka.warworlds.server.handlers.RequestHandler;
import au.com.codeka.warworlds.server.handlers.Route;
import au.com.codeka.warworlds.server.html.render.EmpireRendererHandler;
import au.com.codeka.warworlds.server.html.render.PlanetRendererHandler;
import au.com.codeka.warworlds.server.html.render.StarRendererHandler;

/**
 * Servlet for working with top-level {@link RequestHandler}s.
 */
public class HtmlServlet extends HandlerServlet {
  private static final Log log = new Log("HtmlServlet");
  private static final ArrayList<Route> ROUTES = Lists.newArrayList(
      new Route("/accounts", AccountsHandler.class),
      new Route("/accounts/associate", AccountAssociateHandler.class),
      new Route("/accounts/verify", AccountVerifyHandler.class),
      new Route("/accounts/patreon-begin", PatreonBeginHandler.class),
      new Route("/accounts/connect-to-patreon", ConnectToPatreonHandler.class),
      new Route("/login", LoginHandler.class),
      new Route(
          "/render/star/(?<star>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$",
          StarRendererHandler.class),
      new Route(
          "/render/planet/(?<star>[0-9]+)/(?<planet>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$",
          PlanetRendererHandler.class),
      new Route(
          "/render/empire/(?<empire>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$",
          EmpireRendererHandler.class),
      new Route("/(?<path>.*)", StaticFileHandler.class)
  );

  public HtmlServlet() {
    super(ROUTES);
  }
}
