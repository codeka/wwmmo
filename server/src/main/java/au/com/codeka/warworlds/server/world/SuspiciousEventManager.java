package au.com.codeka.warworlds.server.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

import javax.annotation.concurrent.GuardedBy;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;
import au.com.codeka.warworlds.server.concurrency.TaskRunner;
import au.com.codeka.warworlds.server.concurrency.Threads;
import au.com.codeka.warworlds.server.proto.SuspiciousEvent;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.Pair;

/**
 * A manager for managing the suspicious event store.
 */
public class SuspiciousEventManager {
  private static final Log log = new Log("SuspiciousEventManager");
  public static final SuspiciousEventManager i = new SuspiciousEventManager();

  /**
   * Delay from receiving the first suspicious modification until we store it. To avoid overloading
   * the store with lots of stores.
   */
  private final static int STORE_DELAY_MS = 30000;

  /**
   * A queue of suspicious events that we're waiting to add to the store. The long in the pair is
   * for the timestamp of the event.
   */
  private final Queue<Pair<Long, SuspiciousModificationException>> suspiciousModificationExceptionQueue =
      new ArrayDeque<>();

  @GuardedBy("suspiciousModificationExceptionQueue")
  private boolean storeTaskQueued;

  /**
   * Adds a suspicious event to the suspicious event store.
   *
   * @param e The {@link SuspiciousModificationException} representing this suspicious event.
   */
  public void addSuspiciousEvent(SuspiciousModificationException e) {
    synchronized (suspiciousModificationExceptionQueue) {
      suspiciousModificationExceptionQueue.add(new Pair<>(System.currentTimeMillis(), e));
      if (!storeTaskQueued) {
        TaskRunner.i.runTask(storeQueuedTask, Threads.BACKGROUND, STORE_DELAY_MS);
        storeTaskQueued = true;
      }
    }
  }

  public Collection<SuspiciousEvent> query(/* TODO: parameters */) {
    // If there's pending events to be stored, just store them now.
    synchronized (suspiciousModificationExceptionQueue) {
      if (storeTaskQueued) {
        storeQueuedTask.run();
      }
    }

    return DataStore.i.suspiciousEvents().query();
  }

  private final Runnable storeQueuedTask = () -> {
    ArrayList<Pair<Long, SuspiciousModificationException>> exceptions = new ArrayList<>();
    synchronized (suspiciousModificationExceptionQueue) {
      exceptions.addAll(suspiciousModificationExceptionQueue);
      suspiciousModificationExceptionQueue.clear();
      storeTaskQueued = false;
    }

    ArrayList<SuspiciousEvent> events = new ArrayList<>();
    for (Pair<Long, SuspiciousModificationException> pair : exceptions) {
      long timestamp = pair.one;
      SuspiciousModificationException e = pair.two;
      events.add(new SuspiciousEvent.Builder()
          .timestamp(timestamp)
          .star_id(e.getStarId())
          .modification(e.getModification())
          .message(e.getMessage())
          .build());
    }
    DataStore.i.suspiciousEvents().add(events);
  };
}
