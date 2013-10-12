package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.billing.IabException;
import au.com.codeka.warworlds.model.billing.Purchase;
import au.com.codeka.warworlds.model.billing.SkuDetails;

public class StarManager extends BaseManager {
    private static StarManager sInstance = new StarManager();
    public static StarManager getInstance() {
        return sInstance;
    }

    private static final Logger log = LoggerFactory.getLogger(StarManager.class);
    private TreeMap<String, Star> mStars;
    private TreeMap<String, StarSummary> mStarSummaries;
    private TreeMap<String, List<StarFetchedHandler>> mStarUpdatedListeners;
    private List<StarFetchedHandler> mAllStarUpdatedListeners;

    public static float DEFAULT_MAX_CACHE_HOURS = 24.0f;

    private StarManager() {
        mStars = new TreeMap<String, Star>();
        mStarSummaries = new TreeMap<String, StarSummary>();
        mStarUpdatedListeners = new TreeMap<String, List<StarFetchedHandler>>();
        mAllStarUpdatedListeners = new ArrayList<StarFetchedHandler>();
    }

    /**
     * Call this to register your interest in when a particular star is updated. Any time that
     * star is re-fetched from the server, your \c StarFetchedHandler will be called.
     */
    public void addStarUpdatedListener(String starKey, StarFetchedHandler handler) {
        if (starKey == null) {
            mAllStarUpdatedListeners.add(handler);
            return;
        }

        synchronized(mStarUpdatedListeners) {
            List<StarFetchedHandler> listeners = mStarUpdatedListeners.get(starKey);
            if (listeners == null) {
                listeners = new ArrayList<StarFetchedHandler>();
                mStarUpdatedListeners.put(starKey, listeners);
            }
            listeners.add(handler);
        }
    }

    /**
     * Removes the given \c StarFetchedHandler from receiving updates about refreshed stars.
     */
    public void removeStarUpdatedListener(StarFetchedHandler handler) {
        synchronized(mStarUpdatedListeners) {
            for (Object o : IteratorUtils.toList(mStarUpdatedListeners.keySet().iterator())) {
                String starKey = (String) o;

                List<StarFetchedHandler> listeners = mStarUpdatedListeners.get(starKey);
                listeners.remove(handler);

                if (listeners.isEmpty()) {
                    mStarUpdatedListeners.remove(starKey);
                }
            }
        }

        mAllStarUpdatedListeners.remove(handler);
    }

    public void fireStarUpdated(final Star star) {
        synchronized(mStarUpdatedListeners) {
            List<StarFetchedHandler> listeners = mStarUpdatedListeners.get(star.getKey());
            if (listeners != null) {
                for (final StarFetchedHandler handler : new ArrayList<StarFetchedHandler>(listeners)) {
                    fireHandler(handler, new Runnable() {
                        @Override
                        public void run() { handler.onStarFetched(star); }
                    });
                }
            }

            // also anybody who's interested in ALL stars
            for (final StarFetchedHandler handler : new ArrayList<StarFetchedHandler>(mAllStarUpdatedListeners)) {
                fireHandler(handler, new Runnable() {
                    @Override
                    public void run() { handler.onStarFetched(star); }
                });
            }
        }

        // also let a couple of the other Managers know
        SectorManager.getInstance().onStarUpdate(star);
        BuildManager.getInstance().onStarUpdate(star);
    }

    public void refreshStar(Star s) {
        refreshStar(s.getKey());
    }

    public void refreshStar(String starKey) {
        refreshStar(starKey, false);
    }

    public boolean refreshStar(String starKey, boolean onlyIfCached) {
        if (onlyIfCached && !mStars.containsKey(starKey)) {
            return false;
        }

        requestStar(starKey, true, new StarFetchedHandler() {
            @Override
            public void onStarFetched(Star s) {
                // When a star is explicitly refreshed, it's usually because it's changed somehow.
                // Generally that also means the sector has changed.
                SectorManager.getInstance().refreshSector(s.getSectorX(), s.getSectorY());
            }
        });

        return true;
    }

