package au.com.codeka.warworlds.model;

import android.content.ContextWrapper;
import android.os.Handler;
import android.view.View;
import au.com.codeka.common.Log;

public class BaseManager {
    private static final Log log = new Log("BaseManager");

    /**
     * Attempts to execute the given \c Runnable instance on whatever type \c context is. For
     * example, if it's a View, we'll run it on the View's main looper thread.
     */
    protected void fireHandler(Object context, final Runnable handler) {
        try {
            if (context instanceof ContextWrapper) {
                ContextWrapper ctx = (ContextWrapper) context;
                new Handler(ctx.getMainLooper()).post(handler);
            } else if (context instanceof View) {
                View view = (View) context;
                new Handler(view.getContext().getMainLooper()).post(handler);
            } else {
                handler.run();
            }
        } catch (Exception e) {
            log.warning("Ignored exception in onStarFetched...", e);
        }
    }
}
