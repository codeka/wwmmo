package au.com.codeka.warworlds.model;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.ColonizeRequest;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.CombatReport;
import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.FleetOrder;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.ScoutReport;
import au.com.codeka.common.model.ScoutReports;
import au.com.codeka.common.model.Star;
import au.com.codeka.common.model.Stars;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

/**
 * This is a sub-class of \c Empire that represents \em my Empire. We have extra methods
 * and such that only the current user can perform.
 */
public class MyEmpireManager {
    private static final Logger log = LoggerFactory.getLogger(MyEmpireManager.class);
    public static MyEmpireManager i = new MyEmpireManager();

    // make sure we don't collect taxes twice
    private boolean mCollectingTaxes = false;

    private MyEmpireManager() {
    }

    /**
     * Colonizes the given planet. We'll call the given \c ColonizeCompleteHandler when the
     * operation completes (successfully or not).
     */
    public void colonize(final Star star, final Planet planet, final ColonizeCompleteHandler callback) {
        log.debug(String.format("Colonizing: Star=%s Planet=%d",
                                star.key,
                                planet.index));
        new BackgroundRunner<Colony>() {
            @Override
            protected Colony doInBackground() {
                try {
                    ColonizeRequest request = new ColonizeRequest.Builder()
                            .planet_index(planet.index)
                            .build();

                    String url = String.format("stars/%s/colonies", star.key);
                    return ApiClient.postProtoBuf(url, request, Colony.class);
                } catch(Exception e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onComplete(Colony colony) {
                if (colony == null) {
                    return; // BAD!
                }

                if (callback != null) {
                    callback.onColonizeComplete(colony);
                }

                // make sure we record the fact that the star is updated as well
                StarManager.i.refreshStar(colony.star_key);
            }
        }.execute();
    }

    public void collectTaxes() {
        if (mCollectingTaxes) {
            return;
        }
        mCollectingTaxes = true;

        new BackgroundRunner<Boolean>() {
            private String mErrorMsg;
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = String.format("empires/%s/taxes?async=1", EmpireManager.i.getEmpire().key);
                    ApiClient.postProtoBuf(url, null);
                } catch(ApiException e) {
                    mErrorMsg = e.getServerErrorMessage();
                }

                return true;
            }

            @Override
            protected void onComplete(Boolean success) {
                mCollectingTaxes = false;
                if (mErrorMsg != null) {
                    return;
                }

                EmpireManager.i.refreshEmpire();
            }
        }.execute();
    }

    public void updateFleetStance(final Star star, final Fleet fleet,
                                  final Fleet.FLEET_STANCE newStance) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = String.format("stars/%s/fleets/%s/orders",
                            fleet.star_key, fleet.key);
                    FleetOrder fleetOrder = new FleetOrder.Builder()
                                    .order(FleetOrder.FLEET_ORDER.SET_STANCE)
                                    .stance(newStance)
                                    .build();
                    ApiClient.postProtoBuf(url, fleetOrder);
                    return true;
                } catch(Exception e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    return; // BAD!
                }

                // make sure we record the fact that the star is updated as well
                EmpireManager.i.refreshEmpire(EmpireManager.i.getEmpire().key);
                StarManager.i.refreshStar(star.key);
            }
        }.execute();

    }

    public void fetchScoutReports(final Star star,
                                  final FetchScoutReportCompleteHandler handler) {
        new BackgroundRunner<List<ScoutReport>>() {
            @Override
            protected List<ScoutReport> doInBackground() {
                try {
                    String url = String.format("stars/%s/scout-reports", star.key);
                    ScoutReports pb = ApiClient.getProtoBuf(url, ScoutReports.class);
                    if (pb == null)
                        return null;

                    return pb.reports;
                } catch(Exception e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onComplete(List<ScoutReport> reports) {
                handler.onComplete(reports);
            }
        }.execute();
    }

    public void fetchCombatReport(final String starKey,
            final String combatReportKey, final FetchCombatReportCompleteHandler handler ) {
        new BackgroundRunner<CombatReport>() {
            @Override
            protected CombatReport doInBackground() {
                try {
                    String url = String.format("stars/%s/combat-reports/%s",
                                               starKey, combatReportKey);
                    return ApiClient.getProtoBuf(url, CombatReport.class);
                } catch(Exception e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onComplete(CombatReport report) {
                if (report != null) {
                    handler.onComplete(report);
                }
            }
        }.execute();
    }

    public void attackColony(final Star star,
                             final Colony colony,
                             final AttackColonyCompleteHandler callback) {
        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                try {
                    String url = "stars/"+star.key+"/colonies/"+colony.key+"/attack";
                    return ApiClient.postProtoBuf(url, null, Star.class);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onComplete(Star star) {
                if (star != null) {
                    StarManager.i.fireStarUpdated(star);
                }

                if (callback != null) {
                    callback.onComplete();
                }
            }
        }.execute();
    }

    public void requestStars(final FetchStarsCompleteHandler callback) {
        new BackgroundRunner<List<Star>>() {
            @Override
            protected List<Star> doInBackground() {
                try {
                    String url = "empires/"+EmpireManager.i.getEmpire().key+"/stars";

                    Stars pb = ApiClient.getProtoBuf(url, Stars.class);
                    if (pb == null)
                        return null;

                    return pb.stars;
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onComplete(List<Star> stars) {
                if (stars != null) {
                    for (Star star : stars) {
                        StarManager.i.fireStarUpdated(star);
                    }
                }

                if (callback != null) {
                    callback.onComplete(stars);
                }
            }
        }.execute();
    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(Colony colony);
    }

    public static interface FetchScoutReportCompleteHandler {
        public void onComplete(List<ScoutReport> reports);
    }

    public static interface FetchCombatReportCompleteHandler {
        public void onComplete(CombatReport report);
    }

    public static interface FetchStarsCompleteHandler {
        public void onComplete(List<Star> stars);
    }

    public static interface AttackColonyCompleteHandler {
        public void onComplete();
    }
}
