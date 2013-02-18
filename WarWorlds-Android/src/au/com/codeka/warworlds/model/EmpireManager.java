package au.com.codeka.warworlds.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    private NativeEmpire mNativeEmpire;

    /**
     * This is called when you first connect to the server. We need to pass in details about
     * the empire and stuff.
     */
    public void setup(MyEmpire empire) {
        mEmpire = empire;
        mNativeEmpire = new NativeEmpire();
    }

    /**
     * Gets a reference to the current empire.
     */
    public MyEmpire getEmpire() {
        return mEmpire;
    }

    public Empire getEmpire(Context context, String empireKey) {
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
            if (oldListeners == null) {
                return;
            }
            List<EmpireFetchedHandler> listeners = new ArrayList<EmpireFetchedHandler>(
                    mEmpireUpdatedListeners.get(empire.getKey()));
            for (EmpireFetchedHandler handler : listeners) {
                handler.onEmpireFetched(empire);
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

        List<EmpireFetchedHandler> inProgress = mInProgress.get(empireKey);
        if (inProgress != null) {
            // if there's already a call in progress, don't fetch again
            if (handler != null) {
                inProgress.add(handler);
            }
            return;
        } else {
            inProgress = new ArrayList<EmpireFetchedHandler>();
            if (handler != null) {
                inProgress.add(handler);
            }
            mInProgress.put(empireKey, inProgress);
        }

        new AsyncTask<Void, Void, Empire>() {
            @Override
            protected Empire doInBackground(Void... arg0) {
                Empire empire = null;

                try {
                    String url = "empires/"+empireKey;

                    Messages.Empire pb = ApiClient.getProtoBuf(url, Messages.Empire.class);
                    new LocalEmpireStore(context).addEmpire(pb);

                    if (empireKey.equals(mEmpire.getKey())) {
                        empire = MyEmpire.fromProtocolBuffer(pb);
                    } else {
                        empire = Empire.fromProtocolBuffer(pb);
                    }
                } catch(Exception e) {
                    // TODO: handle exceptions
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                return empire;
            }

            @Override
            protected void onPostExecute(Empire empire) {
                if (empire == null) {
                    return; // BAD!
                }

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
        }.execute();

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

    public interface EmpireFetchedHandler {
        public void onEmpireFetched(Empire empire);
    }

    private static class LocalEmpireStore extends SQLiteOpenHelper {
        private static Object sLock = new Object();

        public LocalEmpireStore(Context context) {
            super(context, "empires.db", null, 1);
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
                    cursor = db.query("empires", new String[] {"empire"},
                            "empire_key = '"+empireKey.replace('\'', ' ')+"'",
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        cursor.close();
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
