package au.com.codeka.warworlds.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import android.os.Handler;
import android.os.Looper;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;

/**
 * Holds all the details about a single event handler.
 */
class EventHandlerInfo {
  private static final Log log = new Log("EventHandlerInfo");

  private final Class<?> eventClass;
  private final Method method;
  private final WeakReference<?> subscriber;
  private final boolean callOnUiThread;
  private final Handler handler;
  private int registerCount;

  public EventHandlerInfo(Class<?> eventClass, Method method, Object subscriber,
      boolean callOnUiThread) {
    this.eventClass = eventClass;
    this.method = method;
    this.subscriber = new WeakReference<>(subscriber);
    this.callOnUiThread = callOnUiThread;
    if (this.callOnUiThread) {
      handler = new Handler(Looper.getMainLooper());
    } else {
      handler = null;
    }
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
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
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
      }
    };

    // if we're scheduled to run on the UI thread, and we're *not* on the UI thread, then
    // post a message to run on the UI thread. If we're already on the UI then don't go through
    // the trouble of posting a message.
    if (handler != null && Looper.myLooper() != Looper.getMainLooper()) {
      handler.post(runnable);
    } else {
      runnable.run();
    }
  }
}
