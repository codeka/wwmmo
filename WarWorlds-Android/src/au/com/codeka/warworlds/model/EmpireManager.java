package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * Manages stuff about your empire (e.g. colonizing planets and whatnot).
 */
public class EmpireManager {
    private static Logger log = LoggerFactory.getLogger(EmpireManager.class);
    private static EmpireManager sInstance = new EmpireManager();

    public static EmpireManager getInstance() {
        return sInstance;
    }

    private Map<String, Empire> mEmpireCache = new HashMap<String, Empire>();
    private Map<String, List<EmpireFetchedHandler>> mInProgress = new HashMap<String, List<EmpireFetchedHandler>>();
    private MyEmpire mEmpire;

    /**
     * This is called when you first connect to the server. We need to pass in details about
     * the empire and stuff.
     */
    public void setup(MyEmpire empire) {
        mEmpire = empire;
    }

    /**
     * Gets a reference to the current empire.
     */
    public MyEmpire getEmpire() {
        return mEmpire;
    }

    public void fetchEmpire(final String empireKey, final EmpireFetchedHandler handler) {
        if (mEmpireCache.containsKey(empireKey)) {
            handler.onEmpireFetched(mEmpireCache.get(empireKey));
            return;
        }

        // if it's us, then that's good enough as well!
        if (mEmpire.getKey().equals(empireKey)) {
            handler.onEmpireFetched(mEmpire);
            return;
        }

        List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
        if (inProgress != null) {
            // if there's already a call in progress, don't fetch again
            inProgress.add(handler);
            return;
        }

        inProgress = new ArrayList<EmpireFetchedHandler>();
        mInProgress.put(empireKey, inProgress);

        new AsyncTask<Void, Void, Empire>() {
            @Override
            protected Empire doInBackground(Void... arg0) {
                Empire empire = null;

                try {
                    String url = "empires/"+empireKey;

                    warworlds.Warworlds.Empire pb = ApiClient.getProtoBuf(url,
                            warworlds.Warworlds.Empire.class);
                    empire = Empire.fromProtocolBuffer(pb);
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

                mEmpireCache.put(empireKey, empire);

                List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
                for (EmpireFetchedHandler handler : inProgress) {
                    handler.onEmpireFetched(empire);
                }
                mInProgress.remove(empireKey);
            }
        }.execute();
    }

    public interface EmpireFetchedHandler {
        public void onEmpireFetched(Empire empire);
    }
}
