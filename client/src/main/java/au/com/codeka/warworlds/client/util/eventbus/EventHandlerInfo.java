package au.com.codeka.warworlds.client.util.eventbus;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;

/**
 * Holds all the details about a single event handler.
 */
class EventHandlerInfo {
  private static final Log log = new Log("EventHandlerInfo");

  private final Class<?> eventClass;
  private final Method method;
  private final WeakReference<?> subscriber;
  private final Threads callOnThread;
  private int registerCount;

  public EventHandlerInfo(Class<?> eventClass, Method method, Object subscriber,
      Threads callOnThread) {
    this.eventClass = eventClass;
    this.method = method;
    this.subscriber = new WeakReference<>(subscriber);
    this.callOnThread = callOnThread;
    registerCount = 1;
  }

  public boolean handles(Object event) {
    return eventClass.isInstance(event);
  }

  public int register() {
    return ++registerCount;
  }

  public int unregister() {
    return --registerCount;
  }

  /** Gets the subscriber object, may be null. */
  @Nullable
  public Object getSubscriber() {
    return subscriber.get();
  }

  /**
   * Calls the subscriber's method with the given event object, on the UI thread if needed.
   */
  public void call(final Object event) {
    final Exception callLocation = new Exception("Location of EventHandlerInfo.call()");
    final Runnable runnable = () -> {
      final Object subscriber = EventHandlerInfo.this.subscriber.get();
      if (subscriber == null) {
        return;
      }
      try {
        method.invoke(subscriber, event);
      } catch (Exception e) {
        log.error("Exception caught handling event.", e);
        log.error("Call location.", callLocation);
      }
    };

    // If it's meant to run on the current thread (and *not* a background thread) then just run it
    // directly. If it's meant to run on a background thread, we'll want it to run on a *different*
    // background thread.
    if (callOnThread.isCurrentThread() && callOnThread != Threads.BACKGROUND) {
      runnable.run();
    } else {
      callOnThread.run(runnable);
    }
  }
}
