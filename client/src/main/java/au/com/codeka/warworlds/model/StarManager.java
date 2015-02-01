package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.util.LruCache;
import android.util.SparseArray;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

public class StarManager extends BaseManager {
    public static final StarManager i = new StarManager();

    public static final EventBus eventBus = new EventBus();

    private static final Log log = new Log("StarManager");
    private LruCache<Integer, StarOrSummary> stars = new LruCache<Integer, StarOrSummary>(100);
    private SparseArray<ApiRequest> inProgress = new SparseArray<>();

    public static float DEFAULT_MAX_CACHE_HOURS = 24.0f;

    public void clearCache() {
        stars.evictAll();
    }

    @Nullable
    public StarSummary getStarSummary(int starID) {
        return getStarSummary(starID, DEFAULT_MAX_CACHE_HOURS);
    }

    @Nullable
    public StarSummary getStarSummary(int starID, float maxCacheAgeHours) {
        StarOrSummary starOrSummary = stars.get(starID);
        if (starOrSummary != null) {
            return starOrSummary.getStarSummary();
        }

        refreshStarSummary(starID, maxCacheAgeHours);
        return null;
    }

    public SparseArray<StarSummary> getStarSummaries(Collection<Integer> starIDs,
            float maxCacheAgeHours) {
        SparseArray<StarSummary> result = new SparseArray<>();

        ArrayList<Integer> toFetch = null;
        for (Integer starID : starIDs) {
            StarOrSummary starOrSummary = stars.get(starID);
            if (starOrSummary != null) {
                StarSummary starSummary = starOrSummary.getStarSummary();
                result.put(starSummary.getID(), starSummary);
            } else {
                if (toFetch == null) {
                    toFetch = new ArrayList<>();
                }
                toFetch.add(starID);
            }
        }

        if (toFetch != null) {
            refreshStarSummaries(starIDs, maxCacheAgeHours);
        }
        return result;
    }

    @Nullable
    public Star getStar(int starID) {
        StarOrSummary starOrSummary = stars.get(starID);
        if (starOrSummary != null) {
            if (starOrSummary.star != null) {
                return starOrSummary.star;
            }
        }

        refreshStar(starID);
        return null;
    }

    /**
     * This can be called when a star is updated outside the context of StarManager. It will
     * notify the rest of the system that the star is updated.
     */
    public void notifyStarUpdated(Star star) {
        stars.put(star.getID(), new StarOrSummary(star));
        eventBus.publish(star);
    }

    /**
     * Refresh the star with the given from the server. An event will be posted to notify when the 
     * star is updated.
     */
    public void refreshStar(int starID) {
        refreshStar(starID, false);
    }

    /**
     * Refresh the star with the given from the server. An event will be posted to notify when the 
     * star is updated.
     *
     * @param onlyIfCached If true, we'll only refresh the star if we already have a cached version,
     *     otherwise this will do nothing.
     */
    public boolean refreshStar(final int starID, boolean onlyIfCached) {
        if (onlyIfCached && stars.get(starID) == null) {
            log.debug("Not updating star,  onlyIfCached = true and star is not cached.");
            return false;
        }

        synchronized(inProgress) {
            if (inProgress.get(starID) != null) {
                log.debug("Star is already being refreshed, not calling again.");
                return true;
            }
        }

        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                ArrayList<Integer> ids = new ArrayList<Integer>();
                ids.add(starID);
                for (Star star : requestStars(ids)) {
                    return star;
                }
                return null; // shouldn't happen!
            }

