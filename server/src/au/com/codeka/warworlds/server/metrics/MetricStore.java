package au.com.codeka.warworlds.server.metrics;

import au.com.codeka.common.protobuf.Messages;

/**
 * A very simple in-memory store of the last 48 hours worth of metrics.
 *
 * When the server shuts down, we attempt to save the current state to disk so that we can reload
 * it back later when the server starts up again.
 */
class MetricStore {
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
