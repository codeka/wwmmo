package au.com.codeka.warworlds.intel;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;

public class RequestRouter extends AbstractHandler {
  private static final Log log = new Log("RequestRouter");
  private static final ArrayList<Route> routes = new ArrayList<Route>() {{
    add(new Route("/tiles/(?<zoom>[0-9]+)/(?<x>[0-9-]+)/(?<y>[0-9-]+)", TileHandler.class));
    add(new Route("/(?<path>.*)", StaticFileHandler.class));
  }};

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException {
    for (Route route : routes) {
      Matcher matcher = route.pattern.matcher(target);
      if (matcher.matches()) {
        handle(matcher, route, request, response);
        baseRequest.setHandled(true);
        return;
      }
    }

    log.info("Could not find handler for URL: %s", target);
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
    public java.util.regex.Pattern pattern;
    public Class<?> handlerClass;

    public Route(String pattern, Class<?> handlerClass) {
      this.pattern = Pattern.compile(pattern);
      this.handlerClass = handlerClass;
    }
  }
}
