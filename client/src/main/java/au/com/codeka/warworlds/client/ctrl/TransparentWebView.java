package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import com.google.common.io.CharStreams;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * A {@link WebView} with a transparent background. It's a little tricky and the method for getting
 * a transparent background seems to be totally different depending on which version of the Android
 * API your device is.
 */
public class TransparentWebView extends WebView {
  public TransparentWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTransparent();
  }

  @Override
  public void loadData(String data, String mimeType, String encoding) {
    super.loadData(data, mimeType, encoding);
    setTransparent();
  }

  @Override
  public void loadDataWithBaseURL(
      String baseUrl,
      String data,
      String mimeType,
      String encoding,
      String historyUrl) {
    super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    setTransparent();
  }

  /**
   * A helper that loads a template HTML (from your assets folder) and then replaces the string "%s"
   * in that template with the HTML you've supplied.
   */
  public void loadHtml(String templateFileName, String html) {
    String tmpl = getHtmlFile(getContext(), templateFileName);
    html = String.format(tmpl, html);

    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
  }

  /** Loads a template HTML file from within your assets folder. */
  public static String getHtmlFile(Context context, String fileName) {
    try {
      AssetManager assetManager = context.getAssets();
      InputStream is = assetManager.open(fileName);
      return CharStreams.toString(new InputStreamReader(is, "utf-8"));
    } catch (Exception e) {
      // any errors (shouldn't be...) and we'll return a "blank" template.
      return "";
    }
  }

  private void setTransparent() {
    setBackgroundColor(Color.TRANSPARENT);

    // This is required to make the background of the WebView actually transparent on Honeycomb+
    // (this API is only available on Honeycomb+ as well, so we need to call it via reflection...):
    // motdView.setLayerType(View.LAYER_TYPE_SOFTWARE, new Paint());
    try {
      Method setLayerType = View.class.getMethod("setLayerType", int.class, Paint.class);
      if (setLayerType != null) {
        setLayerType.invoke(this, 1, new Paint());
      }
    } catch (Exception e) {
      // ignore if the method isn't supported on this platform...
    }
  }
}
