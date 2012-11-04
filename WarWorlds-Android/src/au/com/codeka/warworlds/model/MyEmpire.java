package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Map<String, Star> mStars;

    public List<Fleet> getAllFleets() {
        return mAllFleets;
    }

    public List<Colony> getAllColonies() {
        return mAllColonies;
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
    public void colonize(final Planet planet, final ColonizeCompleteHandler callback) {
        log.debug(String.format("Colonizing: Star=%s Planet=%d",
                                planet.getStar().getKey(),
                                planet.getIndex()));
        new AsyncTask<Void, Void, Colony>() {
            @Override
            protected Colony doInBackground(Void... arg0) {
                try {
                    if (planet.getStar() == null) {
                        log.warn("planet.getStar() returned null!");
                        return null;
                    }

                    Messages.ColonizeRequest request = Messages.ColonizeRequest.newBuilder()
                            .setPlanetIndex(planet.getIndex())
                            .build();

                    String url = String.format("stars/%s/colonies", planet.getStar().getKey());
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
                StarManager.getInstance().refreshStar(colony.getStarKey());
            }
        }.execute();
    }

    public void collectTaxes(final Star star, final Colony colony) {
        new AsyncTask<Void, Void, Colony>() {
            @Override
            protected Colony doInBackground(Void... arg0) {
                try {
                    String url = String.format("stars/%s/colonies/%s/taxes",
                                               star.getKey(), colony.getKey());
                    Messages.Colony pb = ApiClient.postProtoBuf(url, null, Messages.Colony.class);
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

                // make sure we record the fact that the star is updated as well
                EmpireManager.getInstance().refreshEmpire(getKey());
                StarManager.getInstance().refreshStar(colony.getStarKey());
            }
        }.execute();
    }

    public void updateFleetStance(final Star star, final Fleet fleet,
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
                EmpireManager.getInstance().refreshEmpire(getKey());
                StarManager.getInstance().refreshStar(star.getKey());
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

    /**
     * Refreshes all the details of this empire (e.g. collection of colonies, fleets etc)
     */
    public void refreshAllDetails(final RefreshAllCompleteHandler callback) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    String url = "empires/"+getKey()+"/details";

                    Messages.Empire pb = ApiClient.getProtoBuf(url, Messages.Empire.class);
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

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);

        Fleet[] fleets = new Fleet[mAllFleets.size()];
        parcel.writeParcelableArray(mAllFleets.toArray(fleets), flags);

        Colony[] colonies = new Colony[mAllColonies.size()];
        parcel.writeParcelableArray(mAllColonies.toArray(colonies), flags);

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

        List<Messages.Colony> colony_pbs = pb.getColoniesList();
        ArrayList<Colony> colonies = new ArrayList<Colony>();
        if (colony_pbs != null && colony_pbs.size() > 0) {
            for (int i = 0; i < colony_pbs.size(); i++) {
                colonies.add(Colony.fromProtocolBuffer(colony_pbs.get(i)));
            }
        }
        mAllColonies = colonies;

        List<Messages.Fleet> fleet_pbs = pb.getFleetsList();
        ArrayList<Fleet> fleets = new ArrayList<Fleet>();
        if (fleet_pbs != null && fleet_pbs.size() > 0) {
            for (int i = 0; i < fleet_pbs.size(); i++) {
                fleets.add(Fleet.fromProtocolBuffer(fleet_pbs.get(i)));
            }
        }
        mAllFleets = fleets;

        List<Messages.Star> star_pbs = pb.getStarsList();
        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        if (star_pbs != null && star_pbs.size() > 0) {
            for (int i = 0; i < star_pbs.size(); i++) {
                stars.put(star_pbs.get(i).getKey(), Star.fromProtocolBuffer(star_pbs.get(i)));
            }
        }
        mStars = stars;
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
}
