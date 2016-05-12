package au.com.codeka.warworlds.server.world;

import com.squareup.wire.Message;

import java.util.ArrayList;

/**
 * A watchable object is any object that's encapsulated in a protocol buffer, which listeners can
 * register to get change notifications for. When we update the underlying object, the listeners
 * are all notified and can act accordingly.
 */
public class WatchableObject<T extends Message> {
  /** The interface you implement when you want to watch for changes to this object. */
  public interface Watcher<T extends Message> {
    void onUpdate(WatchableObject<T> object);
  }

  private final ArrayList<Watcher> watchers = new ArrayList<>();
  private final long id;
  private T object;

  public WatchableObject(long id, T object) {
    this.id = id;
    this.object = object;
  }

  public T get() {
    return object;
  }

  public void set(T obj) {
    this.object = obj;
    synchronized (watchers) {
      for (Watcher watcher : watchers) {
        watcher.onUpdate(this);
      }
    }
  }

  public void addWatcher(Watcher watcher) {
    synchronized (watchers) {
      watchers.add(watcher);
    }
  }

  public void removeWatcher(Watcher watcher) {
    synchronized (watchers) {
      watchers.remove(watcher);
    }
  }
}
