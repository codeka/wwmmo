package au.com.codeka.warworlds.server.world;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.squareup.wire.Message;

import java.util.ArrayList;

import javax.annotation.Nonnull;

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

  private final ArrayList<Watcher<T>> watchers = new ArrayList<>();
  private T object;

  /**
   * An {@link Object} that you must use to lock access to this {@link WatchableObject}, when you
   * want to modify it.
   */
  public final Object lock = new Object();

  public WatchableObject(T object) {
    this.object = Preconditions.checkNotNull(object);
  }

  @Nonnull
  public T get() {
    return object;
  }

  public void set(T obj) {
    this.object = obj;
    synchronized (watchers) {
      for (Watcher<T> watcher : watchers) {
        watcher.onUpdate(this);
      }
    }
  }

  public void addWatcher(Watcher<T> watcher) {
    synchronized (watchers) {
      watchers.add(watcher);
    }
  }

  public void removeWatcher(Watcher<T> watcher) {
    synchronized (watchers) {
      watchers.remove(watcher);
    }
  }
}
