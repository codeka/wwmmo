package au.com.codeka.warworlds.client.util.eventbus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An implementation of the "event bus" pattern.
 *
 * <p/>Basically, you register objects with the event bus that include public methods annotated with
 * the {@link EventHandlerInfo} annotation, and when events are fired, the corresponding
 * implementation is called for you.
 *
 * <p/>A couple of features make this more interesting than just holding callbacks directly:
 * <ol>
 *   <li>The callback objects are held with weak references, so you don't <i>have to</i> unregister
 *       them (though it's still a good idea to do it).
 *   <li>The {@link EventHandler} annotation can specify whether you require the callback on the UI
 *       thread, or another thread, or whatever.
 *   <li>The implementation handles all the details of handling multiple callbacks and so on.
 * </ol>
 */
public class EventBus {
  private final List<EventHandlerInfo> handlers = new CopyOnWriteArrayList<>();

  /** Subscribe the given object to the event bus. */
  public void register(Object subscriber) {
    // If it's already registered, just increment it's register count.
    boolean alreadyRegistered = false;
    for (EventHandlerInfo handler : handlers) {
      Object existingSubscriber = handler.getSubscriber();
      if (existingSubscriber != null && existingSubscriber == subscriber) {
        handler.register();
        alreadyRegistered = true;
      }
    }
    if (alreadyRegistered) {
      return;
    }

    int numMethods = 0;
    for (Method method : subscriber.getClass().getDeclaredMethods()) {
      EventHandler eventHandlerAnnotation = method.getAnnotation(EventHandler.class);
      if (eventHandlerAnnotation == null) {
        continue;
      }

      Class<?>[] parameters = method.getParameterTypes();
      if (parameters.length != 1) {
        throw new IllegalArgumentException("EventHandler method must have exactly one parameter.");
      }
      if (parameters[0].isPrimitive()) {
        throw new IllegalArgumentException(
            "EventHandler method's parameter must not be a primitive type.");
      }

      numMethods++;
      handlers.add(new EventHandlerInfo(
          parameters[0], method, subscriber, eventHandlerAnnotation.thread()));
    }

    if (numMethods == 0) {
      // If you don't have any @EventHandler methods, then there's no point registering
      // the object. Usually this means you made a programming error.
      throw new NoEventHandlerMethods();
    }
  }

  /**
   * Unregisters the given object from the event bus.
   */
  public void unregister(Object subscriber) {
    ArrayList<EventHandlerInfo> kill = new ArrayList<EventHandlerInfo>();
    for (EventHandlerInfo handler : handlers) {
      Object existingSubscriber = handler.getSubscriber();
      if (existingSubscriber == null // Remove dead subscribers while we're here....
          || (existingSubscriber == subscriber && handler.unregister() == 0)) {
        kill.add(handler);
      }
    }

    handlers.removeAll(kill);
  }

  /** Publish the given event and call all subscribers. */
  public void publish(Object event) {
    if (event == null) {
      throw new IllegalArgumentException("Event cannot be null!");
    }
    for (EventHandlerInfo handler : handlers) {
      if (handler.handles(event)) {
        handler.call(event);
      }
    }
  }

  public static class NoEventHandlerMethods extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NoEventHandlerMethods() {
      super("Tried to register a class with no @EventHandler methods on EventBus.");
    }
  }
}
