package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.util.LruCache;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventBus;

/**
 * Manages stuff about your empire (e.g. colonising planets and what-not).
 */
public class EmpireManager {
    private static final Log log = new Log("EmpireManager");
    public static EmpireManager i = new EmpireManager();

    public static EventBus eventBus = new EventBus();

    private LruCache<Integer, Empire> mEmpireCache = new LruCache<Integer, Empire>(64);
    private HashSet<Integer> mInProgress = new HashSet<Integer>();
    private MyEmpire mEmpire;
    private NativeEmpire mNativeEmpire = new NativeEmpire();

    /**
     * This is called when you first connect to the server. We need to pass in details about
     * the empire and stuff.
     */
    public void setup(MyEmpire empire) {
        mEmpire = empire;
    }

    /**
     * Gets a reference to the current empire.
     */
    public MyEmpire getEmpire() {
        return mEmpire;
    }
    public void clearEmpire() {
        mEmpire = null;
    }

    public Empire getEmpire(Integer empireID) {
        return getEmpire(empireID, false);
    }

    public Empire getEmpire(Integer empireID, boolean allowOld) {
        if (empireID == null) {
            return mNativeEmpire;
        }

        if (empireID == mEmpire.getID()) {
            return mEmpire;
        }

        Empire empire = mEmpireCache.get(empireID);
        if (empire != null) {
            return empire;
        }

        // Refresh the empire from the server, but just return null for now.
        refreshEmpire(empireID);
        return null;
    }

    /**
     * Gets all of the empires with the given IDs. If we don't have them all cached, you may not
     * get all of the ones you ask for.
     */
    public List<Empire> getEmpires(Collection<Integer> empireIDs) {
        return getEmpires(empireIDs, false);
    }

    /**
     * Gets all of the empires with the given IDs. If we don't have them all cached, you may not
     * get all of the ones you ask for.
     */
    public List<Empire> getEmpires(Collection<Integer> empireIDs, boolean allowOld) {
        ArrayList<Empire> empires = new ArrayList<Empire>();
        ArrayList<Integer> missing = new ArrayList<Integer>();

        for(Integer empireID : empireIDs) {
            if (empireID == mEmpire.getID()) {
                empires.add(mEmpire);
                continue;
            }

            Empire empire = mEmpireCache.get(empireID);
            if (empire != null) {
                empires.add(empire);
                continue;
            }

            missing.add(empireID);
        }

        if (missing.size() > 0) {
            refreshEmpires(missing);
        }

        return empires;
    }

    public List<Empire> getMatchingEmpiresFromCache(String filter) {
        Messages.Empires empires_pb = new LocalEmpireStore().getMatchingEmpires(filter);
        if (empires_pb == null) {
            return null;
        }

        ArrayList<Empire> empires = new ArrayList<Empire>();
        for (Messages.Empire empire_pb : empires_pb.getEmpiresList()) {
            Empire empire = new Empire();
            empire.fromProtocolBuffer(empire_pb);
            empires.add(empire);
        }

        return empires;
    }

    public NativeEmpire getNativeEmpire() {
        return mNativeEmpire;
    }

    public void refreshEmpire() {
        if (mEmpire == null) {
            return;
        }

        refreshEmpire(mEmpire.getID());
    }

    public void refreshEmpire(final Integer empireID) {
        if (empireID == null) {
            // Nothing to do for native empires
            return;
        }

        ArrayList<Integer> empireIDs = new ArrayList<Integer>();
        empireIDs.add(empireID);
        refreshEmpires(empireIDs);
    }

    public void refreshEmpires(final Collection<Integer> empireIDs) {
        final ArrayList<Integer> toFetch = new ArrayList<Integer>();

        for (Integer empireID : empireIDs) {
            if (!mInProgress.contains(empireID)) {
                toFetch.add(empireID);
                mInProgress.add(empireID);
            }
        }

        if (toFetch.size() > 0) {
            new BackgroundRunner<List<Empire>>() {
                @Override
                protected List<Empire> doInBackground() {
                    try {
                        return refreshEmpiresSync(toFetch);
                    } catch (ApiException e) {
                        log.error("An error occured fetching empires.", e);
                        return null;
                    }
                }

                @Override
                protected void onComplete(List<Empire> empires) {
                    if (empires == null) {
                        return; // BAD!
                    }

                    for (Empire empire : empires) {
                        Integer empireID = empire.getID();

                        if (mEmpire != null && empireID == mEmpire.getID()) {
                            mEmpire = (MyEmpire) empire;
                        } else {
                            mEmpireCache.put(empireID, empire);
                        }

                        mInProgress.remove(empire.getID());

                        eventBus.publish(empire);
                    }
                }
            }.execute();
        }
    }

