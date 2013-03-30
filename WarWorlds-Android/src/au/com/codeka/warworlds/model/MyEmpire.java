package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.model.protobuf.Messages;

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
                                planet.getStarSummary().getKey(),
                                planet.getIndex()));
        new AsyncTask<Void, Void, Colony>() {
            @Override
            protected Colony doInBackground(Void... arg0) {
                try {
                    if (planet.getStarSummary() == null) {
                        log.warn("planet.getStarSummary() returned null!");
                        return null;
                    }

                    Messages.ColonizeRequest request = Messages.ColonizeRequest.newBuilder()
                            .setPlanetIndex(planet.getIndex())
                            .build();

                    String url = String.format("stars/%s/colonies", planet.getStarSummary().getKey());
                    Messages.Colony pb = ApiClient.postProtoBuf(url, request, Messages.Colony.class);
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

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    String url = String.format("empires/%s/taxes?async=1", getKey());
                    ApiClient.postProtoBuf(url, null);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return false;
                }
/*TODO
                // update our copy of everything and reset the uncollected taxes to zero, this'll
                // keep us accurate until the server notifies us that's finished as well.
                Simulation sim = new Simulation();
                float taxes = 0.0f;
                for (Star star : mStars.values()) {
                    sim.simulate(star);
                    for (Colony colony : star.getColonies()) {
                        if (colony.getEmpireKey() != null && colony.getEmpireKey().equals(getKey())) {
                            taxes += colony.getUncollectedTaxes();
                            colony.setUncollectedTaxes(0.0f);
                        }
                    }
                }
                mCash += taxes;
                */

                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                mCollectingTaxes = false;
                EmpireManager.getInstance().fireEmpireUpdated(MyEmpire.this);
            }
        }.execute();
    }

    public void updateFleetStance(final Context context, final Star star, final Fleet fleet,
                                  final Fleet.Stance newStance) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
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
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    return; // BAD!
                }

                // make sure we record the fact that the star is updated as well
                EmpireManager.getInstance().refreshEmpire(context, getKey());
                StarManager.getInstance().refreshStar(context, star.getKey());
            }
        }.execute();

    }

    public void fetchScoutReports(final Star star, final FetchScoutReportCompleteHandler handler) {
        new AsyncTask<Void, Void, List<ScoutReport>>() {
            @Override
            protected List<ScoutReport> doInBackground(Void... arg0) {
                try {
                    String url = String.format("stars/%s/scout-reports", star.getKey());
                    Messages.ScoutReports pb = ApiClient.getProtoBuf(url, Messages.ScoutReports.class);
                    if (pb == null)
                        return null;

                    ArrayList<ScoutReport> reports = new ArrayList<ScoutReport>();
                    for (Messages.ScoutReport srpb : pb.getReportsList()) {
                        reports.add(ScoutReport.fromProtocolBuffer(srpb));
                    }
                    return reports;
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<ScoutReport> reports) {
                handler.onComplete(reports);
            }
        }.execute();
    }

    public void fetchCombatReport(final String starKey, final String combatReportKey,
            final FetchCombatReportCompleteHandler handler ) {
        new AsyncTask<Void, Void, CombatReport>() {
            @Override
            protected CombatReport doInBackground(Void... arg0) {
                try {
                    String url = String.format("stars/%s/combat-reports/%s",
                                               starKey, combatReportKey);
                    Messages.CombatReport pb = ApiClient.getProtoBuf(url,
                                                 Messages.CombatReport.class);
                    if (pb == null)
                        return null;

                    return CombatReport.fromProtocolBuffer(pb);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onPostExecute(CombatReport report) {
                if (report != null) {
                    handler.onComplete(report);
                }
            }
        }.execute();
    }

    public void attackColony(final Context context, final Star star,
                             final Colony colony,
                             final AttackColonyCompleteHandler callback) {
        new AsyncTask<Void, Void, Star>() {
            @Override
            protected Star doInBackground(Void... arg0) {
                try {
                    String url = "stars/"+star.getKey()+"/colonies/"+colony.getKey()+"/attack";

                    Messages.Star pb = ApiClient.postProtoBuf(url, null, Messages.Star.class);
                    if (pb == null)
                        return null;

                    return Star.fromProtocolBuffer(pb);
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Star star) {
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
        new AsyncTask<Void, Void, List<Star>>() {
            @Override
            protected List<Star> doInBackground(Void... arg0) {
                try {
                    String url = "empires/"+getKey()+"/stars";

                    Messages.Stars pb = ApiClient.getProtoBuf(url, Messages.Stars.class);
                    if (pb == null)
                        return null;

                    Simulation sim = new Simulation();
                    ArrayList<Star> stars = new ArrayList<Star>();
                    for (Messages.Star star_pb : pb.getStarsList()) {
                        Star star = Star.fromProtocolBuffer(star_pb);
                        sim.simulate(star);
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
            protected void onPostExecute(List<Star> stars) {
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

    public static final Parcelable.Creator<MyEmpire> CREATOR
                = new Parcelable.Creator<MyEmpire>() {
        @Override
        public MyEmpire createFromParcel(Parcel parcel) {
            MyEmpire e = new MyEmpire();
            e.readFromParcel(parcel);
            return e;
        }

        @Override
        public MyEmpire[] newArray(int size) {
            return new MyEmpire[size];
        }
    };

    public static MyEmpire fromProtocolBuffer(Messages.Empire pb) {
        MyEmpire empire = new MyEmpire();
        empire.populateFromProtocolBuffer(pb);
        return empire;
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
