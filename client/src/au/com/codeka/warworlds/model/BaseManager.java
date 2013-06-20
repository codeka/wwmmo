package au.com.codeka.warworlds.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContextWrapper;
import android.os.Handler;
import android.view.View;

public class BaseManager {
    private static final Logger log = LoggerFactory.getLogger(StarManager.class);

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
            log.warn("Ignored exception in onStarFetched...", e);
        }
    }
}
