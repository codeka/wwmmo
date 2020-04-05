package au.com.codeka.warworlds.server.metrics;

import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

import au.com.codeka.common.Log;

/**
 * This class is responsible for taking metrics from a {@link MetricRegistry} and sending them to
 * a {@link MetricStore} every 5 minutes.
 */
class Reporter {
  private static final Log log = new Log("Reporter");

  private final MetricRegistry registry;
  private final MetricStore store;
  private final Thread thread;

  private boolean stopped = false;
  private final Object stopLock = new Object();

  public Reporter(MetricRegistry registry, MetricStore store) {
    this.registry = registry;
    this.store = store;
    this.thread = new Thread(this::threadProc);
  }

  public void start() {
    thread.start();
  }

  public void stop() {
    stopped = true;
    synchronized (stopLock) {
      stopLock.notify();
    }

    try {
      thread.wait(10000);
    } catch (InterruptedException e) {
      // Ignore
    }
  }

  /** Called every 5 minutes to report the stats to the {@link MetricStore}. */
  private void report() {
    log.debug("Taking snapshot.");
    store.next().populate(registry);
  }

  private void threadProc() {
    // Wait 20 seconds before taking the first snapshot.
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) { /* Ignore */}

    while (!stopped) {
      report();

      synchronized (stopLock) {
        try {
          stopLock.wait(TimeUnit.MINUTES.toMillis(5));
        } catch (InterruptedException e) { /* Ignore */ }
      }
    }
  }
}
