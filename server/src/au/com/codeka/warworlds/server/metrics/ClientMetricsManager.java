package au.com.codeka.warworlds.server.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import au.com.codeka.common.protobuf.Messages;

public class ClientMetricsManager {
  public static final ClientMetricsManager i = new ClientMetricsManager();

  private final Timer queueTime;
  private final Timer responseTime;
  private final Map<Integer, EmpireMetrics> empireRequests = new HashMap<>();
  private final Map<String, UrlMetrics> urlMetrics = new HashMap<>();

  private ClientMetricsManager() {
    MetricRegistry registry = MetricsManager.i.getMetricsRegistry();
    responseTime = registry.timer("client.response-time");
    queueTime = registry.timer("client.queue-time");
  }

  public void recordClientMetrics(Messages.ClientMetrics metrics) {
    for (Messages.ApiRequestTiming timing : metrics.getApiTimingsList()) {
      responseTime.update(timing.getResponseTimeMs(), TimeUnit.MILLISECONDS);
      queueTime.update(timing.getRequestQueueTimeMs(), TimeUnit.MILLISECONDS);

      synchronized (empireRequests) {
        EmpireMetrics empireMetrics = empireRequests.get(metrics.getEmpireId());
        if (empireMetrics == null) {
          empireMetrics =
              new EmpireMetrics(metrics.getEmpireId(), MetricsManager.i.getMetricsRegistry());
          empireRequests.put(metrics.getEmpireId(), empireMetrics);
        }

        empireMetrics.update(timing);
      }

      synchronized (urlMetrics) {
        UrlMetrics um = urlMetrics.get(timing.getUrl());
        if (um == null) {
          um = new UrlMetrics(timing.getUrl(), MetricsManager.i.getMetricsRegistry());
          urlMetrics.put(timing.getUrl(), um);
        }

        um.update(timing);
      }
    }
  }

  static class EmpireMetrics {
    public Timer queueTime;
    public Timer responseTime;

    public EmpireMetrics(int empireID, MetricRegistry registry) {
      queueTime =
          registry.timer(String.format(Locale.US, "client.empire[%d].queue-time", empireID));
      responseTime =
          registry.timer(String.format(Locale.US, "client.empire[%d].response-time", empireID));
    }

    public void update(Messages.ApiRequestTiming timings) {
      queueTime.update(timings.getRequestQueueTimeMs(), TimeUnit.MILLISECONDS);
      responseTime.update(timings.getResponseTimeMs(), TimeUnit.MILLISECONDS);
    }
  }

  static class UrlMetrics {
    public Timer responseTime;
    public Histogram requestSize;
    public Histogram responseSize;

    public UrlMetrics(String url, MetricRegistry registry) {
      responseTime = registry.timer(String.format(Locale.US, "client.url[%s].response-time", url));
      requestSize =
          registry.histogram(String.format(Locale.US, "client.url[%s].request-size", url));
      responseSize =
          registry.histogram(String.format(Locale.US, "client.url[%s].response-size", url));
    }

    public void update(Messages.ApiRequestTiming timing) {
      responseTime.update(timing.getResponseTimeMs(), TimeUnit.MILLISECONDS);
      requestSize.update(timing.getRequestSize());
      responseSize.update(timing.getResponseSize());
    }
  }
}
