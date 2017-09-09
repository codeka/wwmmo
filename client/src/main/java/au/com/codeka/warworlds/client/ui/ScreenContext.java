package au.com.codeka.warworlds.client.ui;

import android.app.Activity;
import android.content.Intent;

/**
 * A context object that's passed to {@link Screen}s to allow them to access some Android
 * functionality (such as starting activities) or screen functionality (such as pushing new
 * screens, popping the backstack, etc).
 */
public interface ScreenContext {
  /** Start an activity with the given {@link Intent}. */
  void startActivity(Intent intent);

  /** Push a new screen onto the current stack. */
  void pushScreen(Screen screen);

  /** Gets the containing {@link Activity}. */
  Activity getActivity();
}
