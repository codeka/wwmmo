package au.com.codeka.warworlds.server.handlers.admin;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.metrics.MetricsManager;

public class AdminMetricsHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    setResponseBody(MetricsManager.i.getMetrics());
  }
}
