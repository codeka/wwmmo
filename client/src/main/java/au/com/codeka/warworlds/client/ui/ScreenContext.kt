package au.com.codeka.warworlds.client.ui

import android.content.Intent
import au.com.codeka.warworlds.client.MainActivity

/**
 * A context object that's passed to [Screen]s to allow them to access some Android
 * functionality (such as starting activities) or screen functionality (such as pushing new
 * screens, popping the backstack, etc).
 */
interface ScreenContext {
  /** Start an activity with the given [Intent].  */
  fun startActivity(intent: Intent?)

  /** Pop all screens until you get an empty stack.  */
  fun home()

  /** Push a new screen onto the current stack.  */
  fun pushScreen(screen: Screen)

  /** Push a new screen onto the current stack.  */
  fun pushScreen(screen: Screen, sharedViews: SharedViews?)

  /** Pop the current screen off the stack.  */
  fun popScreen()

  /** Gets the containing [MainActivity].  */
  val activity: MainActivity
}