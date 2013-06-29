package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.RealmContext;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.common.protobuf.Messages;

/**
 * Manages stuff about your empire (e.g. colonising planets and what-not).
 */
public class EmpireManager {
    private static Logger log = LoggerFactory.getLogger(EmpireManager.class);
    public static EmpireManager i = new EmpireManager();

    private Map<String, Empire> mEmpireCache = new HashMap<String, Empire>();
    private Map<String, List<EmpireFetchedHandler>> mInProgress = new HashMap<String, List<EmpireFetchedHandler>>();
    private Map<String, List<EmpireFetchedHandler>> mEmpireUpdatedListeners = new TreeMap<String, List<EmpireFetchedHandler>>();
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

    public Empire getEmpire(String empireKey) {
        if (empireKey == null) {
            return mNativeEmpire;
        }

        if (empireKey.equals(mEmpire.getKey())) {
            return mEmpire;
        }

        Empire empire = mEmpireCache.get(empireKey);
        if (empire != null) {
            return empire;
        }

        Messages.Empire pb = new LocalEmpireStore().getEmpire(empireKey);
        if (pb != null) {
            empire = new Empire();
            empire.fromProtocolBuffer(pb);
            mEmpireCache.put(empireKey, empire);
            return empire;
        }

        return mEmpireCache.get(empireKey);
    }

    public NativeEmpire getNativeEmpire() {
        return mNativeEmpire;
    }

    /**
     * Call this to register your interest in when a particular empire is updated. Any time that
     * empire is re-fetched from the server, your \c EmpireFetchedHandler will be called.
     */
    public void addEmpireUpdatedListener(String empireKey, EmpireFetchedHandler handler) {
        if (empireKey == null) {
            empireKey = "";
        }
        synchronized(mEmpireUpdatedListeners) {
            List<EmpireFetchedHandler> listeners = mEmpireUpdatedListeners.get(empireKey);
            if (listeners == null) {
                listeners = new ArrayList<EmpireFetchedHandler>();
                mEmpireUpdatedListeners.put(empireKey, listeners);
            }
            listeners.add(handler);
        }
    }

    /**
     * Removes the given \c EmpireFetchedHandler from receiving updates about refreshed empires.
     */
    public void removeEmpireUpdatedListener(EmpireFetchedHandler handler) {
        synchronized(mEmpireUpdatedListeners) {
            for (Object o : IteratorUtils.toList(mEmpireUpdatedListeners.keySet().iterator())) {
                String empireKey = (String) o;

                List<EmpireFetchedHandler> listeners = mEmpireUpdatedListeners.get(empireKey);
                listeners.remove(handler);

                if (listeners.isEmpty()) {
                    mEmpireUpdatedListeners.remove(empireKey);
                }
            }
        }
    }

    public void fireEmpireUpdated(Empire empire) {
        synchronized(mEmpireUpdatedListeners) {
            List<EmpireFetchedHandler> oldListeners = mEmpireUpdatedListeners.get(empire.getKey());
            if (oldListeners != null) {
                List<EmpireFetchedHandler> listeners = new ArrayList<EmpireFetchedHandler>(oldListeners);
                for (EmpireFetchedHandler handler : listeners) {
                    handler.onEmpireFetched(empire);
                }
            }

            oldListeners = mEmpireUpdatedListeners.get("");
            if (oldListeners != null) {
                List<EmpireFetchedHandler> listeners = new ArrayList<EmpireFetchedHandler>(oldListeners);
                for (EmpireFetchedHandler handler : listeners) {
                    handler.onEmpireFetched(empire);
                }
            }
        }
    }

    public void refreshEmpire() {
        if (mEmpire == null) {
            // todo?
            return;
        }
        refreshEmpire(mEmpire.getKey());
    }

    public void refreshEmpire(final String empireKey) {
        refreshEmpire(empireKey, null);
    }

    public void refreshEmpire(final String empireKey,
            final EmpireFetchedHandler handler) {
        if (empireKey == null || empireKey.length() == 0) {
            if (handler != null) {
                handler.onEmpireFetched(mNativeEmpire);
            }
            return;
        }

        ArrayList<String> empireKeys = new ArrayList<String>();
        empireKeys.add(empireKey);
        refreshEmpires(empireKeys, handler);
    }

