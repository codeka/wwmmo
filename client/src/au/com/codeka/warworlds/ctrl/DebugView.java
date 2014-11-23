package au.com.codeka.warworlds.ctrl;

import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

  // Maximum values for the various memory stats we track, for the 'water level' bars.
  private static long maxDalvikKb;
  private static long maxNativeKb;
  private static long maxOtherKb;

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
      isAttached = true;

      refresh(RequestManager.getCurrentState());
      queueRefresh();
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

  private void queueRefresh() {
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
    String str = String.format(Locale.ENGLISH, "Sim: %d Conn: %d",
        Simulation.getNumRunningSimulations(), state.numInProgressRequests);
    connectionInfo.setText(str);

    ImageView memoryGraph = (ImageView) view.findViewById(R.id.memory_graph);
    memoryGraph.setImageBitmap(createMemoryGraph(memoryGraph.getWidth(), memoryGraph.getHeight()));
  }

  /**
   * Draws a bar graph with current memory usage split between dalvik, native and "other" (i.e.
   * GL memory, mostly). Shows a watermark at the current maximum for each of those bars as well.
   */
  private Bitmap createMemoryGraph(int width, int height) {
    if (width == 0 || height == 0) {
      return null;
    }

    // Grab the current memory snapshot and update the maximums, if needed.
    Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
    Debug.getMemoryInfo(memoryInfo);
    long dalvikKb = memoryInfo.dalvikPrivateDirty;
    long nativeKb = memoryInfo.nativePrivateDirty;
    long otherKb = memoryInfo.otherPrivateDirty;
    if (maxDalvikKb < dalvikKb) {
      maxDalvikKb = dalvikKb;
    }
    if (maxNativeKb < nativeKb) {
      maxNativeKb = nativeKb;
    }
    if (maxOtherKb < otherKb) {
      maxOtherKb = otherKb;
    }

    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);

    Paint p = new Paint();
    p.setARGB(80, 0, 0, 0);
    canvas.drawRect(new Rect(0, 0, width, height), p);

    double max = Math.max(maxDalvikKb, Math.max(maxNativeKb, maxOtherKb));

    // green = dalvik memory
    p.setARGB(255, 0, 100, 0);
    canvas.drawRect(new Rect(0, height - (int) ((dalvikKb / max) * height),
        (int) (width * 0.33), height), p);
    canvas.drawRect(new Rect(0, height - (int) ((maxDalvikKb / max) * height),
        (int) (width * 0.33), height - (int) ((maxDalvikKb / max) * height) + 2), p);

    // blue = native memory
    p.setARGB(255, 100, 100, 255);
    canvas.drawRect(new Rect((int) (width * 0.33) + 1, height - (int) ((nativeKb / max) * height),
        (int) (width * 0.66), height), p);
    canvas.drawRect(new Rect((int) (width * 0.33) + 1,
        height - (int) ((maxNativeKb / max) * height),
        (int) (width * 0.66), height - (int)((maxNativeKb / max) * height) + 2), p);

    // red = other (= GL)
    p.setARGB(255, 255, 100, 100);
    canvas.drawRect(new Rect((int) (width * 0.66) + 1, height - (int) ((otherKb / max) * height),
        width, height), p);
    canvas.drawRect(new Rect((int) (width * 0.66) + 1,
        height - (int) ((maxOtherKb / max) * height),
        width, height - (int) ((maxOtherKb / max) * height) + 2), p);

    p.setARGB(255, 255, 255, 255);
    p.setTextSize(16);
    p.setTextAlign(Paint.Align.CENTER);
    canvas.drawText(String.format("%d", dalvikKb / 1024), (0.00f + 0.167f) * width, height - 10, p);
    canvas.drawText(String.format("%d", nativeKb / 1024), (0.33f + 0.167f) * width, height - 10, p);
    canvas.drawText(String.format("%d", otherKb / 1024), (0.66f + 0.167f) * width, height - 10, p);
    return bmp;
  }
}
