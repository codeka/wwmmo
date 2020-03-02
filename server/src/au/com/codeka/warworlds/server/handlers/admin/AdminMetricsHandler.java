package au.com.codeka.warworlds.server.handlers.admin;

import com.codahale.metrics.servlets.MetricsServlet;

import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.metrics.MetricsManager;

public class AdminMetricsHandler extends AdminHandler {
  private static final Log log = new Log("AdminMetricsHandler");

  @Override
  protected void get() throws RequestException {
    try {
      MetricsServlet metricsServlet = new MetricsServlet();

      metricsServlet.init(new MyServletConfig());

      metricsServlet.service(getRequest(), getResponse());
    } catch (IOException | ServletException e) {
      throw new RequestException(e);
    }
  }

  private static final class MyServletConfig implements ServletConfig {
    private WebAppContext context = new WebAppContext();

    public MyServletConfig() {
      context.setAttribute(MetricsServlet.METRICS_REGISTRY, MetricsManager.i.getMetricsRegistry());
    }

    @Override
    public String getServletName() {
      return "metrics";
    }

    @Override
    public ServletContext getServletContext() {
      return context.getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
      return context.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
      return context.getInitParameterNames();
    }
  }
}
