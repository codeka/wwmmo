package au.com.codeka.warworlds.server.world

import com.squareup.wire.Message
import java.util.*

/**
 * A watchable object is any object that's encapsulated in a protocol buffer, which listeners can
 * register to get change notifications for. When we update the underlying object, the listeners
 * are all notified and can act accordingly.
 */
class WatchableObject<T : Message<*, *>>(private var obj: T) {
  /** The interface you implement when you want to watch for changes to this object.  */
  interface Watcher<T : Message<*, *>> {
    fun onUpdate(obj: WatchableObject<T>)
  }

  private val watchers = ArrayList<Watcher<T>>()

  /**
   * An [Object] that you must use to lock access to this [WatchableObject], when you
   * want to modify it.
   */
  val lock = Any()

  fun get(): T {
    return obj
  }

  fun set(obj: T) {
    this.obj = obj
    synchronized(watchers) {
      for (watcher in watchers) {
        watcher.onUpdate(this)
      }
    }
  }

  fun addWatcher(watcher: Watcher<T>) {
    synchronized(watchers) { watchers.add(watcher) }
  }

  fun removeWatcher(watcher: Watcher<T>) {
    synchronized(watchers) { watchers.remove(watcher) }
  }
}