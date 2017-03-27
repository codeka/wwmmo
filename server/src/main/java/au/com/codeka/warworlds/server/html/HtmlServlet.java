package au.com.codeka.warworlds.server.html;

import com.google.common.collect.Lists;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.handlers.HandlerServlet;
import au.com.codeka.warworlds.server.handlers.RequestHandler;
import au.com.codeka.warworlds.server.handlers.Route;

/**
 * Servlet for working with top-level {@link RequestHandler}s.
 */
public class HtmlServlet extends HandlerServlet {
  private static final Log log = new Log("HtmlServlet");
  private static final ArrayList<Route> ROUTES = Lists.newArrayList(
      //TODO
      new Route("/(?<path>.*)", StaticFileHandler.class)
  );

  public HtmlServlet() {
    super(ROUTES);
  }
}
