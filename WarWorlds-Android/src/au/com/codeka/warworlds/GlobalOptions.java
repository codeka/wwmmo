package au.com.codeka.warworlds;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

public class GlobalOptions {
    private SharedPreferences mPreferences;

    private static CopyOnWriteArrayList<OptionsChangedListener> mOptionsChangedListeners =
            new CopyOnWriteArrayList<OptionsChangedListener>();

    public static void addOptionsChangedListener(OptionsChangedListener listener) {
        if (!mOptionsChangedListeners.contains(listener)) {
            mOptionsChangedListeners.add(listener);
        }
    }

    public static void removeOptionsChangedListener(OptionsChangedListener listener) {
        mOptionsChangedListeners.remove(listener);
    }

    protected void fireOptionsChanged(GlobalOptions newOptions) {
        for(OptionsChangedListener listener : mOptionsChangedListeners) {
            listener.onOptionsChanged(newOptions);
        }
    }

    public GlobalOptions(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.startsWith("GlobalOptions.")) {
                    fireOptionsChanged(GlobalOptions.this);
                }
            }
        });
    }

    public StarfieldDetail getStarfieldDetail() {
        String val = mPreferences.getString("GlobalOptions.StarfieldDetail", StarfieldDetail.STARS_AND_GAS.toString());
        for (StarfieldDetail d : StarfieldDetail.values()) {
            if (d.toString().equals(val)) {
                return d;
            }
        }

        return StarfieldDetail.STARS_AND_GAS;
    }

    public boolean uniqueStarsAndPlanets() {
        return mPreferences.getBoolean("GlobalOptions.GenUniqueStarsAndPlanets", false);
    }

    public NotificationOptions getNotificationOptions(NotificationKind kind) {
        String baseName = "GlobalOptions.Notifications["+kind+"].";
        NotificationOptions opt = new NotificationOptions();
        opt.mKind = kind;
        opt.mNotificationEnabled = mPreferences.getBoolean(baseName+"Enabled", true);
        opt.mLedColour = mPreferences.getInt(baseName+"Colour", Color.RED);
        return opt;
    }

    public Map<NotificationKind, NotificationOptions> getNotificationOptions() {
        TreeMap<NotificationKind, NotificationOptions> options = new TreeMap<NotificationKind, NotificationOptions>();

        for (NotificationKind kind : NotificationKind.values()) {
            options.put(kind, getNotificationOptions(kind));
        }

        return options;
    }

    /**
     * Pass this to \c addOnOptionsChangedListener to be notified when options change.
     */
    public interface OptionsChangedListener {
        void onOptionsChanged(GlobalOptions newOptions);
    }

    public enum StarfieldDetail {
        BLACK (0),
        STARS (1),
        STARS_AND_GAS (2);

        private int mValue;
        StarfieldDetail(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static StarfieldDetail fromValue(int value) {
            for(StarfieldDetail d : StarfieldDetail.values()) {
                if (d.getValue() == value) {
                    return d;
                }
            }

            return StarfieldDetail.STARS_AND_GAS;
        }
    }

    public enum NotificationKind {
        OTHER,
        BUILDING_BUILD_COMPLETE,
        FLEET_BUILD_COMPLETE,
        FLEET_MOVE_COMPLETE,
        FLEET_UNDER_ATTACK,
        FLEET_DESTROYED,
        FLEET_VICTORIOUS
    }

    public static class NotificationOptions {
        private NotificationKind mKind;
        private boolean mNotificationEnabled;
        private int mLedColour;

        public NotificationKind getKind() {
            return mKind;
        }
        public boolean isEnabled() {
            return mNotificationEnabled;
        }
        public void setEnabled(boolean enabled) {
            mNotificationEnabled = true;
        }
        public int getLedColour() {
            return mLedColour;
        }
        public void setLedColour(int argb) {
            mLedColour = argb;
        }
    }
}
