package au.com.codeka.warworlds.model;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.ColonizeRequest;
import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * This is a sub-class of \c Empire that represents \em my Empire. We have extra methods
 * and such that only the current user can perform.
 */
public class MyEmpire extends Empire {
    private static final Logger log = LoggerFactory.getLogger(MyEmpire.class);

    /**
     * Colonizes the given planet. We'll call the given \c ColonizeCompleteHandler when the
     * operation completes (successfully or not).
     */
    public void colonize(final Planet planet, final ColonizeCompleteHandler callback) {
        new AsyncTask<Void, Void, Colony>() {
            @Override
            protected Colony doInBackground(Void... arg0) {
                try {
                    if (planet.getStar() == null) {
                        log.warn("planet.getStar() returned null!");
                        return null;
                    } else if (planet.getStar().getSector() == null) {
                        log.warn("planet.getStar().getSector() returned null!");
                        return null;
                    }

                    ColonizeRequest request = ColonizeRequest.newBuilder()
                            .setPlanetKey(planet.getKey())
                            .setStarKey(planet.getStar().getKey())
                            .build();

                    warworlds.Warworlds.Colony pb = ApiClient.postProtoBuf("colonies", request,
                            warworlds.Warworlds.Colony.class);
                    if (pb == null)
                        return null;
                    return Colony.fromProtocolBuffer(pb);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Colony colony) {
                if (colony == null) {
                    return; // BAD!
                }

                if (callback != null) {
                    callback.onColonizeComplete(colony);
                }
            }
        }.execute();
    }

    public static MyEmpire fromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        MyEmpire empire = new MyEmpire();
        populateFromProtocolBuffer(pb, empire);
        return empire;
    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(Colony colony);
    }
}
