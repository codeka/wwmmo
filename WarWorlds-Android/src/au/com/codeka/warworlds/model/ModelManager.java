package au.com.codeka.warworlds.model;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;

public class ModelManager {
    private static final Logger log = LoggerFactory.getLogger(ModelManager.class);

    /**
     * Requests the details of a star from the server, and calls the given callback when it's
     * received. The callback is called on the main thread.
     * @param sectorX
     * @param sectorY
     * @param starID
     * @param callback
     */
    public static void requestStar(final long sectorX, final long sectorY, final int starID,
            final StarFetchedHandler callback) {
        new AsyncTask<Void, Void, Star>() {
            @Override
            protected Star doInBackground(Void... arg0) {
                Star star = null;

                try {
                    String url = "sectors/"+sectorX+","+sectorY+"/stars/"+starID;

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

                if (callback != null) {
                    callback.onStarFetched(star);
                }
            }
        }.execute();

    }
    
    public interface StarFetchedHandler {
        void onStarFetched(Star s);
    }
}
