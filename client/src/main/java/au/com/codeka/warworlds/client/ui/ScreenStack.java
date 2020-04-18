package au.com.codeka.warworlds.client.ui;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.transition.Scene;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.common.Log;

import static au.com.codeka.warworlds.client.concurrency.Threads.checkOnThread;

/**
 * {@link ScreenStack} is used to manage a stack of {@link Screen}s. The stack shows it's
 * corresponding view in the {@link ViewGroup} that the stack is created with.
 */
public class ScreenStack {
  private static final Random RANDOM = new Random();
  private static final Log log = new Log("ScreenStack");

  public interface ScreenStackStateUpdatedCallback {
    void onStackChanged();
  }

  private final Set<ScreenStackStateUpdatedCallback> callbacks;
  private final MainActivity activity;
  private final ViewGroup container;
  private final Stack<ScreenHolder> screens = new Stack<>();

  public ScreenStack(MainActivity activity, ViewGroup container) {
    this.activity = activity;
    this.container = container;
    callbacks = new HashSet<>();
  }

  /**
   * Push the given {@link Screen} onto the stack. The currently visible screen (if any) will
   * become hidden (though not destroyed).
   *
   * @param screen The {@link Screen} to push.
   */
  public void push(Screen screen) {
    push(screen, null);
  }

  /**
   * Push the given {@link Screen} onto the stack. The currently visible screen (if any) will
   * become hidden (though not destroyed).
   *
   * @param screen The {@link Screen} to push.
   */
  public void push(Screen screen, @Nullable SharedViews sharedViews) {
    checkOnThread(Threads.UI);

    if (!screens.isEmpty()) {
      ScreenHolder top = screens.peek();
      top.screen.onHide();
    }

    boolean containsScreen = false;
    for (ScreenHolder screenHolder : screens) {
      if (screenHolder.screen == screen) {
        containsScreen = true;
        break;
      }
    }
    if (containsScreen) {
      // If the screen is already on the stack, we'll just remove everything up to that screen.
      while (screens.peek().screen != screen) {
        pop();
      }
    } else {
      screens.push(new ScreenHolder(screen, sharedViews));
      screen.onCreate(context, container);
    }

    performShow(screen, sharedViews, true);
    notifyStackChanged();
  }

  /**
   * Pop the top-most {@link Screen} from the stack.
   *
   * @return true if there's another {@link Screen} displaying, or false if we popped the last
   *     {@link Screen}.
   */
  public boolean pop() {
    return popInternal(true);
  }

  /**
   * Peek the top-most {@link Screen} and return a reference to it.
   * @return The top-most {@link Screen}, or null if there's no screen (which should be very rare).
   */
  @Nullable
  public Screen peek() {
    if (screens.empty()) {
      return null;
    }

    return screens.peek().screen;
  }

  /**
   * Implements "back" behaviour. This is basically "pop" but when we get to the last screen,
   * something a bit different happens: if the last screen is the "home" screen (of the passed-in
   * type), then we'll pop it and return false. Otherwise, the last screen will be replaced with
   * a new instance of the home screen.
   */
  public boolean backTo(Class<? extends Screen> homeScreen) {
    if (screens.size() == 1 && homeScreen.isInstance(screens.get(0).screen)) {
      return pop();
    } else if (screens.size() == 1) {
      pop();
      try {
        push(homeScreen.newInstance());
      } catch (Exception e) {
        log.error("Unexpected.", e);
        return false;
      }
      return true;
    }

    return pop();
  }

  /**
   * Pop all screen from the stack, return to blank "home".
   */
  public void home() {
    while (popInternal(false)) {
      // Keep going.
    }
  }

  /**
   * Returns the depth of the screen stack. 1 means there's only one screen, etc.
   */
  public int depth() {
    return screens.size();
  }

  public void addScreenStackStateUpdatedCallback(ScreenStackStateUpdatedCallback callback) {
    synchronized (callbacks) {
      callbacks.add(callback);
    }
  }

