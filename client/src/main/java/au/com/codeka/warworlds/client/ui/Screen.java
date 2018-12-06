package au.com.codeka.warworlds.client.ui;

import android.view.ViewGroup;

/**
 * A {@link Screen} is similar to a fragment, in that it's a place to keep the business logic
 * of a view.
 *
 * <p>Unlike a Fragment, though, a Screen's lifecycle is very simple. It's created, shown, hidden
 * and destroyed. Once created, it can be shown and hidden multiple times (for example as you
 * navigate the backstack, it might be hidden and then shown again).
 */
public abstract class Screen {
  /**
   * Called before anything else.
   */
  public void onCreate(ScreenContext context, ViewGroup container) {
  }

  /**
   * Called when the screen is shown. Returns the {@link ShowInfo} that describes what we should
   * show.
   */
  public ShowInfo onShow() {
    return null;
  }

  public void onHide() {
  }

  public void onDestroy() {
  }
}
