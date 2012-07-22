package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

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

    private List<Fleet> mAllFleets;
    private List<Colony> mAllColonies;

    public List<Fleet> getAllFleets() {
        return mAllFleets;
    }

    public List<Colony> getAllColonies() {
        return mAllColonies;
    }

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

    /**
     * Refreshes all the details of this empire (e.g. collection of colonies, fleets etc)
     */
    public void refreshAllDetails(final RefreshAllCompleteHandler callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    String url = "empires/"+getKey()+"/details";

                    warworlds.Warworlds.Empire pb = ApiClient.getProtoBuf(url,
                            warworlds.Warworlds.Empire.class);
                    if (pb == null)
                        return false;

                    populateFromProtocolBuffer(pb);
                    return true;
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (callback != null) {
                    callback.onRefreshAllComplete(MyEmpire.this);
                }
            }
        }.execute();
    }

    public static MyEmpire fromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        MyEmpire empire = new MyEmpire();
        empire.populateFromProtocolBuffer(pb);
        return empire;
    }

    @Override
    protected void populateFromProtocolBuffer(warworlds.Warworlds.Empire pb) {
        super.populateFromProtocolBuffer(pb);

        List<warworlds.Warworlds.Colony> colony_pbs = pb.getColoniesList();
        if (colony_pbs != null && colony_pbs.size() > 0) {
            ArrayList<Colony> colonies = new ArrayList<Colony>();
            for (int i = 0; i < colony_pbs.size(); i++) {
                colonies.add(Colony.fromProtocolBuffer(colony_pbs.get(i)));
            }
            mAllColonies = colonies;
        }

        List<warworlds.Warworlds.Fleet> fleet_pbs = pb.getFleetsList();
        if (fleet_pbs != null && fleet_pbs.size() > 0) {
            ArrayList<Fleet> fleets = new ArrayList<Fleet>();
            for (int i = 0; i < fleet_pbs.size(); i++) {
                fleets.add(Fleet.fromProtocolBuffer(fleet_pbs.get(i)));
            }
            mAllFleets = fleets;
        }
    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(Colony colony);
    }

    public static interface RefreshAllCompleteHandler {
        public void onRefreshAllComplete(MyEmpire empire);
    }
}