    public void refreshEmpires(final Collection<String> empireKeys,
                               final EmpireFetchedHandler handler) {
        final ArrayList<String> toFetch = new ArrayList<String>();

        for (String empireKey : empireKeys) {
            List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
            if (inProgress != null) {
                // if there's already a call in progress, don't fetch again
                if (handler != null) {
                    inProgress.add(handler);
                }
                continue;
            } else {
                inProgress = new ArrayList<EmpireFetchedHandler>();
                if (handler != null) {
                    inProgress.add(handler);
                }
                mInProgress.put(empireKey, inProgress);
                toFetch.add(empireKey);
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
                        String empireKey = empire.getKey();

                        if (empireKey.equals(mEmpire.getKey())) {
                            mEmpire = (MyEmpire) empire;
                        } else {
                            mEmpireCache.put(empireKey, empire);
                        }

                        List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
                        if (inProgress != null) for (EmpireFetchedHandler handler : inProgress) {
                            if (handler != null) {
                                handler.onEmpireFetched(empire);
                            }
                        }
                        mInProgress.remove(empireKey);

                        fireEmpireUpdated(empire);
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
        if (mEmpire != null && mEmpire.getAlliance() != null && mEmpire.getAlliance().getKey().equals(alliance.getKey())) {
            mEmpire.updateAlliance(alliance);
            fireEmpireUpdated(mEmpire);
        }

        for (Empire empire : mEmpireCache.values()) {
            if (empire.getAlliance() != null && empire.getAlliance().getKey().equals(alliance.getKey())) {
                empire.updateAlliance(alliance);
                fireEmpireUpdated(empire);
            }
        }
    }

    /**
     * Synchronously fetch a list of empires. Note that we \i may return fewer empires than you
     * requested, if some of them are already in-progress.
     */
    public List<Empire> refreshEmpiresSync(final Collection<String> empireKeys) throws ApiException {
        ArrayList<Empire> empires = new ArrayList<Empire>();
        LocalEmpireStore store = new LocalEmpireStore();

        Iterator<String> iter = empireKeys.iterator();
        while (iter.hasNext()) {
            String url = null;
            int num = 0;
            while (iter.hasNext()) {
                String empireKey = iter.next();
                if (url == null) {
                    url = "empires/search?ids=";
                } else {
                    url += ",";
                }
                url += empireKey;
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
                }
            }
        }

        return empires;
    }

    public void fetchEmpire(final String empireKey,
                            final EmpireFetchedHandler handler) {
        if (empireKey == null) {
            if (handler != null) {
                handler.onEmpireFetched(mNativeEmpire);
            }
            return;
        }

        if (mEmpireCache.containsKey(empireKey)) {
            if (handler != null) {
                handler.onEmpireFetched(mEmpireCache.get(empireKey));
            }
            return;
        }

        // if it's us, then that's good enough as well!
        if (mEmpire != null && mEmpire.getKey().equals(empireKey)) {
            if (handler != null) {
                handler.onEmpireFetched(mEmpire);
            }
            return;
        }

        Messages.Empire pb = new LocalEmpireStore().getEmpire(empireKey);
        if (pb != null) {
            Empire empire = new Empire();
            empire.fromProtocolBuffer(pb);
            mEmpireCache.put(empireKey, empire);
            if (handler != null) {
                handler.onEmpireFetched(empire);
            }
            return;
        }

        refreshEmpire(empireKey, handler);
    }

    public void fetchEmpires(Collection<String> empireKeys, EmpireFetchedHandler handler) {
        ArrayList<String> toFetch = new ArrayList<String>();
        for (String empireKey : empireKeys) {
            if (empireKey == null) {
                if (handler != null) {
                    handler.onEmpireFetched(mNativeEmpire);
                }
                continue;
            }

            if (mEmpireCache.containsKey(empireKey)) {
                if (handler != null) {
                    handler.onEmpireFetched(mEmpireCache.get(empireKey));
                }
                continue;
            }

            // if it's us, then that's good enough as well!
            if (mEmpire != null && mEmpire.getKey().equals(empireKey)) {
                if (handler != null) {
                    handler.onEmpireFetched(mEmpire);
                }
                continue;
            }

            // if it's in the local store, that's fine as well
            Messages.Empire pb = new LocalEmpireStore().getEmpire(empireKey);
            if (pb != null) {
                Empire empire = new Empire();
                empire.fromProtocolBuffer(pb);
                mEmpireCache.put(empireKey, empire);
                if (handler != null) {
                    handler.onEmpireFetched(empire);
                }
                continue;
            }

            // otherwise, we'll have to fetch it
            toFetch.add(empireKey);
        }

        if (toFetch.size() > 0) {
            refreshEmpires(toFetch, handler);
        }
    }

    public List<Empire> fetchEmpiresSync(Collection<String> empireKeys) {
        ArrayList<Empire> empires = new ArrayList<Empire>();
        ArrayList<String> missingKeys = new ArrayList<String>();

        for (String empireKey : empireKeys) {
            Empire empire = getEmpire(empireKey);
            if (empire != null) {
                empires.add(empire);
            } else {
                missingKeys.add(empireKey);
            }
        }

        if (missingKeys.size() > 0) {
            try {
                List<Empire> fetchedEmpires = refreshEmpiresSync(missingKeys);
                for (Empire empire : fetchedEmpires) {
                    empires.add(empire);
                }
            } catch (ApiException e) {
                log.error("An error occured fetching empires.", e);
            }
        }

        return empires;
    }

    /**
     * Fetches empires in the given rank range. This will always include the top three
     * empires as well (since that's usually what you want in addition to the specific
     * range you asked for as well).
     */
    public void fetchEmpiresByRank(final int minRank, final int maxRank,
                                   final EmpiresFetchedHandler handler) {
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
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return empires;
            }

            @Override
            protected void onComplete(List<Empire> empires) {
                for (Empire empire : empires) {
                    if (!empire.getKey().equals(mEmpire.getKey())) {
                        mEmpireCache.put(empire.getKey(), empire);
                        fireEmpireUpdated(empire);
                    }
                }

                handler.onEmpiresFetched(empires);
            }
        }.execute();
    }

    public void searchEmpires(final Context context, final String nameSearch,
                              final EmpiresFetchedHandler handler) {
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
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return empires;
            }

            @Override
            protected void onComplete(List<Empire> empires) {
                for (Empire empire : empires) {
                    if (!empire.getKey().equals(mEmpire.getKey())) {
                        mEmpireCache.put(empire.getKey(), empire);
                        fireEmpireUpdated(empire);
                    }
                }

                handler.onEmpiresFetched(empires);
            }
        }.execute();
    }

    public interface EmpireFetchedHandler {
        public void onEmpireFetched(Empire empire);
    }

    public interface EmpiresFetchedHandler {
        public void onEmpiresFetched(List<Empire> empires);
    }

    private static class LocalEmpireStore extends SQLiteOpenHelper {
        private static Object sLock = new Object();

        public LocalEmpireStore() {
            super(App.i, "empires.db", null, 3);
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
                      +"  empire BLOB,"
                      +"  timestamp INTEGER);");
            db.execSQL("CREATE INDEX IX_empire_key_realm_id ON empires (empire_key, realm_id)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion == 2) {
                db.execSQL("ALTER TABLE empires "
                          +"ADD COLUMN timestamp INTEGER DEFAULT 0;");
            }
            if (newVersion == 3) {
                db.execSQL("ALTER TABLE empires "
                          +"ADD COLUMN realm_id INTEGER DEFAULT "+RealmManager.BETA_REALM_ID);
                db.execSQL("CREATE INDEX IX_empire_key_realm_id ON empires (empire_key, realm_id)");
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

                    ContentValues values = new ContentValues();
                    values.put("empire", empireBlob.toByteArray());
                    values.put("empire_key", empire.getKey());
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

        public Messages.Empire getEmpire(String empireKey) {
            synchronized(sLock) {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = null;
                try {
                    cursor = db.query("empires", new String[] {"empire", "timestamp"},
                            "empire_key = '"+empireKey.replace('\'', ' ')+"' AND realm_id="+RealmContext.i.getCurrentRealm().getID(),
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        cursor.close();
                        return null;
                    }

                    // if it's too old, we'll want to refresh it anyway from the server
                    long timestamp = cursor.getLong(1);
                    long oneDayAgo = DateTime.now(DateTimeZone.UTC).minusDays(1).getMillis();
                    if (timestamp == 0 || timestamp < oneDayAgo) {
                        return null;
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
    }
}
