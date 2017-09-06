package au.com.codeka.warworlds.client.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import javax.annotation.Nullable;

/**
 * A {@link Screen} is similar to a fragment, in that it's a place to keep the business logic
 * of a view.
 *
 * <p>Unlike a Fragment, though, a Screen's lifecycle is very simple. It's created, shown, hidden
 * and destroyed. Once created, it can be shown and hidden multiple times (for example as you
 * navigate the backstack, it might be hidden and then shown again).
 */
public abstract class Screen {

  /** Called before anything else. */
  public void onCreate(ScreenStack screenStack) {
  }

  /**
   * Create the view for this {@link Screen}. This will be called after {@link #onCreate} and
   * before the first {@link #onShow}. The {@link View} will be reused for subsequent show/hide,
   * and destroyed before {@link #onDestroy} is called.
    */
  public abstract View createView(LayoutInflater inflater, ViewGroup container);

  /**
   * Called when the screen is shown. Returns the view we should add to the contain (can be null,
   * however, in which case the contain will be empty).
   */
  @Nullable
  public View onShow() {
    return null;
  }

  public void onHide() {
  }

  public void onDestroy() {
  }
}
