package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.metrics.ClientMetricsManager;

/**
 * This handler is where error reports from the client are posted.
 */
public class ClientMetricsHandler extends RequestHandler {
  private static final Log log = new Log("ClientMetricsManager");

  @Override
  public void post() throws RequestException {
    Messages.ClientMetrics.Builder clientMetrics =
        getRequestBody(Messages.ClientMetrics.class).toBuilder();

    // Make sure it has the correct empire ID set.
    clientMetrics.setEmpireId(getSession().getEmpireID());

    log.info("Got metrics from empire: %d", clientMetrics.getEmpireId());
    for (Messages.ApiRequestTiming timing : clientMetrics.getApiTimingsList()) {
      log.info("  [%s] %d : queue-time=%d response-time=%d request-size=%d response-size=%d",
          timing.getUrl(), timing.getResponseCode(), timing.getRequestQueueTimeMs(),
          timing.getResponseTimeMs(), timing.getRequestSize(), timing.getResponseSize());
    }

    ClientMetricsManager.i.recordClientMetrics(clientMetrics.build());
  }
}
