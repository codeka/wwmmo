package au.com.codeka.warworlds.server.metrics;

import com.codahale.metrics.MetricRegistry;

import au.com.codeka.common.protobuf.Messages;

public class MetricsManager {
  public static final MetricsManager i = new MetricsManager();

  private final MetricStore store = new MetricStore();
  private final MetricRegistry metrics = new MetricRegistry();
  private final Reporter reporter = new Reporter(metrics, store);

  private MetricsManager() {
  }

  public MetricRegistry getMetricsRegistry() {
    return metrics;
  }

  /** Returns the last 48 hours of metrics, as a series of snapshots taken every 5 minutes. */
  public Messages.MetricsHistory getMetrics() {
    return store.build();
  }

  public void start() {
    store.start();
    reporter.start();
  }

  public void stop() {
    reporter.stop();
    store.stop();
  }
}
