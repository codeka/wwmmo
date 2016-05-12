package au.com.codeka.warworlds.ctrl;

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

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.RequestManager;

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

  private static boolean interceptorAdded;
  private static final ArrayList<MessageInfo> messages = new ArrayList<>();

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

      if (!interceptorAdded) {
        interceptorAdded = true;
        RequestManager.i.addInterceptor(requestInterceptor);
      }
      isAttached = true;

      queueRefresh();
    }
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    if (!isInEditMode()) {
      isAttached = false;
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
      if (messages.get(i).millis > 0 && messages.get(i).createTime < old) {
        messages.remove(i);
        i--;
      }
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
    canvas.drawText(String.format("%d", dalvikKb / 1024), (0.00f + 0.167f) * width, height - 10, p);
    canvas.drawText(String.format("%d", nativeKb / 1024), (0.33f + 0.167f) * width, height - 10, p);
    canvas.drawText(String.format("%d", otherKb / 1024), (0.66f + 0.167f) * width, height - 10, p);
    return bmp;
  }

  private static final Interceptor requestInterceptor = new Interceptor() {
    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();

      MessageInfo msg = new MessageInfo();
      msg.request = request;
      synchronized (messages) {
        messages.add(0, msg);
        while (messages.size() > 8) {
          messages.remove(messages.size() - 1);
        }
      }

      long startTime = SystemClock.elapsedRealtime();
      Response response = chain.proceed(request);
      long endTime = SystemClock.elapsedRealtime();

      msg.millis = endTime - startTime;

      return response;
    }
  };

  private static class MessageInfo {
    public long createTime;
    @Nullable public Request request;
    public long millis;

    public MessageInfo() {
      createTime = SystemClock.elapsedRealtime();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (request != null) {
        String url = request.url().getPath();
        String realmUrl = RealmContext.i.getCurrentRealm().getBaseUrl().getPath().toString();
        if (url.startsWith(realmUrl)) {
          url = url.substring(realmUrl.length());
        }
        if (request.url().getQuery() != null) {
          url += "?" + request.url().getQuery();
        }

        if (millis == 0) {
          sb.append(">> ");
          sb.append(url);
        } else {
          sb.append("<< ");
          sb.append(millis);
          sb.append("ms ");
          sb.append(url);
        }
      }
      return sb.toString();
    }
  }
}
