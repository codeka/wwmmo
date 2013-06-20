package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

/**
 * This is a sub-class of \c Empire that represents \em my Empire. We have extra methods
 * and such that only the current user can perform.
 */
public class MyEmpire extends Empire {
    private static final Logger log = LoggerFactory.getLogger(MyEmpire.class);

    // make sure we don't collect taxes twice
    private boolean mCollectingTaxes = false;

    public MyEmpire() {
    }

    public void addCash(float amount) {
        mCash += amount;
    }

    /**
     * Colonizes the given planet. We'll call the given \c ColonizeCompleteHandler when the
     * operation completes (successfully or not).
     */
    public void colonize(final Context context, final Planet planet,
                         final ColonizeCompleteHandler callback) {
        log.debug(String.format("Colonizing: Star=%s Planet=%d",
                                planet.getStar().getKey(),
                                planet.getIndex()));
        new BackgroundRunner<Colony>() {
            @Override
            protected Colony doInBackground() {
                try {
                    if (planet.getStar() == null) {
                        log.warn("planet.getStarSummary() returned null!");
                        return null;
                    }

                    Messages.ColonizeRequest request = Messages.ColonizeRequest.newBuilder()
                            .setPlanetIndex(planet.getIndex())
                            .build();

                    String url = String.format("stars/%s/colonies", planet.getStar().getKey());
                    Messages.Colony pb = ApiClient.postProtoBuf(url, request, Messages.Colony.class);
                    if (pb == null)
                        return null;

                    Colony colony = new Colony();
                    colony.fromProtocolBuffer(pb);
                    return colony;
                } catch(Exception e) {
                    // TODO: handle exceptions
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
                StarManager.getInstance().refreshStar(context, colony.getStarKey());
            }
        }.execute();
    }

    public void collectTaxes(final Context context) {
        if (mCollectingTaxes) {
            return;
        }
        mCollectingTaxes = true;

        new BackgroundRunner<Boolean>() {
            private String mErrorMsg;
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = String.format("empires/%s/taxes?async=1", getKey());
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
                    new StyledDialog.Builder(context)
                                    .setTitle("Error")
                                    .setMessage(mErrorMsg)
                                    .create().show();
                }

                EmpireManager.i.refreshEmpire(context);
            }
        }.execute();
    }

    public void updateFleetStance(final Context context, final Star star, final Fleet fleet,
                                  final Fleet.Stance newStance) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = String.format("stars/%s/fleets/%s/orders",
                            fleet.getStarKey(),
                            fleet.getKey());
                    Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                                    .setOrder(Messages.FleetOrder.FLEET_ORDER.SET_STANCE)
                                    .setStance(Messages.Fleet.FLEET_STANCE.valueOf(newStance.getValue()))
                                    .build();
                    ApiClient.postProtoBuf(url, fleetOrder);
                    return true;
                } catch(Exception e) {
                    // TODO: handle exceptions
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
                EmpireManager.i.refreshEmpire(context, getKey());
                StarManager.getInstance().refreshStar(context, star.getKey());
            }
        }.execute();

    }

    public void fetchScoutReports(final Star star, final FetchScoutReportCompleteHandler handler) {
        new BackgroundRunner<List<ScoutReport>>() {
            @Override
            protected List<ScoutReport> doInBackground() {
                try {
                    String url = String.format("stars/%s/scout-reports", star.getKey());
                    Messages.ScoutReports pb = ApiClient.getProtoBuf(url, Messages.ScoutReports.class);
                    if (pb == null)
                        return null;

                    ArrayList<ScoutReport> reports = new ArrayList<ScoutReport>();
                    for (Messages.ScoutReport srpb : pb.getReportsList()) {
                        ScoutReport scoutReport = new ScoutReport();
                        scoutReport.fromProtocolBuffer(srpb);
                        reports.add(scoutReport);
                    }
                    return reports;
                } catch(Exception e) {
                    // TODO: handle exceptions
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

    public void fetchCombatReport(final String starKey, final String combatReportKey,
            final FetchCombatReportCompleteHandler handler ) {
        new BackgroundRunner<CombatReport>() {
            @Override
            protected CombatReport doInBackground() {
                try {
                    String url = String.format("stars/%s/combat-reports/%s",
                                               starKey, combatReportKey);
                    Messages.CombatReport pb = ApiClient.getProtoBuf(url,
                                                 Messages.CombatReport.class);
                    if (pb == null)
                        return null;

                    CombatReport combatReport = new CombatReport();
                    combatReport.fromProtocolBuffer(pb);
                    return combatReport;
                } catch(Exception e) {
                    // TODO: handle exceptions
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

    public void attackColony(final Context context, final Star star,
                             final Colony colony,
                             final AttackColonyCompleteHandler callback) {
        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                try {
                    String url = "stars/"+star.getKey()+"/colonies/"+colony.getKey()+"/attack";

                    Messages.Star pb = ApiClient.postProtoBuf(url, null, Messages.Star.class);
                    if (pb == null)
                        return null;

                    Star star = new Star();
                    star.fromProtocolBuffer(pb);
                    return star;
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onComplete(Star star) {
                if (star != null) {
                    StarManager.getInstance().refreshStar(context, star.getKey());
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
                    String url = "empires/"+getKey()+"/stars";

                    Messages.Stars pb = ApiClient.getProtoBuf(url, Messages.Stars.class);
                    if (pb == null)
                        return null;

                    ArrayList<Star> stars = new ArrayList<Star>();
                    for (Messages.Star star_pb : pb.getStarsList()) {
                        Star star = new Star();
                        star.fromProtocolBuffer(star_pb);
                        stars.add(star);
                    }

                    return stars;
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
                        StarManager.getInstance().fireStarUpdated(star);
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
