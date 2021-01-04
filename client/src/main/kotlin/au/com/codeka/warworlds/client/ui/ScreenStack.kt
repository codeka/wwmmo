package au.com.codeka.warworlds.client.ui

import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.transition.Scene
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import au.com.codeka.warworlds.client.MainActivity
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.ui.ScreenStack
import au.com.codeka.warworlds.common.Log
import java.util.*

/**
 * [ScreenStack] is used to manage a stack of [Screen]s. The stack shows it's
 * corresponding view in the [ViewGroup] that the stack is created with.
 */
class ScreenStack(private val activity: MainActivity, private val container: ViewGroup) {
  interface ScreenStackStateUpdatedCallback {
    fun onStackChanged()
  }

  private val callbacks: MutableSet<ScreenStackStateUpdatedCallback>
  private val screens = Stack<ScreenHolder>()

  /**
   * Push the given [Screen] onto the stack. The currently visible screen (if any) will
   * become hidden (though not destroyed).
   *
   * @param screen The [Screen] to push.
   */
  @JvmOverloads
  fun push(screen: Screen, sharedViews: SharedViews? = null) {
    Threads.checkOnThread(Threads.UI)
    if (!screens.isEmpty()) {
      val top = screens.peek()
      top.screen.onHide()
    }
    var containsScreen = false
    for (screenHolder in screens) {
      if (screenHolder.screen === screen) {
        containsScreen = true
        break
      }
    }
    if (containsScreen) {
      // If the screen is already on the stack, we'll just remove everything up to that screen.
      while (screens.peek().screen !== screen) {
        pop()
      }
    } else {
      screens.push(ScreenHolder(screen, sharedViews))
      screen.onCreate(context, container)
    }
    performShow(screen, sharedViews, true)
    notifyStackChanged()
  }

  /**
   * Pop the top-most [Screen] from the stack.
   *
   * @return true if there's another [Screen] displaying, or false if we popped the last
   * [Screen].
   */
  fun pop(): Boolean {
    return popInternal(true)
  }

  /**
   * Peek the top-most [Screen] and return a reference to it.
   * @return The top-most [Screen], or null if there's no screen (which should be very rare).
   */
  fun peek(): Screen? {
    return if (screens.empty()) {
      null
    } else screens.peek().screen
  }

  /**
   * Implements "back" behaviour. This is basically "pop" but when we get to the last screen,
   * something a bit different happens: if the last screen is the "home" screen (of the passed-in
   * type), then we'll pop it and return false. Otherwise, the last screen will be replaced with
   * a new instance of the home screen.
   */
  fun backTo(homeScreen: Class<out Screen>): Boolean {
    if (screens.size == 1 && homeScreen.isInstance(screens[0].screen)) {
      return pop()
    } else if (screens.size == 1) {
      pop()
      try {
        push(homeScreen.newInstance())
      } catch (e: Exception) {
        log.error("Unexpected.", e)
        return false
      }
      return true
    }
    return pop()
  }

  /**
   * Pop all screen from the stack, return to blank "home".
   */
  fun home() {
    while (popInternal(false)) {
      // Keep going.
    }
  }

  /**
   * Returns the depth of the screen stack. 1 means there's only one screen, etc.
   */
  fun depth(): Int {
    return screens.size
  }

  fun addScreenStackStateUpdatedCallback(callback: ScreenStackStateUpdatedCallback) {
    synchronized(callbacks) { callbacks.add(callback) }
  }

  fun removeScreenStackStateUpdatedCallback(callback: ScreenStackStateUpdatedCallback) {
    synchronized(callbacks) { callbacks.remove(callback) }
  }

  private fun popInternal(transition: Boolean): Boolean {
    if (screens.empty()) {
      return false
    }
    var screenHolder = screens.pop()
    if (screenHolder == null) {
      notifyStackChanged()
      return false
    }
    val oldSharedViews = screenHolder.sharedViews
    screenHolder.screen.onHide()
    screenHolder.screen.onDestroy()
    if (!screens.isEmpty()) {
      screenHolder = screens.peek()
      performShow(screenHolder.screen, oldSharedViews, transition)
      notifyStackChanged()
      return true
    }
    container.removeAllViews()
    notifyStackChanged()
    return false
  }

  /**
   * Performs the "show". Calls [Screen.onShow] to get the view, then creates a [Scene]
   * (if needed), and transitions to it.
   */
  private fun performShow(screen: Screen, sharedViews: SharedViews?, transition: Boolean) {
    val showInfo = screen.onShow()
    val view = showInfo?.view
    if (view != null) {
      if (transition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        log.info(
            "Performing show of '%s' with: %s",
            screen.javaClass.simpleName,
            if (sharedViews == null) "no transition" else sharedViews.toDebugString(context.activity))
        val scene = Scene(container, view)
        val mainTransition = TransitionSet()

        // TODO: sometimes this would crash (e.g. when adding/removing list items). I don't know
        // why (why is it adding/remove list items from a list?) but we'll just disable the fade. It
        // kind of looks better without the fade anyway.
//        Transition fadeTransition = Transitions.fade().clone();
//        mainTransition.addTransition(fadeTransition);
        if (sharedViews != null) {
          val transformTransition = Transitions.transform().clone()
          mainTransition.addTransition(transformTransition)
          for (sharedView in sharedViews.sharedViews) {
            if (sharedView.viewId != 0) {
//              fadeTransition.excludeTarget(sharedView.getViewId(), true);
              transformTransition.addTarget(sharedView.viewId)
            } else {
              val name = "shared-" + RANDOM.nextLong()
              if (sharedView.fromViewId != 0 && sharedView.toViewId != 0) {
                container.findViewById<View>(sharedView.fromViewId)?.transitionName = name
                view.findViewById<View>(sharedView.toViewId)?.transitionName = name
              } else if (sharedView.fromView != null && sharedView.toViewId != 0) {
                sharedView.fromView.transitionName = name
                val toView = view.findViewById<View>(sharedView.toViewId)
                if (toView != null) {
                  toView.transitionName = name
                }
              } else {
                log.error("Unexpected SharedView configuration.")
              }
              //              fadeTransition.excludeTarget(name, true);
              transformTransition.addTarget(name)
            }
          }
        }
        TransitionManager.go(scene, mainTransition)
      } else {
        container.removeAllViews()
        container.addView(view)
      }
    } else {
      container.removeAllViews()
    }
    activity.setToolbarVisible(showInfo?.toolbarVisible ?: true)
  }

  private fun notifyStackChanged() {
    synchronized(callbacks) {
      for (callback in callbacks) {
        callback.onStackChanged()
      }
    }
  }

  /** Contains info we need about a [Screen] while it's on the stack.  */
  private class ScreenHolder(val screen: Screen, val sharedViews: SharedViews?)

  private val context: ScreenContext = object : ScreenContext {

    override fun startActivity(intent: Intent?) {
      activity.startActivity(intent)
    }

    override fun home() {
      this@ScreenStack.home()
    }

    override fun pushScreen(screen: Screen) {
      push(screen, null)
    }

    override fun pushScreen(screen: Screen, sharedViews: SharedViews?) {
      push(screen, sharedViews)
    }

    override fun popScreen() {
      pop()
    }

    override val activity: MainActivity
      get() = this@ScreenStack.activity
  }

  companion object {
    private val RANDOM = Random()
    private val log = Log("ScreenStack")
  }

  init {
    callbacks = HashSet()
  }
}