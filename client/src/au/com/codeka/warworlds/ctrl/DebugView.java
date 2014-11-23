package au.com.codeka.warworlds.ctrl;

import java.util.Locale;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.eventbus.EventHandler;

/**
 * This is a view that's displayed over all activities and shows up a little bit of debugging
 * information.
 */
public class DebugView extends FrameLayout {
  private View view;
  private Handler handler;
  private boolean isAttached;

  public DebugView(Context context) {
    this(context, null);
  }

  public DebugView(Context context, AttributeSet attrs) {
    super(context, attrs);

    view = inflate(context, R.layout.debug_ctrl, null);
    addView(view);
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (!isInEditMode()) {
      handler = new Handler();

      RequestManager.eventBus.register(eventHandler);
      refresh(RequestManager.getCurrentState());

      isAttached = true;
    }
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (!isInEditMode()) {
      RequestManager.eventBus.unregister(eventHandler);
      isAttached = false;
    }
  }

  private final Object eventHandler = new Object() {
    @EventHandler(thread = EventHandler.UI_THREAD)
    public void onRequestManagerStateChanged(RequestManager.RequestManagerStateEvent event) {
      refresh(event);
    }
  };

  public void queueRefresh() {
    if (!isAttached) {
      return;
    }

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        refresh(RequestManager.getCurrentState());
        queueRefresh();
      }
    }, 1000);
  }

  public void refresh(RequestManager.RequestManagerStateEvent state) {
    TextView connectionInfo = (TextView) view.findViewById(R.id.connection_info);
    String str = String.format(Locale.ENGLISH, "Sim: %d Conn: %d Mem: %.1f MB",
        Simulation.getNumRunningSimulations(), state.numInProgressRequests,
        Debug.getNativeHeapSize() / 1024.02f / 1024.0f);
    connectionInfo.setText(str);
  }
}