    /**
     * This is called by the AllianceManager when an alliance changes, we'll want to refresh
     * any empires we have cached with the new data.
     */
    public void onAllianceUpdated(Alliance alliance) {
        if (mEmpire != null && mEmpire.getAlliance() != null
                && mEmpire.getAlliance().getKey().equals(alliance.getKey())) {
            mEmpire.updateAlliance(alliance);
            eventBus.publish(mEmpire);
        }

        for (Map.Entry<Integer, Empire> entry : mEmpireCache.snapshot().entrySet()) {
            Empire empire = entry.getValue();
            if (empire != null && empire.getAlliance() != null
                    && empire.getAlliance().getKey().equals(alliance.getKey())) {
                empire.updateAlliance(alliance);
                eventBus.publish(empire);
            }
        }
    }

    /**
     * Synchronously fetch a list of empires. Note that we \i may return fewer empires than you
     * requested, if some of them are already in-progress.
     */
    public List<Empire> refreshEmpiresSync(final Collection<Integer> empireKeys) throws ApiException {
        ArrayList<Empire> empires = new ArrayList<Empire>();
        LocalEmpireStore store = new LocalEmpireStore();

        Iterator<Integer> iter = empireKeys.iterator();
        while (iter.hasNext()) {
            String url = null;
            int num = 0;
            while (iter.hasNext()) {
                Integer empireID = iter.next();

                // If it's in the empire store, then we don't need to fetch from the server, yay!
                Messages.Empire pb = new LocalEmpireStore().getEmpire(empireID, false);
                if (pb != null) {
                    Empire empire = new Empire();
                    empire.fromProtocolBuffer(pb);
                    mEmpireCache.put(empireID, empire);
                    empires.add(empire);
                    continue;
                }

                if (url == null) {
                    url = "empires/search?ids=";
                } else {
                    url += ",";
                }
                url += empireID;
                num ++;
                if (num >= 25 || url.length() > 1000) {
                    break;
                }
            }
            if (url == null) {
                break;
            }

            Messages.Empires pb = ApiClient.getProtoBuf(url, Messages.Empires.class);
            for (Messages.Empire empire_pb : pb.getEmpiresList()) {
                store.addEmpire(empire_pb);

                if (mEmpire != null && empire_pb.getKey().equals(mEmpire.getKey())) {
                    MyEmpire empire = new MyEmpire();
                    empire.fromProtocolBuffer(empire_pb);
                    empires.add(empire);
                } else {
                    Empire empire = new Empire();
                    empire.fromProtocolBuffer(empire_pb);
                    empires.add(empire);
                    mEmpireCache.put(empire.getID(), empire);
                }
            }
        }

        return empires;
    }

    // we cache the call below, because it's annoying when you switch between
    // tabs to have to keep reloading the list for no real reason.
    private int mLastFetchMinRank = 0;
    private int mLastFetchMaxRank = 0;
    private List<Empire> mLastRankFetchEmpires;

    /**
     * Searches empires in the given rank range. This will always include the top three
     * empires as well (since that's usually what you want in addition to the specific
     * range you asked for as well).
     */
    public void searchEmpiresByRank(final int minRank, final int maxRank,
                                    final SearchCompleteHandler handler) {
        if (mLastFetchMinRank == minRank && mLastFetchMaxRank == maxRank) {
            handler.onSearchComplete(mLastRankFetchEmpires);
            return;
        }

        new BackgroundRunner<List<Empire>>() {
            @Override
            protected List<Empire> doInBackground() {
                List<Empire> empires = new ArrayList<Empire>();

                try {
                    String url = "empires/search?minRank="+minRank+"&maxRank="+maxRank;

                    Messages.Empires pb = ApiClient.getProtoBuf(url, Messages.Empires.class);

                    LocalEmpireStore les = new LocalEmpireStore();
                    for (Messages.Empire empire_pb : pb.getEmpiresList()) {
                        les.addEmpire(empire_pb);
                        Empire empire = new Empire();
                        empire.fromProtocolBuffer(empire_pb);
                        empires.add(empire);
                    }
                } catch(Exception e) {
                    log.error("Error fetching empires.", e);
                }

                return empires;
            }

            @Override
            protected void onComplete(List<Empire> empires) {
                for (Empire empire : empires) {
                    if (!empire.getKey().equals(mEmpire.getKey())) {
                        mEmpireCache.put(empire.getID(), empire);
                        eventBus.publish(empire);
                    }
                }

                mLastFetchMinRank = minRank;
                mLastFetchMaxRank = maxRank;
                mLastRankFetchEmpires = empires;
                handler.onSearchComplete(empires);
            }
        }.execute();
    }

