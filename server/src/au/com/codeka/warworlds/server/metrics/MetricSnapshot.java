package au.com.codeka.warworlds.server.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.Map;

import javax.annotation.Nullable;

import au.com.codeka.common.protobuf.Messages;

public class MetricSnapshot {
  private Messages.MetricsSnapshot.Builder pb = Messages.MetricsSnapshot.newBuilder();

  public void load(Messages.MetricsSnapshot snapshot) {
    pb.mergeFrom(snapshot);
  }

  /**
   * Populate this {@link MetricSnapshot} with the current values stored in the given
   * {@link MetricRegistry}.
   */
  public void populate(MetricRegistry metrics) {
    pb.setTime(System.currentTimeMillis());

    pb.clearMetric();
    for (Map.Entry<String, Gauge> entry : metrics.getGauges().entrySet()) {
      long value = convertGaugeValue(entry.getValue().getValue());
      pb.addMetric(Messages.MetricsSnapshot.Metric.newBuilder()
          .setName(entry.getKey())
          .setGauge(Messages.MetricsSnapshot.Gauge.newBuilder().setValue(value))
          .build());
    }

    for (Map.Entry<String, Counter> entry : metrics.getCounters().entrySet()) {
      long value = convertGaugeValue(entry.getValue().getCount());
      pb.addMetric(Messages.MetricsSnapshot.Metric.newBuilder()
          .setName(entry.getKey())
          .setGauge(Messages.MetricsSnapshot.Gauge.newBuilder().setValue(value))
          .build());
    }

    for (Map.Entry<String, Timer> entry : metrics.getTimers().entrySet()) {
      final Timer timer = entry.getValue();
      pb.addMetric(Messages.MetricsSnapshot.Metric.newBuilder()
          .setName(entry.getKey())
          .setTimer(Messages.MetricsSnapshot.Timer.newBuilder()
              .setMeter(Messages.MetricsSnapshot.Meter.newBuilder()
                  .setCount(timer.getCount())
                  .setMeanRate(timer.getMeanRate())
                  .setM1Rate(timer.getOneMinuteRate())
                  .setM5Rate(timer.getFiveMinuteRate())
                  .setM15Rate(timer.getFifteenMinuteRate())
                  .build())
              .setHistogram(buildHistogram(timer.getSnapshot())))
          .build());
    }

    for (Map.Entry<String, Meter> entry : metrics.getMeters().entrySet()) {
      final Meter meter = entry.getValue();
      pb.addMetric(Messages.MetricsSnapshot.Metric.newBuilder()
          .setName(entry.getKey())
          .setMeter(Messages.MetricsSnapshot.Meter.newBuilder()
              .setCount(meter.getCount())
              .setMeanRate(meter.getMeanRate())
              .setM1Rate(meter.getOneMinuteRate())
              .setM5Rate(meter.getFiveMinuteRate())
              .setM15Rate(meter.getFifteenMinuteRate()))
          .build());
    }

    for (Map.Entry<String, Histogram> entry : metrics.getHistograms().entrySet()) {
      pb.addMetric(Messages.MetricsSnapshot.Metric.newBuilder()
          .setName(entry.getKey())
          .setHistogram(buildHistogram(entry.getValue().getSnapshot()))
          .build());
    }
  }

  public Messages.MetricsSnapshot build() {
    return pb.build();
  }

  private static Messages.MetricsSnapshot.Histogram.Builder buildHistogram(Snapshot snapshot) {
    return Messages.MetricsSnapshot.Histogram.newBuilder()
        .setMin(snapshot.getMin())
        .setMax(snapshot.getMax())
        .setMean(snapshot.getMean())
        .setMedian(snapshot.getMedian())
        .setStdDev(snapshot.getStdDev())
        .setP75(snapshot.get75thPercentile())
        .setP95(snapshot.get95thPercentile())
        .setP98(snapshot.get98thPercentile())
        .setP99(snapshot.get99thPercentile())
        .setP999(snapshot.get999thPercentile());
  }

  private static long convertGaugeValue(@Nullable Object value) {
    if (value == null) {
      return 0;
    }

    if (value instanceof Long) {
      return (Long) value;
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }

    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
