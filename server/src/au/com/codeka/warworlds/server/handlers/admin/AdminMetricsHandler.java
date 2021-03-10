package au.com.codeka.warworlds.server.handlers.admin;

import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.Map;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.metrics.MetricsManager;

public class AdminMetricsHandler extends AdminHandler {
  private static final Log log = new Log("AdminMetricsHandler");

  @Override
  public void get() throws RequestException {
    Messages.MetricsHistory metricsHistory = MetricsManager.i.getMetrics();

    Map<String, String[]> params = getRequest().getParameterMap();
    if (params.containsKey("subset[]")) {
      HashSet<String> metricNames = new HashSet<>(Lists.newArrayList(params.get("subset[]")));

      // Filter the snapshot so it only includes the given names.
      Messages.MetricsHistory.Builder filteredHistory = Messages.MetricsHistory.newBuilder();
      for (Messages.MetricsSnapshot snapshot : metricsHistory.getSnapshotList()) {
        Messages.MetricsSnapshot.Builder filteredSnapshot =
            snapshot.toBuilder()
                .clearMetric();
        for (Messages.MetricsSnapshot.Metric metric : snapshot.getMetricList()) {
          if (metricNames.contains(metric.getName())) {
            filteredSnapshot.addMetric(metric);
          }
        }
        filteredHistory.addSnapshot(filteredSnapshot.build());
      }

      setResponseBody(filteredHistory.build());
    } else {
      setResponseBody(metricsHistory);
    }
  }
}