    public void searchEmpires(final Context context, final String nameSearch,
                              final SearchCompleteHandler handler) {
        new BackgroundRunner<List<Empire>>() {
            @Override
            protected List<Empire> doInBackground() {
                List<Empire> empires = new ArrayList<Empire>();

                try {
                    String url = "empires/search?name="+nameSearch;

                    Messages.Empires pb = ApiClient.getProtoBuf(url, Messages.Empires.class);

                    LocalEmpireStore les = new LocalEmpireStore();
                    for (Messages.Empire empire_pb : pb.getEmpiresList()) {
                        les.addEmpire(empire_pb);
                        Empire empire = new Empire();
                        empire.fromProtocolBuffer(empire_pb);
                        empires.add(empire);
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error("Error fetching empires.", e);
                }

                return empires;
            }

            @Override
            protected void onComplete(List<Empire> empires) {
                for (Empire empire : empires) {
                    if (!empire.getKey().equals(mEmpire.getKey())) {
                        mEmpireCache.put(empire.getID(), empire);
                        eventBus.publish(empire);
                    }
                }

                handler.onSearchComplete(empires);
            }
        }.execute();
    }

    private static class LocalEmpireStore extends SQLiteOpenHelper {
        private static Object sLock = new Object();

        public LocalEmpireStore() {
            super(App.i, "empires.db", null, 4);
        }

        /**
         * This is called the first time we open the database, in order to create the required
         * tables, etc.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE empires ("
                      +"  id INTEGER PRIMARY KEY,"
                      +"  realm_id INTEGER,"
                      +"  empire_key STRING,"
                      +"  empire_name STRING,"
                      +"  empire BLOB,"
                      +"  timestamp INTEGER);");
            db.execSQL("CREATE INDEX IX_empire_key_realm_id ON empires (empire_key, realm_id)");
            db.execSQL("CREATE INDEX IX_empire_name_realm_id ON empires (empire_name, realm_id)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE empires "
                          +"ADD COLUMN timestamp INTEGER DEFAULT 0;");
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE empires "
                          +"ADD COLUMN realm_id INTEGER DEFAULT "+RealmManager.BETA_REALM_ID);
                db.execSQL("CREATE INDEX IX_empire_key_realm_id ON empires (empire_key, realm_id)");
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE empires "
                          +"ADD COLUMN empire_name STRING");
                db.execSQL("CREATE INDEX IX_empire_name_realm_id ON empires (empire_name, realm_id)");
                // Note: we don't bother populating the empire_name column, since it's only used
                // for filtering and it'll be populated anyway the next time the empire is fetched.
            }
        }

        public void addEmpire(Messages.Empire empire) {
            synchronized(sLock) {
                SQLiteDatabase db = getWritableDatabase();
                try {
                    ByteArrayOutputStream empireBlob = new ByteArrayOutputStream();
                    try {
                        empire.writeTo(empireBlob);
                    } catch (IOException e) {
                        // we won't get the notification, but not the end of the world...
                        return;
                    }

                    // delete any old cached values first
                    db.delete("empires", getWhereClause(Integer.parseInt(empire.getKey())), null);

                    // insert a new cached value
                    ContentValues values = new ContentValues();
                    values.put("empire", empireBlob.toByteArray());
                    values.put("empire_key", empire.getKey());
                    values.put("empire_name", empire.getDisplayName());
                    values.put("realm_id", RealmContext.i.getCurrentRealm().getID());
                    values.put("timestamp", DateTime.now(DateTimeZone.UTC).getMillis());
                    db.insert("empires", null, values);
                } catch(Exception e) {
                    // ignore errors... todo: log them
                } finally {
                    db.close();
                }
            }
        }

        public Messages.Empires getMatchingEmpires(String filter) {
            synchronized(sLock) {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = null;
                try {
                    cursor = db.query("empires", new String[] {"empire", "empire_name", "timestamp"},
                            getFilterClause(filter),
                            null, null, null, "empire_name");

                    Messages.Empires.Builder empires_pb = Messages.Empires.newBuilder();
                    if (cursor.moveToFirst()) {
                        while (true) {
                            // NOTE: we ignore the timestamp for this operation.
                            Messages.Empire empire_pb = Messages.Empire.parseFrom(cursor.getBlob(0));
                            empires_pb.addEmpires(empire_pb);
    
                            if (!cursor.moveToNext()) {
                                break;
                            }
                        }
                    }

                    return empires_pb.build();
                } catch (Exception e) {
                    log.error("Error occured fetching matching empires.", e);
                    return null;
                } finally {
                    if (cursor != null) cursor.close();
                    db.close();
                }

            }
        }

        public Messages.Empire getEmpire(Integer empireID, boolean allowOld) {
            synchronized(sLock) {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = null;
                try {
                    cursor = db.query("empires", new String[] {"empire", "timestamp"},
                            getWhereClause(empireID),
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        cursor.close();
                        return null;
                    }

                    // if it's too old, we'll want to refresh it anyway from the server
                    if (!allowOld) {
                        long timestamp = cursor.getLong(1);
                        long oneDayAgo = DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis();
                        if (timestamp == 0 || timestamp < oneDayAgo) {
                            return null;
                        }
                    }

                    return Messages.Empire.parseFrom(cursor.getBlob(0));
                } catch (Exception e) {
                    // todo: log errors
                    return null;
                } finally {
                    if (cursor != null) cursor.close();
                    db.close();
                }
            }
        }

        private String getWhereClause(Integer empireID) {
            return "empire_key = '"+empireID+"' AND realm_id="+RealmContext.i.getCurrentRealm().getID();
        }

        private String getFilterClause(String empireName) {
            return "empire_name LIKE '%"+empireName.replace('\'', ' ').replace('%', ' ')+"%' AND realm_id="+RealmContext.i.getCurrentRealm().getID();
        }
    }

    public interface SearchCompleteHandler {
        public void onSearchComplete(List<Empire> empires);
    }
}