            @Override
            protected void onComplete(Star star) {
                if (star != null) {
                    stars.put(star.getID(), new StarOrSummary(star));
                    log.debug("Star %s refreshed, publishing event...", star.getName());
                    eventBus.publish(star);
                }
                inProgress.remove(starID);
            }
        }.execute();

        return true;
    }

    public void refreshStarSummary(final int starID) {
        refreshStarSummary(starID, DEFAULT_MAX_CACHE_HOURS);
    }

    @SuppressWarnings("serial")
    public void refreshStarSummary(final int starID, float maxCacheAgeHours) {
        refreshStarSummaries(new ArrayList<Integer>() {{
            add(starID);
        }}, maxCacheAgeHours);
    }

    public void refreshStarSummaries(final Collection<Integer> starIDs,
            final float maxCacheAgeHours) {
        new BackgroundRunner<List<StarSummary>>() {
            @Override
            protected List<StarSummary> doInBackground() {
                ArrayList<StarSummary> starSummaries = new ArrayList<StarSummary>();
                ArrayList<Integer> notCached = null;
                LocalStarsStore store = new LocalStarsStore();

                for (Integer starID : starIDs) {
                    Messages.Star star_pb = store.getStar(
                            Integer.toString(starID), maxCacheAgeHours);
                    if (star_pb != null) {
                        StarSummary starSummary = new StarSummary();
                        starSummary.fromProtocolBuffer(star_pb);
                        starSummaries.add(starSummary);
                    } else {
                        if (notCached == null) {
                            notCached = new ArrayList<Integer>();
                        }
                        notCached.add(starID);
                    }
                }

                if (notCached != null) {
                    final ArrayList<Integer> notInProgress = new ArrayList<Integer>();
                    synchronized(inProgress) {
                        for (Integer starID : starIDs) {
                            if (inProgress.add(starID)) {
                                notInProgress.add(starID);
                            }
                        }
                    }

                    for (Star star : requestStars(notInProgress)) {
                        starSummaries.add(star);
                    }
                }

                return starSummaries;
            }

            @Override
            protected void onComplete(List<StarSummary> result) {
                for (StarSummary starSummary : result) {
                    eventBus.publish(starSummary);
                    inProgress.remove(starSummary.getID());

                    if (starSummary instanceof Star) {
                        stars.put(starSummary.getID(), new StarOrSummary((Star) starSummary));
                    } else {
                        stars.put(starSummary.getID(), new StarOrSummary(starSummary));
                    }
                }
            }
        }.execute();
    }
/*
    private List<Star> requestStars(Collection<Integer> starIDs) {
        ArrayList<Star> stars = new ArrayList<Star>();
        for (Integer starID : starIDs) {
            Star star = requestStar(starID);
            if (star != null) {
                stars.add(star);
            }
        }
        return stars;
    }
*/
    public void renameStar(final Purchase purchase, final Star star, final String newName,
            final StarRenameCompleteHandler onCompleteHandler) {
        new BackgroundRunner<Star>() {
            private String errorMessage;

            @Override
            protected Star doInBackground() {
                String url = "stars/"+star.getKey();

                String price = "???";
                SkuDetails sku = null;
                if (purchase != null) {
                    try {
                        sku = PurchaseManager.i.getInventory().getSkuDetails(purchase.getSku());
                    } catch (IabException e1) {
                    }
                }
                if (sku != null) {
                    price = sku.getPrice();
                }

                Messages.StarRenameRequest.Builder pb = Messages.StarRenameRequest.newBuilder()
                        .setStarKey(star.getKey())
                        .setOldName(star.getName())
                        .setNewName(newName);
                if (purchase != null) {
                        pb.setPurchaseInfo(Messages.PurchaseInfo.newBuilder()
                          .setSku(purchase.getSku())
                          .setOrderId(purchase.getOrderId())
                          .setPrice(price)
                          .setToken(purchase.getToken())
                          .setDeveloperPayload(purchase.getDeveloperPayload()));
                }

                Messages.Star star_pb;
                try {
                    star_pb = ApiClient.putProtoBuf(url, pb.build(), Messages.Star.class);
                    Star star = new Star();
                    star.fromProtocolBuffer(star_pb);

                    new LocalStarsStore().addStar(star_pb);

                    return star;
                } catch (ApiException e) {
                    log.error("Error renaming star!", e);
                    errorMessage = e.getServerErrorMessage();
                    return null;
                }
            }

            @Override
            protected void onComplete(Star star) {
                if (star == null) {
                    if (onCompleteHandler != null) {
                        onCompleteHandler.onStarRename(null, false, errorMessage);
                    }
                } else {
                    notifyStarUpdated(star);
                    if (onCompleteHandler != null) {
                        onCompleteHandler.onStarRename(star, true, null);
                    }
                }
            }
        }.execute();
    }
