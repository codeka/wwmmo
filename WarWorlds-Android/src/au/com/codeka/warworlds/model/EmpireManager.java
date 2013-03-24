package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.protobuf.Messages;

/**
 * Manages stuff about your empire (e.g. colonising planets and what-not).
 */
public class EmpireManager {
    private static Logger log = LoggerFactory.getLogger(EmpireManager.class);
    private static EmpireManager sInstance = new EmpireManager();

    public static EmpireManager getInstance() {
        return sInstance;
    }

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

    public Empire getEmpire(Context context, String empireKey) {
        if (empireKey == null) {
            return mNativeEmpire;
        }

        if (empireKey.equals(mEmpire.getKey())) {
            return mEmpire;
        }

        Messages.Empire pb = new LocalEmpireStore(context).getEmpire(empireKey);
        if (pb != null) {
            Empire empire = Empire.fromProtocolBuffer(pb);
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
        mEmpire.refreshAllDetails(null);
    }

    public void refreshEmpire(final Context context, final String empireKey) {
        refreshEmpire(context, empireKey, null);
    }

    public void refreshEmpire(final Context context,
            final String empireKey,
            final EmpireFetchedHandler handler) {
        if (empireKey == null || empireKey.length() == 0) {
            if (handler != null) {
                handler.onEmpireFetched(mNativeEmpire);
            }
            return;
        }

        ArrayList<String> empireKeys = new ArrayList<String>();
        empireKeys.add(empireKey);
        refreshEmpires(context, empireKeys, handler);
    }

    public void refreshEmpires(final Context context,
                               final Collection<String> empireKeys,
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
            new AsyncTask<Void, Void, List<Empire>>() {
                @Override
                protected List<Empire> doInBackground(Void... arg0) {
                    try {
                        return refreshEmpiresSync(context, toFetch);
                    } catch (ApiException e) {
                        log.error("An error occured fetching empires.", e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(List<Empire> empires) {
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
    public List<Empire> refreshEmpiresSync(final Context context,
                                           final Collection<String> empireKeys) throws ApiException {
        String url = null;
        for (String empireKey : empireKeys) {
            if (url == null) {
                url = "empires/search?ids=";
            } else {
                url += ",";
            }
            url += empireKey;
        }

        Messages.Empires pb = ApiClient.getProtoBuf(url, Messages.Empires.class);
        ArrayList<Empire> empires = new ArrayList<Empire>();
        LocalEmpireStore store = new LocalEmpireStore(context);
        for (Messages.Empire empire_pb : pb.getEmpiresList()) {
            store.addEmpire(empire_pb);

            if (mEmpire != null && empire_pb.getKey().equals(mEmpire.getKey())) {
                empires.add(MyEmpire.fromProtocolBuffer(empire_pb));
            } else {
                empires.add(Empire.fromProtocolBuffer(empire_pb));
            }
        }

        return empires;
    }

    public void fetchEmpire(final Context context,
                            final String empireKey,
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

        Messages.Empire pb = new LocalEmpireStore(context).getEmpire(empireKey);
        if (pb != null) {
            Empire empire = Empire.fromProtocolBuffer(pb);
            mEmpireCache.put(empireKey, empire);
            if (handler != null) {
                handler.onEmpireFetched(empire);
            }
            return;
        }

        refreshEmpire(context, empireKey, handler);
    }

    public List<Empire> fetchEmpiresSync(Context context, Collection<String> empireKeys) {
        ArrayList<Empire> empires = new ArrayList<Empire>();
        ArrayList<String> missingKeys = new ArrayList<String>();

        for (String empireKey : empireKeys) {
            Empire empire = getEmpire(context, empireKey);
            if (empire != null) {
                empires.add(empire);
            } else {
                missingKeys.add(empireKey);
            }
        }

        if (missingKeys.size() > 0) {
            try {
                List<Empire> fetchedEmpires = refreshEmpiresSync(context, missingKeys);
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
    public void fetchEmpiresByRank(final Context context, final int minRank, final int maxRank,
                                   final EmpiresFetchedHandler handler) {
        new AsyncTask<Void, Void, List<Empire>>() {
            @Override
            protected List<Empire> doInBackground(Void... arg0) {
                List<Empire> empires = new ArrayList<Empire>();

                try {
                    String url = "empires/search?minRank="+minRank+"&maxRank="+maxRank;

                    Messages.Empires pb = ApiClient.getProtoBuf(url, Messages.Empires.class);

                    LocalEmpireStore les = new LocalEmpireStore(context);
                    for (Messages.Empire empire_pb : pb.getEmpiresList()) {
                        les.addEmpire(empire_pb);
                        empires.add(Empire.fromProtocolBuffer(empire_pb));
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return empires;
            }

            @Override
            protected void onPostExecute(List<Empire> empires) {
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
        new AsyncTask<Void, Void, List<Empire>>() {
            @Override
            protected List<Empire> doInBackground(Void... arg0) {
                List<Empire> empires = new ArrayList<Empire>();

                try {
                    String url = "empires/search?name="+nameSearch;

                    Messages.Empires pb = ApiClient.getProtoBuf(url, Messages.Empires.class);

                    LocalEmpireStore les = new LocalEmpireStore(context);
                    for (Messages.Empire empire_pb : pb.getEmpiresList()) {
                        les.addEmpire(empire_pb);
                        empires.add(Empire.fromProtocolBuffer(empire_pb));
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return empires;
            }

            @Override
            protected void onPostExecute(List<Empire> empires) {
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

        public LocalEmpireStore(Context context) {
            super(context, "empires.db", null, 2);
        }

        /**
         * This is called the first time we open the database, in order to create the required
         * tables, etc.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE empires ("
                      +"  id INTEGER PRIMARY KEY,"
                      +"  empire_key STRING,"
                      +"  empire BLOB);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion == 2) {
                db.execSQL("ALTER TABLE empires "
                          +"ADD COLUMN timestamp INTEGER DEFAULT 0;");
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
                    values.put("timestamp", DateTime.now(DateTimeZone.UTC).getMillis());
                    db.insert("empires", null, values);
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
                            "empire_key = '"+empireKey.replace('\'', ' ')+"'",
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
                } catch (InvalidProtocolBufferException e) {
                    return null;
                } finally {
                    if (cursor != null) cursor.close();
                    db.close();
                }
            }
        }
    }
}
