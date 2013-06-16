package au.com.codeka.warworlds.model;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.api.ApiClient;

public class RealmManager {
    public static RealmManager i = new RealmManager();

    private List<Realm> mRealms;
    private Realm mCurrentRealm;
    private ArrayList<RealmChangedHandler> mRealmChangedHandlers;

    private RealmManager() {
        mRealmChangedHandlers = new ArrayList<RealmChangedHandler>();
    }

    // The IDs for the realms can NEVER change, once set
    public static int DEBUG_REALM_ID = 1000;
    public static int ALPHA_REALM_ID = 1;
    public static int BETA_REALM_ID = 2;
    public static int BLITZ_REALM_ID = 10;

    public void setup(Context context) {
        mRealms = new ArrayList<Realm>();
        try {
            if (Util.isDebug()) {
                mRealms.add(new Realm(DEBUG_REALM_ID, "http://192.168.1.4:8080/realms/beta/",
                                      "Debug",
                                      "The debug realm runs on my local dev box for testing.",
                                      Realm.AuthenticationMethod.Default, false));
            }
            mRealms.add(new Realm(ALPHA_REALM_ID, "https://warworldsmmo.appspot.com/api/v1/",
                                  "Alpha",
                                  "The Alpha realm is officially deprecated and new players should join Beta.",
                                  Realm.AuthenticationMethod.AppEngine, true));
            mRealms.add(new Realm(BETA_REALM_ID, "https://game.war-worlds.com/realms/beta/",
                                  "Beta",
                                  "If you're new to War Worlds, you should join this realm. eXplore, eXpand, eXploit, eXterminate!",
                                  Realm.AuthenticationMethod.Default, false));
            if (Util.isDebug()) {
                mRealms.add(new Realm(BLITZ_REALM_ID, "https://game.war-worlds.com/realms/blitz/",
                                      "Blitz",
                                      "The goal of Blitz is to build as big an empire as you can in 1 month. Each month, the universe is reset and a winner announced.",
                                      Realm.AuthenticationMethod.Default, false));
            }
        } catch(URISyntaxException e) {
            // should never happen
        }

        SharedPreferences prefs = Util.getSharedPreferences(context);
        if (prefs.getString("RealmName", null) != null) {
            selectRealm(context, prefs.getString("RealmName", null), false);
        }
    }

    public void addRealmChangedHandler(RealmChangedHandler handler) {
        synchronized(mRealmChangedHandlers) {
            mRealmChangedHandlers.add(handler);
        }
    }
    public void removeRealmChangedHandler(RealmChangedHandler handler) {
        synchronized(mRealmChangedHandlers) {
            mRealmChangedHandlers.remove(handler);
        }
    }
    protected void fireRealmChangedHandler(Realm newRealm) {
        synchronized(mRealmChangedHandlers) {
            for (RealmChangedHandler handler : mRealmChangedHandlers) {
                handler.onRealmChanged(newRealm);
            }
        }
    }

    public Realm getRealm() {
        return mCurrentRealm;
    }

    public List<Realm> getRealms() {
        return mRealms;
    }

    public void selectRealm(Context context, String realmName) {
        selectRealm(context, realmName, true);
    }

    private void selectRealm(Context context, String realmName, boolean saveSelection) {
        if (realmName == null) {
            mCurrentRealm = null;
        } else {
            for (Realm realm : mRealms) {
                if (realm.getDisplayName().equals(realmName)) {
                    mCurrentRealm = realm;

                    // make sure the ApiClient knows which base URL to use
                    ApiClient.configure(context, mCurrentRealm.getBaseUrl());
                }
            }
        }

        if (saveSelection) {
            Util.getSharedPreferences(context).edit()
                .putString("RealmName",  mCurrentRealm == null ? null : mCurrentRealm.getDisplayName())
                .commit();
        }

        fireRealmChangedHandler(mCurrentRealm);
    }

    public interface RealmChangedHandler {
        public void onRealmChanged(Realm newRealm);
    }
}
