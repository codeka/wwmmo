package au.com.codeka.warworlds.server.admin;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.regex.Matcher;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.admin.handlers.AdminFileHandler;
import au.com.codeka.warworlds.server.admin.handlers.AdminHandler;
import au.com.codeka.warworlds.server.admin.handlers.AdminLoginHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxAccountsHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxChatHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxDesignsHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxEmpireHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxSectorsHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxStarfieldHandler;
import au.com.codeka.warworlds.server.admin.handlers.AjaxUsersHandler;
import au.com.codeka.warworlds.server.admin.handlers.ChatHandler;
import au.com.codeka.warworlds.server.admin.handlers.DashboardHandler;
import au.com.codeka.warworlds.server.admin.handlers.DebugSimulationQueueHandler;
import au.com.codeka.warworlds.server.admin.handlers.DebugSuspiciousEventsHandler;
import au.com.codeka.warworlds.server.admin.handlers.EmpireDetailsHandler;
import au.com.codeka.warworlds.server.admin.handlers.EmpiresHandler;
import au.com.codeka.warworlds.server.admin.handlers.SectorsHandler;
import au.com.codeka.warworlds.server.admin.handlers.StarfieldHandler;
import au.com.codeka.warworlds.server.admin.handlers.UsersCreateHandler;
import au.com.codeka.warworlds.server.admin.handlers.UsersHandler;
import au.com.codeka.warworlds.server.handlers.HandlerServlet;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.handlers.Route;

/**
 * The {@link AdminServlet} is the root of all requests for the admin backend.
 */
public class AdminServlet extends HandlerServlet {
  private static final Log log = new Log("AdminServlet");
  private static final ArrayList<Route> ROUTES = Lists.newArrayList(
      new Route("/", DashboardHandler.class),
      new Route("/login", AdminLoginHandler.class),
      new Route("/sectors", SectorsHandler.class),
      new Route("/starfield", StarfieldHandler.class),
      new Route("/empires", EmpiresHandler.class),
      new Route("/empires/(?<id>[0-9]+)", EmpireDetailsHandler.class),
      new Route("/users", UsersHandler.class),
      new Route("/users/create", UsersCreateHandler.class),
      new Route("/chat", ChatHandler.class),
      new Route("/debug/suspicious-events", DebugSuspiciousEventsHandler.class),
      new Route("/debug/simulation-queue", DebugSimulationQueueHandler.class),
      new Route("/ajax/accounts", AjaxAccountsHandler.class),
      new Route("/ajax/chat", AjaxChatHandler.class),
      new Route("/ajax/empire", AjaxEmpireHandler.class),
      new Route("/ajax/designs", AjaxDesignsHandler.class),
      new Route("/ajax/sectors", AjaxSectorsHandler.class),
      new Route("/ajax/starfield", AjaxStarfieldHandler.class),
      new Route("/ajax/users", AjaxUsersHandler.class),
      new Route("/(?<path>.*)", AdminFileHandler.class)
  );

  public AdminServlet() {
    super(ROUTES);
  }

  protected void handle(
      Matcher matcher,
      Route route,
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      Session session = getSession(request);
      AdminHandler handler = (AdminHandler) route.createRequestHandler();
      handler.setup(matcher, route.getExtraOption(), session, request, response);
      handler.handle();
    } catch (RequestException e) {
      e.populate(response);
    }
  }

  @Nullable
  private static Session getSession(HttpServletRequest request) {
    if (request.getCookies() != null) {
      String sessionCookieValue = "";
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals("SESSION")) {
          sessionCookieValue = cookie.getValue();
          return SessionManager.i.getSession(sessionCookieValue);
        }
      }
    }

    return null;
  }
}
