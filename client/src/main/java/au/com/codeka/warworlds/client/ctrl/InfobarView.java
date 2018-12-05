package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.net.ServerPacketEvent;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.Empire;

/**
 * The {@link InfobarView} shows some relevant information like your current empire and some info
 * about network requests and so on.
 */
public class InfobarView extends FrameLayout {
  /** Number of milliseconds to display the progress after receiving a packet. */
  private static final long PROGRESS_TIME_MS = 1000L;

  private final Handler handler;
  private final TextView empireName;
  private final ProgressBar working;

  private long lastPacketTime;

  public InfobarView(Context context, AttributeSet attrs) {
    super(context, attrs);

    handler = new Handler();

    inflate(context, R.layout.ctrl_infobar_view, this);
    empireName = findViewById(R.id.empire_name);
    working = findViewById(R.id.working);
  }

  public void hideEmpireName() {
    empireName.setVisibility(View.GONE);
  }

  /** Must be run on the UI thread. */
  private void refresh() {
    if (isInEditMode()) {
      return;
    }

    long now = System.currentTimeMillis();
    if (now - lastPacketTime < PROGRESS_TIME_MS) {
      working.setVisibility(View.VISIBLE);
    } else {
      working.setVisibility(View.GONE);
    }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (isInEditMode()) {
      return;
    }

    App.i.getEventBus().register(eventHandler);
    refreshEmpire(EmpireManager.i.getMyEmpire());
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (isInEditMode()) {
      return;
    }

    App.i.getEventBus().unregister(eventHandler);
  }

  private void refreshEmpire(Empire empire) {
    empireName.setText(empire.display_name);

    // set up the initial state
    refresh();
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      if (empire.id != null && empire.id.equals(EmpireManager.i.getMyEmpire())) {
        refreshEmpire(empire);
      }
    }

    @EventHandler(thread = Threads.UI)
    public void onPacket(ServerPacketEvent event) {
      lastPacketTime = System.currentTimeMillis();
      refresh();
      handler.postDelayed(() -> refresh(), PROGRESS_TIME_MS);
    }
  };
}