    public Star refreshStarSync(String starKey, boolean onlyIfCached) {
        if (onlyIfCached && !mStars.containsKey(starKey)) {
            return null;
        }

        Star star = requestStarSync(starKey, true);
        // When a star is explicitly refreshed, it's usually because it's changed somehow.
        // Generally that also means the sector has changed.
        try {
            SectorManager.getInstance().refreshSector(star.getSectorX(), star.getSectorY());
        } catch(Exception e) {
            // this can happen if we're called from a background thread, but we're not too worried.
        }
        return star;
    }

    public void requestStarSummary(final String starKey, final StarSummaryFetchedHandler callback) {
        StarSummary ss = mStarSummaries.get(starKey);
        if (ss != null) {
            callback.onStarSummaryFetched(ss);
            return;
        }

        ss = mStars.get(starKey);
        if (ss != null) {
            callback.onStarSummaryFetched(ss);
            return;
        }

        new BackgroundRunner<StarSummary>() {
            @Override
            protected StarSummary doInBackground() {
                return requestStarSummarySync(starKey, DEFAULT_MAX_CACHE_HOURS);
            }

            @Override
            protected void onComplete(StarSummary starSummary) {
                if (starSummary != null) {
                    callback.onStarSummaryFetched(starSummary);
                }
            }
        }.execute();
    }

    public void requestStarSummaries(final Collection<String> starKeys, final StarSummariesFetchedHandler callback) {
        final ArrayList<StarSummary> summaries = new ArrayList<StarSummary>();
        final ArrayList<String> toFetch = new ArrayList<String>();

        for (String starKey : starKeys) {
            StarSummary ss = mStarSummaries.get(starKey);
            if (ss != null) {
                summaries.add(ss);
                continue;
            }

            ss = mStars.get(starKey);
            if (ss != null) {
                summaries.add(ss);
                continue;
            }

            toFetch.add(starKey);
        }

        if (toFetch.size() == 0) {
            callback.onStarSummariesFetched(summaries);
        } else {
            new BackgroundRunner<List<StarSummary>>() {
                @Override
                protected List<StarSummary> doInBackground() {
                    return requestStarSummariesSync(toFetch, DEFAULT_MAX_CACHE_HOURS);
                }

                @Override
                protected void onComplete(List<StarSummary> stars) {
                    if (stars != null) {
                        for (StarSummary star : stars) {
                            summaries.add(star);
                        }
                    }
                    callback.onStarSummariesFetched(summaries);
                }
            }.execute();
        }
    }

    /**
     * Gets a StarSummary, but only if it's cached locally.
     */
    public StarSummary getStarSummaryNoFetch(String starKey, float maxCacheAgeHours) {
        StarSummary ss = mStarSummaries.get(starKey);
        if (ss != null) {
            return ss;
        }

        ss = mStars.get(starKey);
        if (ss != null) {
            return ss;
        }

        ss = loadStarSummary(starKey, maxCacheAgeHours);
        if (ss != null) {
            return ss;
        }

        return null;
    }

    /**
     * Like \c requestStarSummary but runs synchronously. Useful if you're
     * @param starKey
     * @return
     */
    public StarSummary requestStarSummarySync(String starKey, float maxCacheAgeHours) {
        StarSummary ss = getStarSummaryNoFetch(starKey, maxCacheAgeHours);
        if (ss != null) {
            return ss;
        }

        // no cache StarSummary, fetch the full star
        return doFetchStar(starKey);
    }

    public List<StarSummary> requestStarSummariesSync(Collection<String> starKeys, float maxCacheAgeHours) {
        ArrayList<StarSummary> starSummaries = new ArrayList<StarSummary>();
        for (String starKey : starKeys) {
            // TODO: this could be more efficient...
            StarSummary ss = requestStarSummarySync(starKey, DEFAULT_MAX_CACHE_HOURS);
            if (ss != null) {
                starSummaries.add(ss);
            }
        }
        return starSummaries;
    }

    /**
     * Requests the details of a star from the server, and calls the given callback when it's
     * received. The callback is called on the main thread.
     */
    public void requestStar(final String starKey, final boolean force,
                            final StarFetchedHandler callback) {
        Star s = mStars.get(starKey);
        if (s != null && !force) {
            callback.onStarFetched(s);
            return;
        }

        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                return requestStarSync(starKey, force);
            }

