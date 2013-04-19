package au.com.codeka.warworlds.ctrl;

import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;

/**
 * A \c WebView with a transparent background. It's a little tricky and the method
 * for getting a transparent background seems to be totally different depending on
 * which version of the Android API your device is.
 */
public class TransparentWebView extends WebView {
    private static Logger log = LoggerFactory.getLogger(TransparentWebView.class);

    public TransparentWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTransparent();
    }

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        super.loadData(data, mimeType, encoding);
        setTransparent();
    }

    /**
     * A helper that loads a template HTML (from your assets folder) and then replaces
     * the string "%s" in that template with the HTML you've supplied.
     */
    public void loadHtml(String templateFileName, String html) {
        String tmpl = getHtmlFile(getContext(), templateFileName);
        html = String.format(tmpl, html);
        html = Base64.encodeToString(html.getBytes(), Base64.DEFAULT);
        log.debug(String.format("Loading HTML (template=%s)", templateFileName));

        String url = "data:text/html;charset=utf-8;base64,"+html;
        log.info(url);

        loadUrl(url);
    }

    /**
     * Loads a template HTML file from within your assets folder.
     */
    public static String getHtmlFile(Context context, String fileName) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open(fileName);

            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer);
            return writer.toString();
        } catch (Exception e) {
            // any errors (shouldn't be...) and we'll return a "blank" template.
            return "";
        }
    }

    private void setTransparent() {
        setBackgroundColor(Color.TRANSPARENT);

        // this is required to make the background of the WebView actually transparent
        // on Honeycomb+ (this API is only available on Honeycomb+ as well, so we need
        // to call it via reflection...):
        // motdView.setLayerType(View.LAYER_TYPE_SOFTWARE, new Paint());
        try {
            Method setLayerType = View.class.getMethod("setLayerType", int.class, Paint.class);
            if (setLayerType != null) {
                setLayerType.invoke(this, 1, new Paint());
          }
        // ignore if the method isn't supported on this platform...
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
    }
}
