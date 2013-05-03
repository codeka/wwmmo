package au.com.codeka.warworlds.server;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jregex.Matcher;
import jregex.Pattern;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.handlers.*;
import au.com.codeka.warworlds.server.handlers.pages.*;


public class RequestRouter extends AbstractHandler {
    private final Logger log = LoggerFactory.getLogger(RequestRouter.class);
    private static ArrayList<Route> sRoutes;

    {
        sRoutes = new ArrayList<Route>();
        sRoutes.add(new Route("^/login$", LoginHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/devices/({id}[0-9]+)$", DevicesHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/devices$", DevicesHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/hello/({device_id}[0-9]+)$", HelloHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/chat$", ChatHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/empires$", EmpiresHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/empires/search$", EmpiresSearchHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/empires/({empire_id}[0-9]+)/stars$", EmpiresStarsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/buildqueue", BuildQueueHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/sectors$", SectorsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars$", StarsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)$", StarHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/simulate$", StarSimulateHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/build/({build_id}[0-9]+)/accelerate", BuildAccelerateHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/build/({build_id}[0-9]+)/stop", BuildStopHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/colonies$", ColoniesHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/colonies/({colony_id}[0-9]+)$", ColonyHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/colonies/({colony_id}[0-9]+)/attack$", ColonyAttackHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/combat-reports/({combat_report_id}[0-9]+)$", CombatReportHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/fleets/({fleet_id}[0-9]+)/orders$", FleetOrdersHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/scout-reports", ScoutReportsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/stars/({star_id}[0-9]+)/sit-reports", SitReportsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/alliances$", AlliancesHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/alliances/({alliance_id}[0-9]+)$", AllianceHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/alliances/({alliance_id}[0-9]+)/join-requests$", AllianceJoinRequestsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/alliances/({alliance_id}[0-9]+)/members$", AllianceMembersHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/sit-reports", SitReportsHandler.class));
        sRoutes.add(new Route("^/realms/[a-z]+/motd", MotdHandler.class));

        sRoutes.add(new Route("^/admin/?({path}.*)", HtmlPageHandler.class, "admin/"));
        sRoutes.add(new Route("^/intel/?({path}$)", HtmlPageHandler.class, "intel/"));
        sRoutes.add(new Route("^/intel/({path}.*)", StaticFileHandler.class, "intel/"));
        sRoutes.add(new Route("^/css/({path}.*)", StaticFileHandler.class, "css/"));
        sRoutes.add(new Route("^/js/({path}.*)", StaticFileHandler.class, "js/"));
        sRoutes.add(new Route("^/img/({path}.*)", StaticFileHandler.class, "img/"));
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        for (Route route : sRoutes) {
            Matcher matcher = route.pattern.matcher(target);
            if (matcher.find()) {
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

        handler.handle(matcher, route.extraOption, request, response);
    }

    private static class Route {
        public Pattern pattern;
        public Class<?> handlerClass;
        public String extraOption;

        public Route(String pattern, Class<?> handlerClass) {
            this(pattern, handlerClass, null);
        }
        public Route(String pattern, Class<?> handlerClass, String extraOption) {
            this.pattern = new Pattern(pattern);
            this.handlerClass = handlerClass;
            this.extraOption = extraOption;
        }
    }
}
