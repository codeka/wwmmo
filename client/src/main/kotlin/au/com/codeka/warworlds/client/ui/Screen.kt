package au.com.codeka.warworlds.client.ui

import android.view.ViewGroup

/**
 * A [Screen] is similar to a fragment, in that it's a place to keep the business logic
 * of a view.
 *
 *
 * Unlike a Fragment, though, a Screen's lifecycle is very simple. It's created, shown, hidden
 * and destroyed. Once created, it can be shown and hidden multiple times (for example as you
 * navigate the backstack, it might be hidden and then shown again).
 */
abstract class Screen {
  /** Called before anything else. */
  open fun onCreate(context: ScreenContext, container: ViewGroup) {}

  /**
   * Called when the screen is shown. Returns the [ShowInfo] that describes what we should show.
   */
  open fun onShow(): ShowInfo? {
    return null
  }

  open fun onHide() {}
  open fun onDestroy() {}

  /** Get the title we should display in the toolbar.  */
  open val title: CharSequence?
    get() = null
}