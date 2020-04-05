package au.com.codeka.warworlds.server.metrics;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;

/**
 * A very simple in-memory store of the last 48 hours worth of metrics.
 *
 * When the server shuts down, we attempt to save the current state to disk so that we can reload
 * it back later when the server starts up again.
 */
class MetricStore {
  private static final Log log = new Log("MetricStore");
  private final Object lock = new Object();

  // 5 minutes per snapshot * 576 entries = 2 days worth of data.
  private final MetricSnapshot[] entries = new MetricSnapshot[576];

  // A pointer to the "current" entry in the entries array that represents the latest snapshot
  // we've taken.
  private int ptr;

  public MetricStore() {
    // TODO: load from disk or something.
    for (int i = 0; i < entries.length; i++) {
      entries[i] = new MetricSnapshot();
    }
    ptr = 0;
  }

  /** Start the store by loading saved values from disk. */
  public void start() {
    Messages.MetricsHistory metrics;

    File baseDir = new File(Configuration.i.getRequestStatsDirectory());
    File file = new File(baseDir, "metrics.pb");
    log.info("Attempting to load metrics from: %s", file.getAbsolutePath());
    if (file.exists()) {
      try {
        FileInputStream ins = new FileInputStream(file);
        metrics = Messages.MetricsHistory.parseFrom(ins);
      } catch (IOException e) {
        log.warning("Error parsing proto from file: ", e);
        return;
      }
    } else {
      log.warning("No saved metrics, nothing to do.");
      return;
    }

    for (Messages.MetricsSnapshot snapshot : Lists.reverse(metrics.getSnapshotList())) {
      if (snapshot.getMetricCount() == 0) {
        continue;
      }

      next().load(snapshot);
    }

    // Note: if the last snapshot was taken a while ago, there could be a large gap between the
    // stats. For now, we're just going to live with that.
  }

  /** Stop the store by saving current values to disk. */
  public void stop() {
    File baseDir = new File(Configuration.i.getRequestStatsDirectory());
    File file = new File(baseDir, "metrics.pb");
    log.info("Saving metrics to: %s", file.getAbsolutePath());
    File parent = file.getParentFile();
    if (!parent.exists()) {
      if (!parent.mkdirs()) {
        log.warning("Couldn't create parent directory. This is probably going to fail.");
      }
    }

    try {
      FileOutputStream outs = new FileOutputStream(file);
      outs.write(build().toByteArray());
      outs.close();
    } catch (IOException e) {
      log.error("Error writing stats.", e);
    }
  }

  /**
   * Increment the pointer and return the next {@link MetricSnapshot}, ready to be populated with
   * current data.
   */
  public MetricSnapshot next() {
    synchronized (lock) {
      ptr++;
      if (ptr >= entries.length) {
        ptr = 0;
      }
      return entries[ptr];
    }
  }

  /** Build the last 48 hours of metrics. */
  public Messages.MetricsHistory build() {
    Messages.MetricsHistory.Builder history = Messages.MetricsHistory.newBuilder();
    synchronized (lock) {
      for (int i = 0; i < entries.length; i++) {
        int index = ptr - i;
        if (index < 0) {
          index += entries.length;
        }

        history.addSnapshot(entries[index].build());
      }
    }

    return history.build();
  }
}
