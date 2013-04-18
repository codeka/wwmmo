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

import au.com.codeka.warworlds.server.handlers.*;
import au.com.codeka.warworlds.server.handlers.pages.*;


public class RequestRouter extends AbstractHandler {
    private static ArrayList<Route> sRoutes;

    {
        sRoutes = new ArrayList<Route>();
        sRoutes.add(new Route("^/login$", LoginHandler.class));
        sRoutes.add(new Route("^/realms/({realm}[^/]+)/devices/({id}[0-9]+)$", DevicesHandler.class));
        sRoutes.add(new Route("^/realms/({realm}[^/]+)/devices$", DevicesHandler.class));
        sRoutes.add(new Route("^/realms/({realm}[^/]+)/hello$", HelloHandler.class));
        sRoutes.add(new Route("^/realms/({realm}[^/]+)/empires$", EmpiresHandler.class));

        sRoutes.add(new Route("^/admin/({path}.*)", HtmlPageHandler.class));
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

        handler.handle(matcher, request, response);
    }

    private static class Route {
        public Pattern pattern;
        public Class<?> handlerClass;

        public Route(String pattern, Class<?> handlerClass) {
            this.pattern = new Pattern(pattern);
            this.handlerClass = handlerClass;
        }
    }
}