            @Override
            protected void onComplete(Star star) {
                if (star == null) {
                    return; // BAD!
                }

                // if we had the star summary cached, remove it (cause the star itself is newer)
                mStarSummaries.remove(starKey);
                mStars.put(starKey, star);

                if (callback != null) {
                    callback.onStarFetched(star);
                }
                fireStarUpdated(star);
            }
        }.execute();
    }

    public Star requestStarSync(final String starKey, boolean force) {
        Star s = mStars.get(starKey);
        if (s != null && !force) {
            return s;
        }

        Star star = doFetchStar(starKey);
        if (star != null) {
            log.debug(String.format("STAR[%s] has %d fleets.", star.getKey(),
                      star.getFleets() == null ? 0 : star.getFleets().size()));
        }

        if (star != null && !RealmContext.i.getCurrentRealm().isAlpha()) {
            // the alpha realm will have already simulated the star, but other realms
            // will need to simulate first.
            Simulation sim = new Simulation();
            sim.simulate(star);
        }
        return star;
    }

    public void renameStar(final Purchase purchase, final Star star, final String newName) {
        new BackgroundRunner<Star>() {
            @Override
            protected Star doInBackground() {
                String url = "stars/"+star.getKey();

                String price = "???";
                SkuDetails sku = null;
                try {
                    sku = PurchaseManager.i.getInventory().getSkuDetails(purchase.getSku());
                } catch (IabException e1) {
                }
                if (sku != null) {
                    price = sku.getPrice();
                }

                Messages.StarRenameRequest pb = Messages.StarRenameRequest.newBuilder()
                        .setStarKey(star.getKey())
                        .setOldName(star.getName())
                        .setNewName(newName)
                        .setPurchaseInfo(Messages.PurchaseInfo.newBuilder()
                                .setSku(purchase.getSku())
                                .setOrderId(purchase.getOrderId())
                                .setPrice(price)
                                .setToken(purchase.getToken())
                                .setDeveloperPayload(purchase.getDeveloperPayload()))
                        .build();

                Messages.Star star_pb;
                try {
                    star_pb = ApiClient.putProtoBuf(url, pb, Messages.Star.class);
                    Star star = new Star();
                    star.fromProtocolBuffer(star_pb);

                    updateStarSummary(star);
                    return star;
                } catch (ApiException e) {
                    log.error("Error renaming star!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(Star star) {
                if (star == null) {
                    return; //TODO: bad!
                }

                // if we had the star summary cached, remove it (cause the star itself is newer)
                mStarSummaries.remove(star.getKey());
                mStars.put(star.getKey(), star);

                fireStarUpdated(star);
            }
        }.execute();
    }

    private Star doFetchStar(final String starKey) {
        Star star = null;

        try {
            String url = "stars/"+starKey;

            Messages.Star pb = ApiClient.getProtoBuf(url, Messages.Star.class);
            star = new Star();
            star.fromProtocolBuffer(pb);
        } catch(Exception e) {
            // TODO: handle exceptions
            log.error(ExceptionUtils.getStackTrace(e));
        }

        if (star != null) {
            updateStarSummary(star);
        }

        return star;
    }

    /**
     * This is called when we fetch a new \c StarSummary, we'll want to cache it.
     */
    private static void updateStarSummary(StarSummary summary) {
        Messages.Star.Builder starpb = Messages.Star.newBuilder();
        summary.toProtocolBuffer(starpb);
        Messages.Star star_pb = starpb.build();

        new LocalStarsStore().addStar(star_pb);
    }

    /**
     * Attempts to load a \c StarSummary back from the cache directory.
     */
    private static StarSummary loadStarSummary(String starKey, float maxCacheAgeHours) {
        Messages.Star star_pb = new LocalStarsStore().getStar(starKey, maxCacheAgeHours); 
        if (star_pb == null) {
            return null;
        }

        StarSummary ss = new StarSummary();
        ss.fromProtocolBuffer(star_pb);

        return ss;
    }

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
                            "star_key = '"+starKey.replace('\'', ' ')+"' AND realm_id="+RealmContext.i.getCurrentRealm().getID(),
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        log.debug("Star "+starKey+" realm "+RealmContext.i.getCurrentRealm().getDisplayName()+" not found in cache.");
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
}
