package au.com.codeka.warworlds.model;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.ColonizeRequest;
import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;

public class Empire {
    private static final Logger log = LoggerFactory.getLogger(Empire.class);
    private String mKey;
    private String mDisplayName;

    public String getKey() {
        return mKey;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

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

    public static Empire fromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        Empire empire = new Empire();
        empire.mKey = pb.getKey();
        empire.mDisplayName = pb.getDisplayName();
        return empire;
    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(Colony colony);
    }
}
