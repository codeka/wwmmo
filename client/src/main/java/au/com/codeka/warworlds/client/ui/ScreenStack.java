package au.com.codeka.warworlds.client.ui;

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
  private final Stack<Screen> screens;
  private final Map<Class<Screen>, Screen> createdScreens = new HashMap<>();

  public ScreenStack(ViewGroup container) {
    this.container = container;
    this.screens = new Stack<>();
  }

  /**
   * Push the given {@link Screen} onto the stack. The currently visible screen (if any) will
   * become hidden (though not destroyed).
   *
   * @param screenClass The class of the {@link Screen} to push.
   * @param params The {@link ScreenParameters} to pass to the screen.
   */
  public void push(Class<Screen> screenClass, ScreenParameters params) {
    checkOnThread(Threads.UI);

    if (!screens.isEmpty()) {
      Screen top = screens.peek();
      top.onHide();
      container.removeAllViews();
    }

    Screen screen = createdScreens.get(screenClass);
    if (screen == null) {
      try {
        screen = screenClass.newInstance();
        createdScreens.put(screenClass, screen);
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException("Could not create Screen.", e);
      }
    }


  }

  /**
   * Pop the top-most {@link Screen} from the stack.
   *
   * @return true if there's another {@link Screen} displaying, or false if we popped the last
   *     {@link Screen}.
   */
  public boolean pop() {
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