/*
    //TODO: add support for fetching multiple stars in a single request
    private Star requestStar(final Integer starID) {
        Star star = null;

        try {
            String url = "stars/"+starID;

            Messages.Star pb = ApiClient.getProtoBuf(url, Messages.Star.class);
            star = new Star();
            star.fromProtocolBuffer(pb);

            new Simulation().simulate(star);
        } catch(Exception e) {
            log.error("Uh Oh!", e);
        }

        if (star != null) {
            Messages.Star.Builder starpb = Messages.Star.newBuilder();
            star.toProtocolBuffer(starpb);
            Messages.Star star_pb = starpb.build();

            new LocalStarsStore().addStar(star_pb);
        }

        return star;
    }
*/
    private static class LocalStarsStore extends SQLiteOpenHelper {
        private static Object sLock = new Object();

        public LocalStarsStore() {
            super(App.i, "stars.db", null, 1);
        }

        /**
         * This is called the first time we open the database, in order to create the required
         * tables, etc.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE stars ("
                      +"  id INTEGER PRIMARY KEY,"
                      +"  realm_id INTEGER,"
                      +"  star_key STRING,"
                      +"  star BLOB,"
                      +"  timestamp INTEGER);");
            db.execSQL("CREATE INDEX IX_star_key_realm_id ON stars (star_key, realm_id)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public void addStar(Messages.Star star) {
            synchronized(sLock) {
                SQLiteDatabase db = getWritableDatabase();
                try {
                    ByteArrayOutputStream starBlob = new ByteArrayOutputStream();
                    try {
                        star.writeTo(starBlob);
                    } catch (IOException e) {
                        // we won't get the notification, but not the end of the world...
                        log.error("Error saving star to cache.", e);
                        return;
                    }

                    // delete existing values first
                    db.delete("stars", getWhereClause(star.getKey()), null);

                    ContentValues values = new ContentValues();
                    values.put("star", starBlob.toByteArray());
                    values.put("star_key", star.getKey());
                    values.put("realm_id", RealmContext.i.getCurrentRealm().getID());
                    values.put("timestamp", DateTime.now(DateTimeZone.UTC).getMillis());
                    db.insert("stars", null, values);
                } catch(Exception e) {
                    log.error("Error saving star to cache.", e);
                } finally {
                    db.close();
                }
            }
        }

        public Messages.Star getStar(String starKey, float maxCacheAgeHours) {
            synchronized(sLock) {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = null;
                try {
                    cursor = db.query("stars", new String[] {"star", "timestamp"},
                            getWhereClause(starKey),
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        cursor.close();
                        return null;
                    }

                    // if it's too old, we'll want to refresh it anyway from the server
                    if (maxCacheAgeHours < Float.MAX_VALUE) {
                        long timestamp = cursor.getLong(1);
                        long oldest = DateTime.now(DateTimeZone.UTC).minusSeconds((int)(maxCacheAgeHours * 3600)).getMillis();
                        if (timestamp == 0 || timestamp < oldest) {
                            return null;
                        }
                    }

                    return Messages.Star.parseFrom(cursor.getBlob(0));
                } catch (Exception e) {
                    log.error("Error fetching star from cache.", e);
                    return null;
                } finally {
                    if (cursor != null) cursor.close();
                    db.close();
                }
            }
        }

        private String getWhereClause(String starKey) {
            return "star_key = '"+starKey.replace('\'', ' ')+"' AND realm_id="+RealmContext.i.getCurrentRealm().getID();
        }
    }
    public interface StarFetchedHandler {
        void onStarFetched(Star s);
    }
    public interface StarSummaryFetchedHandler {
        void onStarSummaryFetched(StarSummary s);
    }
    public interface StarSummariesFetchedHandler {
        void onStarSummariesFetched(Collection<StarSummary> stars);
    }
    public interface StarRenameCompleteHandler {
        void onStarRename(Star star, boolean successful, String errorMessage);
    }

    private static class StarOrSummary {
        public Star star;
        public StarSummary starSummary;

        public StarOrSummary(Star star) {
            this.star = star;
        }
        public StarOrSummary(StarSummary starSummary) {
            this.starSummary = starSummary;
        }

        public StarSummary getStarSummary() {
            if (starSummary != null) {
                return starSummary;
            }
            return star;
        }
    }
}
