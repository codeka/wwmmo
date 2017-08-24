package au.com.codeka.warworlds.server.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import javax.annotation.concurrent.GuardedBy;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;
import au.com.codeka.warworlds.server.concurrency.TaskRunner;
import au.com.codeka.warworlds.server.concurrency.Threads;
import au.com.codeka.warworlds.server.proto.SuspiciousEvent;
import au.com.codeka.warworlds.server.store.DataStore;

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

  /** A queue of suspicious events that we're waiting to add to the store. */
  private final Queue<SuspiciousModificationException> suspiciousModificationExceptionQueue =
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
      suspiciousModificationExceptionQueue.add(e);
      if (!storeTaskQueued) {
        TaskRunner.i.runTask(storeQueuedTask, Threads.BACKGROUND, STORE_DELAY_MS);
        storeTaskQueued = true;
      }
    }
  }

  private final Runnable storeQueuedTask = () -> {
    ArrayList<SuspiciousModificationException> exceptions = new ArrayList<>();
    synchronized (suspiciousModificationExceptionQueue) {
      exceptions.addAll(suspiciousModificationExceptionQueue);
      suspiciousModificationExceptionQueue.clear();
      storeTaskQueued = false;
    }

    ArrayList<SuspiciousEvent> events = new ArrayList<>();
    for (SuspiciousModificationException e : exceptions) {
      events.add(new SuspiciousEvent.Builder()
          .star_id(e.getStarId())
          .modification(e.getModification())
          .message(e.getMessage())
          .build());
    }
    DataStore.i.suspiciousEvents().add(events);
  };
}
