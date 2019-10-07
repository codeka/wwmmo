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
  private ThreadLocal<Realm> threadRealms;
  private Realm globalRealm;

  private RealmContext() {
    threadRealms = new ThreadLocal<>();
    globalRealm = null;
  }

  public Realm getCurrentRealm() {
    Realm realm = threadRealms.get();
    if (realm == null) {
      realm = globalRealm;
    }

    return realm;
  }

  public void setThreadRealm(Realm realm) {
    threadRealms.set(realm);
  }

  public void setGlobalRealm(Realm realm) {
    globalRealm = realm;
    globalRealm.update();
  }
}
