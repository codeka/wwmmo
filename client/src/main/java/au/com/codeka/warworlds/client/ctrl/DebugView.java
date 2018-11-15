package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.net.ServerPacketEvent;
import au.com.codeka.warworlds.client.opengl.FrameCounter;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;

/**
 * This is a view that's displayed over the top of the activity and shows up a little bit of
 * debugging information.
 */
public class DebugView extends FrameLayout {
  private View view;
  private Handler handler;
  private boolean isAttached;
  @Nullable private FrameCounter frameCounter;

  // Maximum values for the various memory stats we track, for the 'water level' bars.
  private static long maxDalvikKb;
  private static long maxNativeKb;
  private static long maxOtherKb;

  private static final ArrayList<MessageInfo> messages = new ArrayList<>();

  public DebugView(Context context) {
    this(context, null);
  }

  public DebugView(Context context, AttributeSet attrs) {
    super(context, attrs);

    view = inflate(context, R.layout.ctrl_debug, null);
    addView(view);
  }

  public void setFrameCounter(@NonNull FrameCounter frameCounter) {
    this.frameCounter = Preconditions.checkNotNull(frameCounter);
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (!isInEditMode()) {
      handler = new Handler();
      isAttached = true;

      App.i.getEventBus().register(eventListener);

      queueRefresh();
    }
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (!isInEditMode()) {
      isAttached = false;
      App.i.getEventBus().unregister(eventListener);
    }
  }

  private void queueRefresh() {
    if (!isAttached) {
      return;
    }

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        refresh();
        queueRefresh();
      }
    }, 1000);
  }

  public void refresh() {
    ImageView memoryGraph = (ImageView) view.findViewById(R.id.memory_graph);
    memoryGraph.setImageBitmap(createMemoryGraph(memoryGraph.getWidth(), memoryGraph.getHeight()));

    LinearLayout messagesContainer = (LinearLayout) view.findViewById(R.id.messages);
    for (int i = 0; i < messagesContainer.getChildCount(); i++) {
      TextView tv = (TextView) messagesContainer.getChildAt(i);
      if (messages.size() > i) {
        tv.setText(messages.get(i).toString());
      } else {
        tv.setText("");
      }
    }

    long old = SystemClock.elapsedRealtime() - 5000;
    for (int i = 0; i < messages.size(); i++) {
      if (messages.get(i).createTime < old) {
        messages.remove(i);
        i--;
      }
    }

    FrameCounter counter = frameCounter;
    if (counter != null) {
      ((TextView) findViewById(R.id.fps)).setText(
          String.format(Locale.US, "%.1f fps", counter.getFramesPerSecond()));
    } else {
      ((TextView) findViewById(R.id.fps)).setText("");
    }
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
    canvas.drawText(
        String.format(Locale.US, "%d", dalvikKb / 1024), (0.00f + 0.167f) * width, height - 10, p);
    canvas.drawText(
        String.format(Locale.US, "%d", nativeKb / 1024), (0.33f + 0.167f) * width, height - 10, p);
    canvas.drawText(
        String.format(Locale.US, "%d", otherKb / 1024), (0.66f + 0.167f) * width, height - 10, p);
    return bmp;
  }

  private static class MessageInfo {
    public long createTime;
    public String msg;

    public MessageInfo(String msg) {
      this.msg = msg;
      createTime = SystemClock.elapsedRealtime();
    }

    @Override
    public String toString() {
      return msg;
    }
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onServerPacketEvent(ServerPacketEvent event) {
      StringBuilder sb = new StringBuilder();
      switch(event.getDirection()) {
        case Sent:
          sb.append(">> ");
          break;
        case Received:
          sb.append("<< ");
          break;
      }
      sb.append(event.getPacketDebug());

      messages.add(new MessageInfo(sb.toString()));
    }
  };
}
