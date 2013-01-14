package au.com.codeka.warworlds;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

@SuppressWarnings("deprecation") // we use the deprecated "single list of
                                 // preferences" here, because we want to support
                                 // older versions of Android, and there's no
                                 // implementing this twice when we only have
                                 // one level of preferences anyway.
public class GlobalOptionsActivity extends PreferenceActivity
                                   implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.global_options);

        ListPreference starfieldDetail = (ListPreference) getPreferenceScreen()
                .findPreference("GlobalOptions.StarfieldDetail");
        String[] values = {
            GlobalOptions.StarfieldDetail.BLACK.toString(),
            GlobalOptions.StarfieldDetail.STARS.toString(),
            GlobalOptions.StarfieldDetail.STARS_AND_GAS.toString()
        };
        String[] displayValues = {
            "Solid black (fastest)",
            "Background stars only",
            "Background stars and Nebulae"
        };
        starfieldDetail.setEntryValues(values);
        starfieldDetail.setEntries(displayValues);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                             .registerOnSharedPreferenceChangeListener(this);

        refreshPreferenceSummaries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                             .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        refreshPreferenceSummaries();
    }

    private void refreshPreferenceSummaries() {
        GlobalOptions opts = new GlobalOptions(this);

        Preference p = getPreferenceScreen().findPreference("GlobalOptions.StarfieldDetail");
        switch (opts.getStarfieldDetail()) {
        case BLACK:
            p.setSummary("Solid black (fastest)");
            break;
        case STARS:
            p.setSummary("Background stars only");
            break;
        case STARS_AND_GAS:
            p.setSummary("Background stars and Nebulae");
            break;
        }

        p = getPreferenceScreen().findPreference("GlobalOptions.GenUniqueStarsAndPlanets");
        if (opts.uniqueStarsAndPlanets()) {
            p.setSummary("Generate unique star and planet images");
        } else {
            p.setSummary("Use generic star and planet images");
        }
    }
}
