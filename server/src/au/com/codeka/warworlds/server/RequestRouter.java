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


public class RequestRouter extends AbstractHandler {
    private final Logger log = LoggerFactory.getLogger(RequestRouter.class);
    private static ArrayList<Route> sRoutes;

    {
        sRoutes = new ArrayList<Route>();
        sRoutes.add(new Route("^/realms/([^/]+)/hello", HelloHandler.class));
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        for (Route route : sRoutes) {
            Matcher matcher = route.pattern.matcher(target);
            if (matcher.find()) {
                handle(route, request, response);
                baseRequest.setHandled(true);
                return;
            }
        }

        response.setStatus(404);
    }

    private void handle(Route route, HttpServletRequest request,
                        HttpServletResponse response) {
        RequestHandler handler;
        try {
            handler = (RequestHandler) route.handlerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return; // TODO: error
        }

        handler.handle(request, response);
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
