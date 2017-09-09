package au.com.codeka.warworlds.client.ui;

import android.os.Build;
import android.support.annotation.CallSuper;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.activity.Transitions;

/**
 * A {@link Screen} is similar to a fragment, in that it's a place to keep the business logic
 * of a view.
 *
 * <p>Unlike a Fragment, though, a Screen's lifecycle is very simple. It's created, shown, hidden
 * and destroyed. Once created, it can be shown and hidden multiple times (for example as you
 * navigate the backstack, it might be hidden and then shown again).
 */
public abstract class Screen {
  private ViewGroup container;

  /**
   * The {@link Scene} representing this screen. We use this to transition between this screen
   * and other screens.
   */
  private Scene scene;

  private SharedViews sharedViews;

  /**
   * Called before anything else.
   */
  @CallSuper
  public void onCreate(ScreenContext context, ViewGroup container) {
    this.container = container;
  }

  /**
   * Performs the "show". Calls {@link #onShow} to get the view, then creates a {@link Scene} (if
   * needed), and transitions to it.
   */
  public void performShow(@Nullable SharedViews sharedViews) {
    View view = onShow();
    if (view != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (scene == null) {
          scene = new Scene(container, view);
        }
        TransitionSet mainTransition = new TransitionSet();
        Transition fadeTransition = Transitions.fade().clone();
        mainTransition.addTransition(fadeTransition);

        if (sharedViews != null) {
          Transition transformTransition = Transitions.transform().clone();
          mainTransition.addTransition(transformTransition);
          for (SharedViews.SharedView sharedView : sharedViews.getSharedViews()) {
            fadeTransition.excludeTarget(sharedView.getViewId(), true);
            transformTransition.addTarget(sharedView.getViewId());
          }
        }
        TransitionManager.go(scene, mainTransition);
      } else {
        container.removeAllViews();
        container.addView(view);
      }
    } else {
      container.removeAllViews();
    }
  }

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
