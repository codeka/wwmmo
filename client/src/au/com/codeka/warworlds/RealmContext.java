package au.com.codeka.warworlds;

import au.com.codeka.warworlds.model.Realm;

/**
 * The \c RealmContext contains details about the realm we're currently connected to. We can
 * temporarily switch realms for the current thread only (useful when a notification is received)
 * or we can switch the whole process to a different realm (useful when you choose a different realm
 * from the \c RealmSelectActivity).
 */
public class RealmContext {
    public static RealmContext i = new RealmContext();
    private ThreadLocal<Realm> mThreadRealms;
    private Realm mGlobalRealm;

    private RealmContext() {
        mThreadRealms = new ThreadLocal<Realm>();
        mGlobalRealm = null;
    }

    public Realm getCurrentRealm() {
        Realm realm = mThreadRealms.get();
        if (realm == null) {
            realm = mGlobalRealm;
        }

        return realm;
    }

    public void setThreadRealm(Realm realm) {
        mThreadRealms.set(realm);
    }

    public void setGlobalRealm(Realm realm) {
        mGlobalRealm = realm;
    }
}
