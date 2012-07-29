package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;

public class StarManager {
    private static StarManager sInstance = new StarManager();
    public static StarManager getInstance() {
        return sInstance;
    }

    private static final Logger log = LoggerFactory.getLogger(StarManager.class);
    private TreeMap<String, Star> mStars;
    private TreeMap<String, List<StarFetchedHandler>> mStarUpdatedListeners;

    private StarManager() {
        mStars = new TreeMap<String, Star>();
        mStarUpdatedListeners = new TreeMap<String, List<StarFetchedHandler>>();
    }

    /**
     * Call this to register your interest in when a particular star is updated. Any time that
     * star is re-fetched from the server, your \c StarFetchedHandler will be called.
     */
    public void addStarUpdatedListener(String starKey, StarFetchedHandler handler) {
        synchronized(mStarUpdatedListeners) {
            List<StarFetchedHandler> listeners = mStarUpdatedListeners.get(starKey);
            if (listeners == null) {
                listeners = new ArrayList<StarFetchedHandler>();
                mStarUpdatedListeners.put(starKey, listeners);
            }
            listeners.add(handler);
        }
    }

    /**
     * Removes the given \c StarFetchedHandler from receiving updates about refreshed stars.
     */
    public void removeStarUpdatedListener(StarFetchedHandler handler) {
        synchronized(mStarUpdatedListeners) {
            for (Object o : IteratorUtils.toList(mStarUpdatedListeners.keySet().iterator())) {
                String starKey = (String) o;

                List<StarFetchedHandler> listeners = mStarUpdatedListeners.get(starKey);
                listeners.remove(handler);

                if (listeners.isEmpty()) {
                    mStarUpdatedListeners.remove(starKey);
                }
            }
        }
    }

    protected void fireStarUpdated(Star star) {
        synchronized(mStarUpdatedListeners) {
            List<StarFetchedHandler> listeners = mStarUpdatedListeners.get(star.getKey());
            if (listeners != null) {
                for (StarFetchedHandler handler : listeners) {
                    handler.onStarFetched(star);
                }
            }
        }
    }

    public void refreshStar(Star s) {
        refreshStar(s.getKey());
    }

    public void refreshStar(String starKey) {
        requestStar(starKey, true, null);
    }

    /**
     * Requests the details of a star from the server, and calls the given callback when it's
     * received. The callback is called on the main thread.
     */
    public void requestStar(final String starKey, boolean force, final StarFetchedHandler callback) {
        Star s = mStars.get(starKey);
        if (s != null && !force) {
            callback.onStarFetched(s);
            return;
        }

        new AsyncTask<Void, Void, Star>() {
            @Override
            protected Star doInBackground(Void... arg0) {
                Star star = null;

                try {
                    String url = "stars/"+starKey;

                    warworlds.Warworlds.Star pb = ApiClient.getProtoBuf(url,
                            warworlds.Warworlds.Star.class);
                    star = Star.fromProtocolBuffer(pb);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return star;
            }

            @Override
            protected void onPostExecute(Star star) {
                if (star == null) {
                    return; // BAD!
                }

                mStars.put(starKey, star);
                if (callback != null) {
                    callback.onStarFetched(star);
                }
                fireStarUpdated(star);
            }
        }.execute();
    }

    public interface StarFetchedHandler {
        void onStarFetched(Star s);
    }
}
