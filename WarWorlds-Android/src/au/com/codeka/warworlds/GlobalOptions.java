package au.com.codeka.warworlds;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GlobalOptions {
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
    };

    private SharedPreferences mPreferences;

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
        return mPreferences.getBoolean("GlobalOptions.UniqueStarsAndPlanets", true);
    }

    /**
     * Pass this to \c addOnOptionsChangedListener to be notified when options change.
     */
    public interface OptionsChangedListener {
        void onOptionsChanged(GlobalOptions newOptions);
    }
}
