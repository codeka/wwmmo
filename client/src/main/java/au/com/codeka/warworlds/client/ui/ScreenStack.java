package au.com.codeka.warworlds.client.ui;

import static au.com.codeka.warworlds.client.concurrency.Threads.checkOnThread;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.concurrency.Threads;
import java.util.Stack;

/**
 * {@link ScreenStack} is used to manage a stack of {@link Screen}s. The stack shows it's
 * corresponding view in the {@link ViewGroup} that the stack is created with.
 */
public class ScreenStack {
  private final Activity activity;
  private final ViewGroup container;
  private final Stack<Screen> screens = new Stack<>();

  public ScreenStack(Activity activity, ViewGroup container) {
    this.activity = activity;
    this.container = container;
  }

  /**
   * Push the given {@link Screen} onto the stack. The currently visible screen (if any) will
   * become hidden (though not destroyed).
   *
   * @param screen The {@link Screen} to push.
   */
  public void push(Screen screen) {
    checkOnThread(Threads.UI);

    if (!screens.isEmpty()) {
      Screen top = screens.peek();
      top.onHide();
    }

    if (screens.contains(screen)) {
      // If the screen is already on the stack, we'll just remove everything up to that screen
      while (screens.peek() != screen) {
        pop();
      }
    } else {
      screens.push(screen);
      screen.onCreate(context, container);
    }

    screen.performShow();
  }

  /**
   * Pop the top-most {@link Screen} from the stack.
   *
   * @return true if there's another {@link Screen} displaying, or false if we popped the last
   *     {@link Screen}.
   */
  public boolean pop() {
    Screen screen = screens.pop();
    if (screen == null) {
      return false;
    }

    screen.onHide();
    screen.onDestroy();
    container.removeAllViews();

    if (!screens.isEmpty()) {
      screen = screens.peek();
      screen.performShow();
      return true;
    }

    return false;
  }

  /**
   * Pop all screen from the stack, return to blank "home".
   */
  public void home() {
    while (pop()) {
      // Keep going.
    }
  }

  private final ScreenContext context = new ScreenContext() {
    @Override
    public void startActivity(Intent intent) {
      container.getContext().startActivity(intent);
    }

    @Override
    public void pushScreen(Screen screen) {
      push(screen);
    }

    @Override
    public Activity getActivity() {
      return activity;
    }
  };
}
