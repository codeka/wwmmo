package au.com.codeka.warworlds.client.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import au.com.codeka.warworlds.client.concurrency.Threads;

import static au.com.codeka.warworlds.client.concurrency.Threads.checkOnThread;

/**
 * {@link ScreenStack} is used to manage a stack of {@link Screen}s. The stack shows it's
 * corresponding view in the {@link ViewGroup} that the stack is created with.
 */
public class ScreenStack {
  private final ViewGroup container;
  private final Stack<Screen> screens = new Stack<>();

  public ScreenStack(ViewGroup container) {
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
      container.removeAllViews();
    }

    if (screens.contains(screen)) {
      // If the screen is already on the stack, we'll just remove everything up to that screen
      while (screens.peek() != screen) {
        pop();
      }
    } else {
      screens.push(screen);
      screen.onCreate(this);
      screen.createView(LayoutInflater.from(container.getContext()), container);
    }

    View view = screen.onShow();
    if (view != null) {
      container.addView(view);
    }
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
      View view = screen.onShow();
      if (view != null) {
        container.addView(view);
      }
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
}
