package au.com.codeka.warworlds.model;

import java.util.HashMap;
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

    private static Map<String, Empire> mEmpireCache = new HashMap<String, Empire>();

    public static EmpireManager getInstance() {
        return sInstance;
    }

    private Empire mEmpire;

    /**
     * This is called when you first connect to the server. We need to pass in details about
     * the empire and stuff.
     */
    public void setup(Empire empire) {
        mEmpire = empire;
    }

    public Empire getEmpire() {
        return mEmpire;
    }

    public void fetchEmpire(final String empireKey, final EmpireFetchedHandler handler) {
        if (mEmpireCache.containsKey(empireKey)) {
            handler.onEmpireFetched(mEmpireCache.get(empireKey));
        }

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

                mEmpireCache.put(empireKey, empire);
                return empire;
            }

            @Override
            protected void onPostExecute(Empire empire) {
                if (empire == null) {
                    return; // BAD!
                }

                mEmpireCache.put(empireKey, empire);
                handler.onEmpireFetched(empire);
            }
        }.execute();
    }

    public interface EmpireFetchedHandler {
        public void onEmpireFetched(Empire empire);
    }
}
