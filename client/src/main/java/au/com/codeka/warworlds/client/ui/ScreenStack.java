package au.com.codeka.warworlds.client.ui;

import android.content.Intent;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Stack;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.concurrency.Threads;

import static au.com.codeka.warworlds.client.concurrency.Threads.checkOnThread;

/**
 * {@link ScreenStack} is used to manage a stack of {@link Screen}s. The stack shows it's
 * corresponding view in the {@link ViewGroup} that the stack is created with.
 */
public class ScreenStack {
  private final AppCompatActivity activity;
  private final ViewGroup container;
  private final Stack<ScreenHolder> screens = new Stack<>();

  public ScreenStack(AppCompatActivity activity, ViewGroup container) {
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

    if (screens.contains(screen)) {
      // If the screen is already on the stack, we'll just remove everything up to that screen
      while (screens.peek().screen != screen) {
        pop();
      }
    } else {
      screens.push(new ScreenHolder(screen, sharedViews));
      screen.onCreate(context, container);
    }

    screen.performShow(sharedViews);
  }

  /**
   * Pop the top-most {@link Screen} from the stack.
   *
   * @return true if there's another {@link Screen} displaying, or false if we popped the last
   *     {@link Screen}.
   */
  public boolean pop() {
    ScreenHolder screenHolder = screens.pop();
    if (screenHolder == null) {
      return false;
    }

    screenHolder.screen.onHide();
    screenHolder.screen.onDestroy();

    if (!screens.isEmpty()) {
      screenHolder = screens.peek();
      screenHolder.screen.performShow(screenHolder.sharedViews);
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
    public AppCompatActivity getActivity() {
      return activity;
    }
  };
}
