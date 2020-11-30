package au.com.codeka.warworlds.server;

import com.codahale.metrics.jetty9.InstrumentedHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.ctrl.SessionController;
import au.com.codeka.warworlds.server.handlers.*;
import au.com.codeka.warworlds.server.handlers.admin.*;
import au.com.codeka.warworlds.server.metrics.MetricsManager;
import au.com.codeka.warworlds.server.monitor.MonitorManager;
import au.com.codeka.warworlds.server.monitor.RequestSuspendedException;

public class RequestRouter extends InstrumentedHandler {
  private static final Log log = new Log("RequestRouter");
  private static ArrayList<Route> sRoutes;

  private final MonitorManager monitorManager = new MonitorManager();

  static {
    sRoutes = new ArrayList<>();
    sRoutes.add(new Route("login", LoginHandler.class));
    sRoutes.add(new Route("devices/(?<id>[0-9]*)", DevicesHandler.class));
    sRoutes.add(new Route("devices", DevicesHandler.class));
    sRoutes.add(new Route("hello/(?<deviceid>[0-9]+)", HelloHandler.class));
    sRoutes.add(new Route("chat/blocks", ChatBlocksHandler.class));
    sRoutes.add(new Route(
        "chat/conversations/(?<conversationid>[0-9]+)/participants/(?<empireid>[0-9]+)",
        ChatConversationParticipantHandler.class));
    sRoutes.add(new Route(
        "chat/conversations/(?<conversationid>[0-9]+)/participants",
        ChatConversationParticipantsHandler.class));
    sRoutes.add(new Route("chat/conversations", ChatConversationsHandler.class));
    sRoutes.add(new Route("chat", ChatHandler.class));
    sRoutes.add(new Route("empires/patreon", EmpiresPatreonHandler.class));
    sRoutes.add(new Route("empires/search", EmpiresSearchHandler.class));
    sRoutes.add(new Route("empires/battle-ranks", EmpireBattleRankListHandler.class));
    sRoutes.add(new Route("empires/(?<empireid>[0-9]+)/stars", EmpiresStarsHandler.class));
    sRoutes.add(new Route("empires/(?<empireid>[0-9]+)/taxes", EmpiresTaxesHandler.class));
    sRoutes.add(new Route("empires/(?<empireid>[0-9]+)/cash-audit", EmpiresCashAuditHandler.class));
    sRoutes.add(new Route(
        "empires/(?<empireid>[0-9]+)/display-name", EmpiresDisplayNameHandler.class));
    sRoutes.add(new Route("empires/(?<empireid>[0-9]+)/shield", EmpiresShieldHandler.class));
    sRoutes.add(new Route("empires/(?<empireid>[0-9]+)/reset", EmpiresResetHandler.class));
    sRoutes.add(new Route("empires/(?<empireid>[0-9]+)/ads", EmpiresAdsHandler.class));
    sRoutes.add(new Route("empires", EmpiresHandler.class));
    sRoutes.add(new Route("buildqueue", BuildQueueHandler.class));
    sRoutes.add(new Route("sectors", SectorsHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)/simulate", StarSimulateHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/build/(?<buildid>[0-9]+)/accelerate",
        BuildAccelerateHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/build/(?<buildid>[0-9]+)/stop", BuildStopHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/colonies/(?<colonyid>[0-9]+)", ColonyHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/colonies/(?<colonyid>[0-9]+)/attack", ColonyAttackHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/colonies/(?<colonyid>[0-9]+)/abandon",
        ColonyAbandonHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)/colonies", ColoniesHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/combat-reports/(?<combatreportid>[0-9]+)",
        CombatReportHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/fleets/(?<fleetid>[0-9]+)/orders", FleetOrdersHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)/fleets/(?<fleetid>[0-9]+)", FleetHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)/scout-reports", ScoutReportsHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)/sit-reports", SitReportsHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)/wormhole/tune", WormholeTuneHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/wormhole/destroy", WormholeDestroyHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/wormhole/take-over", WormholeTakeOverHandler.class));
    sRoutes.add(new Route(
        "stars/(?<starid>[0-9]+)/wormhole/disruptor-nearby", WormholeDisruptorNearbyHandler.class));
    sRoutes.add(new Route("stars/(?<starid>[0-9]+)", StarHandler.class));
    sRoutes.add(new Route("stars", StarsHandler.class));
    sRoutes.add(new Route(
        "alliances/(?<allianceid>[0-9]+)/requests/(?<requestid>[0-9]+)",
        AllianceRequestHandler.class));
    sRoutes.add(new Route(
        "alliances/(?<allianceid>[0-9]+)/requests", AllianceRequestsHandler.class));
    sRoutes.add(new Route("alliances/(?<allianceid>[0-9]+)/shield", AllianceShieldHandler.class));
    sRoutes.add(new Route(
        "alliances/(?<allianceid>[0-9]+)/wormholes", AllianceWormholeHandler.class));
    sRoutes.add(new Route("alliances/(?<allianceid>[0-9]+)", AllianceHandler.class));
    sRoutes.add(new Route("alliances", AlliancesHandler.class));
    sRoutes.add(new Route("sit-reports/read", SitReportsReadHandler.class));
    sRoutes.add(new Route("sit-reports", SitReportsHandler.class));
    sRoutes.add(new Route(
        "rankings/(?<year>[0-9]+)/(?<month>[0-9]+)", RankingHistoryHandler.class));
    sRoutes.add(new Route("motd", MotdHandler.class));
    sRoutes.add(new Route("notifications", NotificationHandler.class));
    sRoutes.add(new Route("error-reports", ErrorReportsHandler.class));
    sRoutes.add(new Route("anon-associate", AnonUserAssociateHandler.class));

    sRoutes.add(new Route("admin/login", AdminLoginHandler.class));
    sRoutes.add(new Route(
        "admin/(?<path>actions/move-star)", AdminActionsMoveStarHandler.class, "admin/"));
    sRoutes.add(new Route(
        "admin/(?<path>actions/reset-empire)", AdminActionsResetEmpireHandler.class, "admin/"));
    sRoutes.add(new Route(
        "admin/(?<path>actions/create-fleet)", AdminActionsCreateFleetHandler.class, "admin/"));
    sRoutes.add(new Route(
        "admin/alliance/(?<allianceid>[0-9]+)/details", AdminAllianceDetailsHandler.class));
    sRoutes.add(new Route(
        "admin/alliance/(?<allianceid>[0-9]+)/requests/(?<requestid>[0-9]+)/force-accept",
        AdminAllianceRequestForceAcceptHandler.class));
    sRoutes.add(new Route("admin/chat", AdminChatHandler.class));
    sRoutes.add(new Route("admin/chat/profanity", AdminChatProfanityHandler.class));
    sRoutes.add(new Route("admin/debug/purchases", AdminDebugPurchasesHandler.class, "admin/"));
    sRoutes.add(new Route(
        "admin/debug/error-reports", AdminDebugErrorReportsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/debug/retrace", AdminDebugRetraceHandler.class, "admin/"));
    sRoutes.add(new Route("admin/debug/moving-fleets", AdminMovingFleetsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/shields", AdminEmpireShieldsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/alts", AdminEmpireAltsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/refresh-ranks", AdminRefreshRanksHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/logins", AdminEmpireLoginsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/(?<empireid>[0-9]+)/details", AdminEmpireDetailsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/(?<empireid>[0-9]+)/logins", AdminEmpireLoginsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/empire/(?<empireid>[0-9]+)/notifications", AdminEmpireNotificationsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/(?<path>empire/bonus-cash)", AdminEmpireBonusCashHandler.class, "admin/"));
    sRoutes.add(new Route("admin/(?<path>empire/ban)", AdminEmpireBanHandler.class, "admin/"));
    sRoutes.add(new Route("admin/users", AdminUsersHandler.class, "admin/"));
    sRoutes.add(new Route("admin/(?<path>cron)", AdminCronHandler.class, "admin/"));
    sRoutes.add(new Route("admin/metrics", AdminMetricsHandler.class, "admin/"));
    sRoutes.add(new Route("admin/(?<path>.+)", AdminGenericHandler.class, "admin/"));
    sRoutes.add(new Route("admin/?", AdminDashboardHandler.class));

    // TODO: move intel to a different handler
    sRoutes.add(new Route("intel/?(?<path>$)", AdminGenericHandler.class, "intel/"));
    sRoutes.add(new Route("intel/(?<path>.*)", StaticFileHandler.class, "intel/"));

    sRoutes.add(new Route("css/(?<path>.*)", StaticFileHandler.class, "css/"));
    sRoutes.add(new Route("js/(?<path>.*)", StaticFileHandler.class, "js/"));
    sRoutes.add(new Route("img/(?<path>.*)", StaticFileHandler.class, "img/"));
    sRoutes.add(new Route("(?<path>[^/]+)", StaticFileHandler.class, "/"));

    // Special route for the root favicon.ico
    sRoutes.add(new Route("/(?<path>[^/]+)", true, StaticFileHandler.class, "/"));
  }

  public RequestRouter() {
    super(MetricsManager.i.getMetricsRegistry(), "handler");
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
                     HttpServletResponse response) throws IOException, ServletException {
    super.handle(target, baseRequest, request, response);

    for (Route route : sRoutes) {
      Matcher matcher = route.pattern.matcher(target);
      if (matcher.matches()) {
        handle(matcher, route, request, response);
        baseRequest.setHandled(true);
        return;
      }
    }

    log.info(String.format("Could not find handler for URL: %s", target));
    response.setStatus(404);
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
    String impersonate = request.getParameter("on_behalf_of");
    if (request.getCookies() != null) {
      String sessionCookieValue = "";
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals("SESSION")) {
          sessionCookieValue = cookie.getValue();
          try {
            session = new SessionController().getSession(sessionCookieValue, impersonate);
          } catch (RequestException e) {
            log.error("Error getting session: cookie=" + sessionCookieValue, e);
          }
        }
      }
      if (sessionCookieValue.equals("")) {
        log.warning("No session cookie found.");
      }
    }

    long startTime = System.currentTimeMillis();
    try {
      try {
        monitorManager.onBeginRequest(session, request, response);
      } catch (RequestException e) {
        e.populate(response);
        return;
      } catch (RequestSuspendedException e) {
        // request was suspended, just finish.
        return;
      }
      handler.handle(matcher, route.extraOption, session, request, response);
    } finally {
      monitorManager.onEndRequest(
          session, request, response, System.currentTimeMillis() - startTime);
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
      this("/realms/(?<realm>[a-z]+)/" + pattern, false, handlerClass, extraOption);
    }

    public Route(String pattern, boolean dontAddRealm, Class<?> handlerClass, String extraOption) {
      this.pattern = Pattern.compile(pattern);
      this.handlerClass = handlerClass;
      this.extraOption = extraOption;
    }
  }
}
