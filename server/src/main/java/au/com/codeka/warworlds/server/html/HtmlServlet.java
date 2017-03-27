package au.com.codeka.warworlds.server.html;

import com.google.common.collect.Lists;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.handlers.HandlerServlet;
import au.com.codeka.warworlds.server.handlers.RequestHandler;
import au.com.codeka.warworlds.server.handlers.Route;
import au.com.codeka.warworlds.server.html.account.AccountAssociateHandler;
import au.com.codeka.warworlds.server.html.account.AccountVerifyHandler;
import au.com.codeka.warworlds.server.html.account.AccountsHandler;
import au.com.codeka.warworlds.server.html.account.LoginHandler;

/**
 * Servlet for working with top-level {@link RequestHandler}s.
 */
public class HtmlServlet extends HandlerServlet {
  private static final Log log = new Log("HtmlServlet");
  private static final ArrayList<Route> ROUTES = Lists.newArrayList(
      new Route("/accounts", AccountsHandler.class),
      new Route("/accounts/associate", AccountAssociateHandler.class),
      new Route("/accounts/verify", AccountVerifyHandler.class),
      new Route("/login", LoginHandler.class),
      new Route("/(?<path>.*)", StaticFileHandler.class)
  );

  public HtmlServlet() {
    super(ROUTES);
  }
}
