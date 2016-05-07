package au.com.codeka.warworlds.server.admin;

import com.google.common.collect.Lists;

import org.eclipse.jetty.http.HttpParser;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.admin.handlers.AdminLoginHandler;

/**
 * The {@link AdminServlet} is the root of all requests for the admin backend.
 */
public class AdminServlet extends GenericServlet {
  private static final Log log = new Log("AdminServlet");
  private static final ArrayList<Route> ROUTES = Lists.newArrayList(
      new Route("/login", AdminLoginHandler.class)//,
//      new Route("admin/(?<path>actions/move-star)", AdminActionsMoveStarHandler.class, "admin/"),
//      new Route("admin/(?<path>actions/reset-empire)", AdminActionsResetEmpireHandler.class, "admin/"),
//      new Route("admin/alliance/(?<allianceid>[0-9]+)/details", AdminAllianceDetailsHandler.class)
  );

  @Override
  public void service(ServletRequest request, ServletResponse response)
      throws IOException, ServletException {
    String path = ((HttpServletRequest) request).getPathInfo();
    log.info("path: %s", path);
    for (Route route : ROUTES) {
      Matcher matcher = route.pattern.matcher(path);
      if (matcher.matches()) {
        handle(matcher, route, (HttpServletRequest) request, (HttpServletResponse) response);
        return;
      }
    }

    log.info(String.format("Could not find handler for URL: %s", path));
    ((HttpServletResponse) response).setStatus(404);
  }

  private void handle(Matcher matcher, Route route, HttpServletRequest request,
      HttpServletResponse response) {
    RequestHandler handler;
    try {
      handler = (RequestHandler) route.handlerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      return; // TODO: error
    }

    Session session = null;
    if (request.getCookies() != null) {
      String sessionCookieValue = "";
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals("SESSION")) {
          sessionCookieValue = cookie.getValue();
          log.info("Got SESSION cookie: %s", sessionCookieValue);
          //try {
            //session = new SessionController().getSession(
            //    sessionCookieValue, impersonate);
            session = new Session(sessionCookieValue, "dean@codeka.com.au", DateTime.now());
          //} catch (RequestException e) {
          //  log.error("Error getting session: cookie=" + sessionCookieValue, e);
          //}
        }
      }
      if (sessionCookieValue.equals("")) {
        log.warning("No session cookie found.");
      }
    }

    try {
      handler.handle(matcher, route.extraOption, session, request, response);
    } catch (RequestException e) {
      e.populate(response);
    }
  }

  private static class Route {
    public java.util.regex.Pattern pattern;
    public Class<?> handlerClass;
    public String extraOption;

    public Route(String pattern, Class<?> handlerClass) {
      this(pattern, handlerClass, null);
    }

    public Route(String pattern, Class<?> handlerClass, String extraOption) {
      this.pattern = Pattern.compile(pattern);
      this.handlerClass = handlerClass;
      this.extraOption = extraOption;
    }
  }
}
