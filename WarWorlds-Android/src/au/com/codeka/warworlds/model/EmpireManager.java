package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * Manages stuff about your empire (e.g. colonising planets and what-not).
 */
public class EmpireManager {
    private static Logger log = LoggerFactory.getLogger(EmpireManager.class);
    private static EmpireManager sInstance = new EmpireManager();

    public static EmpireManager getInstance() {
        return sInstance;
    }

    private Map<String, Empire> mEmpireCache = new HashMap<String, Empire>();
    private Map<String, List<EmpireFetchedHandler>> mInProgress = new HashMap<String, List<EmpireFetchedHandler>>();
    private Map<String, List<EmpireFetchedHandler>> mEmpireUpdatedListeners = new TreeMap<String, List<EmpireFetchedHandler>>();
    private MyEmpire mEmpire;
    private NativeEmpire mNativeEmpire;

    /**
     * This is called when you first connect to the server. We need to pass in details about
     * the empire and stuff.
     */
    public void setup(MyEmpire empire) {
        mEmpire = empire;
        mNativeEmpire = new NativeEmpire();
    }

    /**
     * Gets a reference to the current empire.
     */
    public MyEmpire getEmpire() {
        return mEmpire;
    }

    public NativeEmpire getNativeEmpire() {
        return mNativeEmpire;
    }

    /**
     * Call this to register your interest in when a particular empire is updated. Any time that
     * empire is re-fetched from the server, your \c EmpireFetchedHandler will be called.
     */
    public void addEmpireUpdatedListener(String empireKey, EmpireFetchedHandler handler) {
        synchronized(mEmpireUpdatedListeners) {
            List<EmpireFetchedHandler> listeners = mEmpireUpdatedListeners.get(empireKey);
            if (listeners == null) {
                listeners = new ArrayList<EmpireFetchedHandler>();
                mEmpireUpdatedListeners.put(empireKey, listeners);
            }
            listeners.add(handler);
        }
    }

    /**
     * Removes the given \c EmpireFetchedHandler from receiving updates about refreshed empires.
     */
    public void removeEmpireUpdatedListener(EmpireFetchedHandler handler) {
        synchronized(mEmpireUpdatedListeners) {
            for (Object o : IteratorUtils.toList(mEmpireUpdatedListeners.keySet().iterator())) {
                String empireKey = (String) o;

                List<EmpireFetchedHandler> listeners = mEmpireUpdatedListeners.get(empireKey);
                listeners.remove(handler);

                if (listeners.isEmpty()) {
                    mEmpireUpdatedListeners.remove(empireKey);
                }
            }
        }
    }

    protected void fireEmpireUpdated(Empire empire) {
        synchronized(mEmpireUpdatedListeners) {
            List<EmpireFetchedHandler> listeners = mEmpireUpdatedListeners.get(empire.getKey());
            if (listeners != null) {
                for (EmpireFetchedHandler handler : listeners) {
                    handler.onEmpireFetched(empire);
                }
            }
        }
    }

    public void refreshEmpire() {
        refreshEmpire(mEmpire.getKey(), null);
    }

    public void refreshEmpire(final String empireKey) {
        refreshEmpire(empireKey, null);
    }

    public void refreshEmpire(final String empireKey, final EmpireFetchedHandler handler) {
        if (empireKey == null || empireKey.length() == 0) {
            if (handler != null) {
                handler.onEmpireFetched(mNativeEmpire);
            }
            return;
        }

        List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
        if (inProgress != null && handler != null) {
            // if there's already a call in progress, don't fetch again
            inProgress.add(handler);
            return;
        } else {
            inProgress = new ArrayList<EmpireFetchedHandler>();
            inProgress.add(handler);
            mInProgress.put(empireKey, inProgress);
        }

        new AsyncTask<Void, Void, Empire>() {
            @Override
            protected Empire doInBackground(Void... arg0) {
                Empire empire = null;

                try {
                    String url = "empires/"+empireKey;

                    Messages.Empire pb = ApiClient.getProtoBuf(url, Messages.Empire.class);
                    if (empireKey.equals(mEmpire.getKey())) {
                        empire = MyEmpire.fromProtocolBuffer(pb);
                    } else {
                        empire = Empire.fromProtocolBuffer(pb);
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return empire;
            }

            @Override
            protected void onPostExecute(Empire empire) {
                if (empire == null) {
                    return; // BAD!
                }

                if (empireKey.equals(mEmpire.getKey())) {
                    mEmpire = (MyEmpire) empire;
                } else {
                    mEmpireCache.put(empireKey, empire);
                }

                List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
                if (inProgress != null) for (EmpireFetchedHandler handler : inProgress) {
                    if (handler != null) {
                        handler.onEmpireFetched(empire);
                    }
                }
                mInProgress.remove(empireKey);

                fireEmpireUpdated(empire);
            }
        }.execute();

    }

    public void fetchEmpire(final String empireKey, final EmpireFetchedHandler handler) {
        if (empireKey == null) {
            handler.onEmpireFetched(mNativeEmpire);
            return;
        }

        if (mEmpireCache.containsKey(empireKey)) {
            handler.onEmpireFetched(mEmpireCache.get(empireKey));
            return;
        }

        // if it's us, then that's good enough as well!
        if (mEmpire != null && mEmpire.getKey().equals(empireKey)) {
            handler.onEmpireFetched(mEmpire);
            return;
        }

        refreshEmpire(empireKey, handler);
    }

    public interface EmpireFetchedHandler {
        public void onEmpireFetched(Empire empire);
    }
}
