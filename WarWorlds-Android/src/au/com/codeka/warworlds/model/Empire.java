package au.com.codeka.warworlds.model;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.ColonizeRequest;
import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;

public class Empire {
    private static final Logger log = LoggerFactory.getLogger(Empire.class);
    private String mID;
    private String mDisplayName;

    public String getID() {
        return mID;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void colonize(final Planet planet, final ColonizeCompleteHandler callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    if (planet.getStar() == null) {
                        log.warn("planet.getStar() returned null!");
                        return false;
                    } else if (planet.getStar().getSector() == null) {
                        log.warn("planet.getStar().getSector() returned null!");
                        return false;
                    }

                    ColonizeRequest request = ColonizeRequest.newBuilder()
                            .setSectorX(planet.getStar().getSector().getX())
                            .setSectorY(planet.getStar().getSector().getY())
                            .setStarId(planet.getStar().getID())
                            .setPlanetIndex(planet.getIndex())
                            .build();

                    return ApiClient.putProtoBuf("colonies", request);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success == null) {
                    return; // BAD!
                }

                if (callback != null) {
                    callback.onColonizeComplete(success);
                }
            }
        }.execute();

    }

    public static Empire fromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        Empire empire = new Empire();
        empire.mID = pb.getId();
        empire.mDisplayName = pb.getDisplayName();
        return empire;
    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(boolean success);
    }
}
