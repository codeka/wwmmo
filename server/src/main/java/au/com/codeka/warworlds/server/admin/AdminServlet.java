package au.com.codeka.warworlds.server.admin;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.admin.handlers.AdminLoginHandler;
import au.com.codeka.warworlds.server.admin.handlers.DashboardHandler;
import au.com.codeka.warworlds.server.admin.handlers.DebugStarfieldHandler;
import au.com.codeka.warworlds.server.admin.handlers.FileHandler;

/**
 * The {@link AdminServlet} is the root of all requests for the admin backend.
 */
public class AdminServlet extends GenericServlet {
  private static final Log log = new Log("AdminServlet");
  private static final ArrayList<Route> ROUTES = Lists.newArrayList(
      new Route("/", DashboardHandler.class),
      new Route("/login", AdminLoginHandler.class),
      new Route("/debug/starfield", DebugStarfieldHandler.class),
      new Route("/(?<path>.*)", FileHandler.class)
  );

  @Override
  public void service(ServletRequest request, ServletResponse response)
      throws IOException, ServletException {
    String path = ((HttpServletRequest) request).getPathInfo();
    if (path == null) {
      path = "/";
    }

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
    } catch (Exception e) {
      return; // should never happen.
    }

    Session session = null;
    if (request.getCookies() != null) {
      String sessionCookieValue = "";
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals("SESSION")) {
          sessionCookieValue = cookie.getValue();
          log.info("Got SESSION cookie: %s", sessionCookieValue);
          session = SessionManager.i.getSession(sessionCookieValue);
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

    public Route(@Nonnull String pattern, Class<?> handlerClass) {
      this(pattern, handlerClass, null);
    }

    public Route(@Nonnull String pattern, Class<?> handlerClass, String extraOption) {
      this.pattern = Pattern.compile(Preconditions.checkNotNull(pattern));
      this.handlerClass = handlerClass;
      this.extraOption = extraOption;
    }
  }
}
