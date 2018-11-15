package au.com.codeka.warworlds.client.game.chat;

import android.content.Context;
import android.transition.TransitionManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.viewpager.widget.ViewPager;

import au.com.codeka.warworlds.client.R;

/**
 * Layout for {@link ChatScreen}.
 */
public class ChatLayout extends RelativeLayout {
  interface Callbacks {
    void onSend(String msg);
  }

  private final Callbacks callbacks;
  private final ViewPager viewPager;
  private final FrameLayout bottomPane;

  public ChatLayout(Context context, Callbacks callbacks) {
    super(context);
    this.callbacks = callbacks;
    inflate(context, R.layout.chat, this);
    setBackgroundColor(context.getResources().getColor(R.color.default_background));

    viewPager = findViewById(R.id.pager);
    bottomPane = findViewById(R.id.bottom_pane);
    showSendPane();
  }

  /** Show the default, send, pane. */
  private void showSendPane() {
    SendBottomPane sendBottomPane = new SendBottomPane(getContext(), callbacks::onSend);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(sendBottomPane);
  }
}
