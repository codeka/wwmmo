package au.com.codeka.warworlds.client.util.eventbus

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * An implementation of the "event bus" pattern.
 *
 *
 * Basically, you register objects with the event bus that include public methods annotated with
 * the [EventHandler] annotation, and when events are fired, the corresponding implementation
 * is called for you.
 *
 *
 * A couple of features make this more interesting than just holding callbacks directly:
 *
 *  1. The callback objects are held with weak references, so you don't *have to* unregister
 * them (though it's still a good idea to do it).
 *  1. The [EventHandler] annotation can specify whether you require the callback on the UI
 * thread, or another thread, or whatever.
 *  1. The implementation handles all the details of handling multiple callbacks and so on.
 *
 */
class EventBus {
  private val handlers: MutableList<EventHandlerInfo> = CopyOnWriteArrayList()

  /** Subscribe the given object to the event bus.  */
  fun register(subscriber: Any) {
    // If it's already registered, just increment it's register count.
    var alreadyRegistered = false
    for (handler in handlers) {
      val existingSubscriber = handler.getSubscriber()
      if (existingSubscriber != null && existingSubscriber === subscriber) {
        handler.register()
        alreadyRegistered = true
      }
    }
    if (alreadyRegistered) {
      return
    }
    var numMethods = 0
    for (method in subscriber.javaClass.declaredMethods) {
      val eventHandlerAnnotation = method.getAnnotation(EventHandler::class.java) ?: continue
      val parameters = method.parameterTypes
      require(parameters.size == 1) { "EventHandler method must have exactly one parameter." }
      require(!parameters[0].isPrimitive) { "EventHandler method's parameter must not be a primitive type." }
      numMethods++
      handlers.add(EventHandlerInfo(
          parameters[0], method, subscriber, eventHandlerAnnotation.thread))
    }
    if (numMethods == 0) {
      // If you don't have any @EventHandler methods, then there's no point registering
      // the object. Usually this means you made a programming error.
      throw NoEventHandlerMethodsException()
    }
  }

  /**
   * Unregisters the given object from the event bus.
   */
  fun unregister(subscriber: Any) {
    val kill = ArrayList<EventHandlerInfo>()
    for (handler in handlers) {
      val existingSubscriber = handler.getSubscriber()
      if (existingSubscriber == null // Remove dead subscribers while we're here....
          || existingSubscriber === subscriber && handler.unregister() == 0) {
        kill.add(handler)
      }
    }
    handlers.removeAll(kill)
  }

  /** Publish the given event and call all subscribers.  */
  fun publish(event: Any?) {
    requireNotNull(event) { "Event cannot be null!" }
    for (handler in handlers) {
      if (handler.handles(event)) {
        handler.call(event)
      }
    }
  }

  class NoEventHandlerMethodsException : RuntimeException() {
  }
}