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

    private RealmManager() {
    }

    public void setup(Context context) {
        mRealms = new ArrayList<Realm>();
        try {
            if (Util.isDebug()) {
                mRealms.add(new Realm("http://192.168.1.4:8271/api/v1/", "Debug Alpha", Realm.AuthenticationMethod.LocalAppEngine));
                mRealms.add(new Realm("http://192.168.1.4:8080/realm/beta/", "Debug Beta", Realm.AuthenticationMethod.Default));
            }
            mRealms.add(new Realm("https://warworldsmmo.appspot.com/api/v1/", "Alpha", Realm.AuthenticationMethod.AppEngine));
        } catch(URISyntaxException e) {
            // should never happen
        }

        SharedPreferences prefs = Util.getSharedPreferences(context);
        if (prefs.getString("RealmName", null) != null) {
            selectRealm(context, prefs.getString("RealmName", null), false);
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
                    ApiClient.configure(mCurrentRealm.getBaseUrl());
                }
            }
        }

        if (saveSelection) {
            Util.getSharedPreferences(context).edit()
                .putString("RealmName",  mCurrentRealm == null ? null : mCurrentRealm.getDisplayName())
                .commit();
        }
    }
}
