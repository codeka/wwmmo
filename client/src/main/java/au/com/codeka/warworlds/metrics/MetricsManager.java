package au.com.codeka.warworlds.metrics;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.model.EmpireManager;

/**
 * Records, and periodically sends, metrics from the client to the server so we can track it.
 */
// TODO: Expand the metrics we record.
// TODO: Save metrics to disk and send them on next start, so we don't miss stuff.
public class MetricsManager {
  private static final Log log = new Log("MetricsManager");
  public static MetricsManager i = new MetricsManager();

  private static final int MAX_METRICS_SIZE = 20;

  private final Object lock = new Object();
  private Messages.ClientMetrics.Builder metricsBuilder = null;

  private MetricsManager() {
  }

  /**
   * Called when an API request completes, includes the timing information for the request.
   */
  public void onApiRequestComplete(ApiRequest request) {
    Messages.ClientMetrics metricsToSend = null;
    synchronized (lock) {
      if (metricsBuilder == null) {
        metricsBuilder = Messages.ClientMetrics.newBuilder();
      } else if (metricsBuilder.getApiTimingsCount() > MAX_METRICS_SIZE) {
        metricsToSend = metricsBuilder.build();
        metricsBuilder.clear();
      }

      Messages.ApiRequestTiming timing = Messages.ApiRequestTiming.newBuilder()
          .setUrl(cleanUrl(request.url()))
          .setMethod(request.method().toUpperCase())
          .setTime(System.currentTimeMillis())
          .setResponseCode(request.responseCode())
          .setRequestSize(request.requestSize())
          .setResponseSize(request.responseSize())
          .setRequestQueueTimeMs(request.getTiming().getQueueTime())
          .setResponseTimeMs(request.getTiming().getResponseTime())
          .build();
      log.info("ApiRequestTiming %s", timing.toString());
      metricsBuilder.addApiTimings(timing);
    }

    if (metricsToSend != null) {
      sendMetrics(metricsToSend);
    }
  }

  private void sendMetrics(Messages.ClientMetrics metrics) {
    RequestManager.i.sendRequest(
        new ApiRequest.Builder("client-metrics", "POST")
            .body(metrics)
            .dontRecordMetrics()
            .build());
  }

  private String cleanUrl(String url) {
    return url.replaceAll("=[^&]*", "=___").replaceAll("-?[0-9]+", "NNN");
  }
}
