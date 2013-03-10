package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    private List<Fleet> mAllFleets;
    private List<Colony> mAllColonies;
    private List<BuildRequest> mAllBuildRequests;
    private Map<String, Star> mStars;
    private List<RefreshAllCompleteHandler> mRefreshAllCompleteHandlers;
    private boolean mIsDirty;

    // make sure we don't collect taxes twice
    private boolean mCollectingTaxes = false;

    public MyEmpire() {
        mRefreshAllCompleteHandlers = new ArrayList<RefreshAllCompleteHandler>();
        mIsDirty = true;
    }

    /**
     * If you don't call setDirty() first, then refreshAll() just resimulates and doesn't actually
     * refresh from the server.
     */
    public void setDirty() {
        mIsDirty = true;
    }

    public void addRefreshAllCompleteHandler(RefreshAllCompleteHandler handler) {
        synchronized(mRefreshAllCompleteHandlers) {
            mRefreshAllCompleteHandlers.add(handler);
        }
    }
    public void removeRefreshAllCompleteHandler(RefreshAllCompleteHandler handler) {
        synchronized(mRefreshAllCompleteHandlers) {
            mRefreshAllCompleteHandlers.remove(handler);
        }
    }
    private void fireRefreshAllCompleteHandler() {
        synchronized(mRefreshAllCompleteHandlers) {
            for (RefreshAllCompleteHandler handler : mRefreshAllCompleteHandlers) {
                handler.onRefreshAllComplete(this);
            }
        }
    }

    public List<Fleet> getAllFleets() {
        return mAllFleets;
    }

    public List<Colony> getAllColonies() {
        return mAllColonies;
    }

    public List<BuildRequest> getAllBuildRequests() {
        return mAllBuildRequests;
    }

    /**
     * Gets a \c List<Star> of the "important" stars (that is, the stars when one of our colonies,
     * fleets, etc are).
     */
    public Map<String, Star> getImportantStars() {
        return mStars;
    }

    public Star getImportantStar(String key) {
        return mStars.get(key);
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
                handler.onComplete(report);
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

    /**
     * Refreshes all the details of this empire (e.g. collection of colonies, fleets etc)
     */
    public void refreshAllDetails(final RefreshAllCompleteHandler callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    if (!mIsDirty) {
                        simulate();
                    } else {
                        String url = "empires/"+getKey()+"/details?do_simulate=0";

                        Messages.Empire pb = ApiClient.getProtoBuf(url, Messages.Empire.class);
                        if (pb == null)
                            return false;

                        populateFromProtocolBuffer(pb);
                        mIsDirty = false;
                    }
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
                    return;
                }

                if (callback != null) {
                    callback.onRefreshAllComplete(MyEmpire.this);
                }
                fireRefreshAllCompleteHandler();

                // also, anybody waiting for updates from empires in general
                EmpireManager.getInstance().fireEmpireUpdated(MyEmpire.this);
            }
        }.execute();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);

        if (mAllFleets == null) {
            mAllFleets = new ArrayList<Fleet>();
        }
        Fleet[] fleets = new Fleet[mAllFleets.size()];
        parcel.writeParcelableArray(mAllFleets.toArray(fleets), flags);

        if (mAllColonies == null) {
            mAllColonies = new ArrayList<Colony>();
        }
        Colony[] colonies = new Colony[mAllColonies.size()];
        parcel.writeParcelableArray(mAllColonies.toArray(colonies), flags);

        if (mAllBuildRequests == null) {
            mAllBuildRequests = new ArrayList<BuildRequest>();
        }
        BuildRequest[] buildRequests = new BuildRequest[mAllBuildRequests.size()];
        parcel.writeParcelableArray(mAllBuildRequests.toArray(buildRequests), flags);

        Star[] stars = new Star[mStars.size()];
        parcel.writeParcelableArray(mStars.values().toArray(stars), flags);
    }

    @Override
    protected void readFromParcel(Parcel parcel) {
        super.readFromParcel(parcel);

        Parcelable[] fleets = parcel.readParcelableArray(Fleet.class.getClassLoader());
        mAllFleets = new ArrayList<Fleet>();
        for (int i = 0; i < fleets.length; i++) {
            mAllFleets.add((Fleet) fleets[i]);
        }

        Parcelable[] colonies = parcel.readParcelableArray(Colony.class.getClassLoader());
        mAllColonies = new ArrayList<Colony>();
        for (int i = 0; i < colonies.length; i++) {
            mAllColonies.add((Colony) colonies[i]);
        }

        Parcelable[] buildRequests = parcel.readParcelableArray(BuildRequest.class.getClassLoader());
        mAllBuildRequests = new ArrayList<BuildRequest>();
        for (int i = 0; i < buildRequests.length; i++) {
            mAllBuildRequests.add((BuildRequest) buildRequests[i]);
        }

        Parcelable[] stars = parcel.readParcelableArray(Star.class.getClassLoader());
        mStars = new TreeMap<String, Star>();
        for (Parcelable p : stars) {
            Star star = (Star) p;
            mStars.put(star.getKey(), star);
        }
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

    @Override
    protected void populateFromProtocolBuffer(Messages.Empire pb) {
        super.populateFromProtocolBuffer(pb);

        mAllColonies = new ArrayList<Colony>();
        mAllFleets = new ArrayList<Fleet>();
        mAllBuildRequests = new ArrayList<BuildRequest>();

        List<Messages.Star> star_pbs = pb.getStarsList();
        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        if (star_pbs != null && star_pbs.size() > 0) {
            for (int i = 0; i < star_pbs.size(); i++) {
                stars.put(star_pbs.get(i).getKey(), Star.fromProtocolBuffer(star_pbs.get(i)));
            }
        }
        mStars = stars;

        simulate();
    }

    private void simulate() {
        Simulation sim = new Simulation();
        for (Star star : mStars.values()) {
            sim.simulate(star);

            for (Colony c : star.getColonies()) {
                if (c.getEmpireKey() != null && c.getEmpireKey().equals(getKey())) {
                    mAllColonies.add(c);

                    for (BuildRequest br : star.getBuildRequests()) {
                        if (br.getColonyKey().equals(c.getKey())) {
                            mAllBuildRequests.add(br);
                        }
                    }
                }
            }
            for (Fleet f : star.getFleets()) {
                if (f.getEmpireKey() != null && f.getEmpireKey().equals(getKey())) {
                    mAllFleets.add(f);
                }
            }
        }
    }

    public static interface ColonizeCompleteHandler {
        public void onColonizeComplete(Colony colony);
    }

    public static interface RefreshAllCompleteHandler {
        public void onRefreshAllComplete(MyEmpire empire);
    }

    public static interface FetchScoutReportCompleteHandler {
        public void onComplete(List<ScoutReport> reports);
    }

    public static interface FetchCombatReportCompleteHandler {
        public void onComplete(CombatReport report);
    }

    public static interface AttackColonyCompleteHandler {
        public void onComplete();
    }
}