  public void removeScreenStackStateUpdatedCallback(ScreenStackStateUpdatedCallback callback) {
    synchronized (callbacks) {
      callbacks.remove(callback);
    }
  }

  private boolean popInternal(boolean transition) {
    if (screens.empty()) {
      return false;
    }

    ScreenHolder screenHolder = screens.pop();
    if (screenHolder == null) {
      notifyStackChanged();
      return false;
    }

    SharedViews oldSharedViews = screenHolder.sharedViews;
    screenHolder.screen.onHide();
    screenHolder.screen.onDestroy();

    if (!screens.isEmpty()) {
      screenHolder = screens.peek();
      performShow(screenHolder.screen, oldSharedViews, transition);
      notifyStackChanged();
      return true;
    }
    container.removeAllViews();
    notifyStackChanged();
    return false;
  }

  /**
   * Performs the "show". Calls {@link Screen#onShow} to get the view, then creates a {@link Scene}
   * (if needed), and transitions to it.
   */
  private void performShow(Screen screen, @Nullable SharedViews sharedViews, boolean transition) {
    ShowInfo showInfo = screen.onShow();
    View view = showInfo.getView();
    if (view != null) {
      if (transition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        log.info(
            "Performing show of '%s' with: %s",
            screen.getClass().getSimpleName(),
            sharedViews == null
                ? "no transition"
                : sharedViews.toDebugString(context.getActivity()));
        Scene scene = new Scene(container, view);
        TransitionSet mainTransition = new TransitionSet();

        // TODO: sometimes this would crash (e.g. when adding/removing list items). I don't know
        // why (why is it adding/remove list items from a list?) but we'll just disable the fade. It
        // kind of looks better without the fade anyway.
//        Transition fadeTransition = Transitions.fade().clone();
//        mainTransition.addTransition(fadeTransition);

        if (sharedViews != null) {
          Transition transformTransition = Transitions.transform().clone();
          mainTransition.addTransition(transformTransition);
          for (SharedViews.SharedView sharedView : sharedViews.getSharedViews()) {
            if (sharedView.getViewId() != 0) {
//              fadeTransition.excludeTarget(sharedView.getViewId(), true);
              transformTransition.addTarget(sharedView.getViewId());
            } else {
              String name = "shared-" + RANDOM.nextLong();
              if (sharedView.getFromViewId() != 0 && sharedView.getToViewId() != 0) {
                container.findViewById(sharedView.getFromViewId()).setTransitionName(name);
                view.findViewById(sharedView.getToViewId()).setTransitionName(name);
              } else if (sharedView.getFromView() != null && sharedView.getToViewId() != 0) {
                sharedView.getFromView().setTransitionName(name);
                View toView = view.findViewById(sharedView.getToViewId());
                if (toView != null) {
                  toView.setTransitionName(name);
                }
              } else {
                log.error("Unexpected SharedView configuration.");
              }
//              fadeTransition.excludeTarget(name, true);
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

    activity.setToolbarVisible(showInfo.getToolbarVisible());
  }

  private void notifyStackChanged() {
    synchronized (callbacks) {
      for (ScreenStackStateUpdatedCallback callback : callbacks) {
        callback.onStackChanged();
      }
    }
  }

  /** Contains info we need about a {@link Screen} while it's on the stack. */
  private static class ScreenHolder {
    private final Screen screen;
    @Nullable private final SharedViews sharedViews;

    public ScreenHolder(Screen screen, @Nullable SharedViews sharedViews) {
      this.screen = screen;
      this.sharedViews = sharedViews;
    }
  }

  private final ScreenContext context = new ScreenContext() {
    @Override
    public void startActivity(Intent intent) {
      container.getContext().startActivity(intent);
    }

    @Override
    public void home() { ScreenStack.this.home(); }

    @Override
    public void pushScreen(Screen screen) {
      push(screen, null);
    }

    @Override
    public void pushScreen(Screen screen, SharedViews sharedViews) {
      push(screen, sharedViews);
    }

    @Override
    public void popScreen() {
      pop();
    }

    @Override
    public MainActivity getActivity() {
      return activity;
    }
  };
}
