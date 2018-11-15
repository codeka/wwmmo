package au.com.codeka.warworlds.client.ui;

import android.os.Build;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;

import java.util.Random;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;

/**
 * A {@link Screen} is similar to a fragment, in that it's a place to keep the business logic
 * of a view.
 *
 * <p>Unlike a Fragment, though, a Screen's lifecycle is very simple. It's created, shown, hidden
 * and destroyed. Once created, it can be shown and hidden multiple times (for example as you
 * navigate the backstack, it might be hidden and then shown again).
 */
public abstract class Screen {
  private static final Random RANDOM = new Random();
  private static final Log log = new Log("Screen");

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
            if (sharedView.getViewId() != 0) {
              fadeTransition.excludeTarget(sharedView.getViewId(), true);
              transformTransition.addTarget(sharedView.getViewId());
            } else {
              String name = "shared-" + Long.toString(RANDOM.nextLong());
              if (sharedView.getFromViewId() != 0 && sharedView.getToViewId() != 0) {
                container.findViewById(sharedView.getFromViewId()).setTransitionName(name);
                view.findViewById(sharedView.getToViewId()).setTransitionName(name);
              } else if (sharedView.getFromView() != null && sharedView.getToViewId() != 0) {
                sharedView.getFromView().setTransitionName(name);
                view.findViewById(sharedView.getToViewId()).setTransitionName(name);
              } else {
                log.error("Unexpected SharedView configuration.");
              }
              fadeTransition.excludeTarget(name, true);
              transformTransition.addTarget(name);
            }
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
