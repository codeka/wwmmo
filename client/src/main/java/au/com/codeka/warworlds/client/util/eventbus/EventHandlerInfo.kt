package au.com.codeka.warworlds.client.util.eventbus

import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.common.Log
import java.lang.ref.WeakReference
import java.lang.reflect.Method

/**
 * Holds all the details about a single event handler.
 */
internal class EventHandlerInfo(private val eventClass: Class<*>, private val method: Method, subscriber: Any,
                                callOnThread: Threads) {
  private val subscriber: WeakReference<*>
  private val callOnThread: Threads
  private var registerCount: Int
  fun handles(event: Any?): Boolean {
    return eventClass.isInstance(event)
  }

  fun register(): Int {
    return ++registerCount
  }

  fun unregister(): Int {
    return --registerCount
  }

  /** Gets the subscriber object, may be null.  */
  fun getSubscriber(): Any? {
    return subscriber.get()
  }

  /**
   * Calls the subscriber's method with the given event object, on the UI thread if needed.
   */
  fun call(event: Any?) {
    val callLocation = Exception("Location of EventHandlerInfo.call()")
    val runnable = Runnable {
      val subscriber = subscriber.get()
      if (subscriber != null) {
        try {
          method.invoke(subscriber, event)
        } catch (e: Exception) {
          log.error("Exception caught handling event.", e)
          log.error("Call location.", callLocation)
        }
      }
    }

    // If it's meant to run on the current thread (and *not* a background thread) then just run it
    // directly. If it's meant to run on a background thread, we'll want it to run on a *different*
    // background thread.
    if (callOnThread.isCurrentThread && callOnThread != Threads.BACKGROUND) {
      runnable.run()
    } else {
      callOnThread.run(runnable)
    }
  }

  companion object {
    private val log = Log("EventHandlerInfo")
  }

  init {
    this.subscriber = WeakReference(subscriber)
    this.callOnThread = callOnThread
    registerCount = 1
  }
}