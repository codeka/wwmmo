package au.com.codeka.warworlds.server.handlers;

import java.util.List;
import java.util.regex.Matcher;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;

/**
 * Base {@link javax.servlet.Servlet} for working with handlers.
 */
public class HandlerServlet extends GenericServlet {
  private Log log = new Log("HandlerServlet");

  private final List<Route> routes;

  protected HandlerServlet(List<Route> routes) {
    this.routes = routes;
  }

  @Override
  public void service(ServletRequest request, ServletResponse response) {
    for (Route route : routes) {
      Matcher matcher = route.matches((HttpServletRequest) request);
      if (matcher != null) {
        handle(matcher, route, (HttpServletRequest) request, (HttpServletResponse) response);
        return;
      }
    }

    log.info(
        String.format(
            "Could not find handler for URL: %s",
            ((HttpServletRequest) request).getPathInfo()));
    ((HttpServletResponse) response).setStatus(404);
  }

  protected void handle(
      Matcher matcher,
      Route route,
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      RequestHandler handler = route.createRequestHandler();
      handler.setup(matcher, route.getExtraOption(), request, response);
      handler.handle();
    } catch (RequestException e) {
      e.populate(response);
    }
  }
}
