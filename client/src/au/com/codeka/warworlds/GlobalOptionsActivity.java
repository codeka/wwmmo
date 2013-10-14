package au.com.codeka.warworlds;

import java.util.Locale;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.text.Html;

@SuppressWarnings("deprecation") // we use the deprecated "single list of
                                 // preferences" here, because we want to support
                                 // older versions of Android, and there's no
                                 // implementing this twice when we only have
                                 // one level of preferences anyway.
public class GlobalOptionsActivity extends PreferenceActivity
                                   implements SharedPreferences.OnSharedPreferenceChangeListener {

    final static String[] sColourValues = {
        "#FFFFFF",
        "#FF0000",
        "#00FF00",
        "#0000FF"
    };
    final static CharSequence[] sColourDisplay = {
        Html.fromHtml("<font color=\"#ffffff\">White</font>"),
        Html.fromHtml("<font color=\"#ff0000\">Red</font>"),
        Html.fromHtml("<font color=\"#00ff00\">Green</font>"),
        Html.fromHtml("<font color=\"#0000ff\">Blue</font>"),
    };

    final static String[] sAutoSendCrashReportValues = {
        GlobalOptions.AutoSendCrashReport.Ask.toString(),
        GlobalOptions.AutoSendCrashReport.Never.toString(),
        GlobalOptions.AutoSendCrashReport.Always.toString()
    };
    final static String[] sAutoSendCrashReportDisplay = {
        "Ask",
        "Never",
        "Always (recommended)"
    };

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

        ListPreference autoSendCrashReport = (ListPreference) getPreferenceScreen().findPreference(
                "GlobalOptions.AutoSendCrashReports");
        autoSendCrashReport.setEntryValues(sAutoSendCrashReportValues);
        autoSendCrashReport.setEntries(sAutoSendCrashReportDisplay);

        for (GlobalOptions.NotificationKind kind : GlobalOptions.NotificationKind.values()) {
            ListPreference colour = (ListPreference) getPreferenceScreen().findPreference(
                    String.format("GlobalOptions.Notifications[%s].LedColour", kind));
            if (colour != null) {
                colour.setEntryValues(sColourValues);
                colour.setEntries(sColourDisplay);
            }

            RingtonePreference ringtone = (RingtonePreference) getPreferenceScreen().findPreference(
                    String.format("GlobalOptions.Notifications[%s].Ringtone", kind));
            if (ringtone != null) {
                ringtone.setDefaultValue(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }
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
        GlobalOptions opts = new GlobalOptions();

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

        p = getPreferenceScreen().findPreference("GlobalOptions.AutoTranslateChatMessages");
        if (opts.autoTranslateChatMessages()) {
            p.setSummary("Auto-translate non-English chat messages to English.");
        } else {
            p.setSummary("Display chat messages in their native language.");
        }

        p = getPreferenceScreen().findPreference("GlobalOptions.EnableNotifications");
        if (opts.notificationsEnabled()) {
            p.setSummary("Notifications are enabled");
        } else {
            p.setSummary("You will not receive any notifications.");
        }

        p = getPreferenceScreen().findPreference("GlobalOptions.AutoSendCrashReports");
        switch (opts.getAutoSendCrashReport()) {
        case Ask:
            p.setSummary("Ask every time");
            break;
        case Always:
            p.setSummary("Send automatically");
            break;
        case Never:
            p.setSummary("Never send");
            break;
        }

        for (GlobalOptions.NotificationKind kind : GlobalOptions.NotificationKind.values()) {
            String prefBaseName = String.format("GlobalOptions.Notifications[%s]", kind);
            p = getPreferenceScreen().findPreference(prefBaseName);
            if (p == null) {
                continue;
            }

            GlobalOptions.NotificationOptions options = opts.getNotificationOptions(kind);

            Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(options.getRingtone()));
            CharSequence colourName = "Unknown";
            for (int i = 0; i < sColourValues.length; i++) {
                int c = Color.parseColor(sColourValues[i]);
                if (options.getLedColour() == c) {
                    colourName = sColourDisplay[i];
                }
            }

            if (!options.isEnabled()) {
                p.setSummary("Disabled");
            } else if (ringtone != null) {
                p.setSummary(String.format(Locale.ENGLISH, "%s, LED: %s",
                                           ringtone.getTitle(this), colourName));
            } else {
                p.setSummary(String.format(Locale.ENGLISH, "LED: %s", colourName));
            }

            p = getPreferenceScreen().findPreference(prefBaseName+".LedColour");
            if (p != null) {
                p.setSummary(colourName);
            }


            p = getPreferenceScreen().findPreference(prefBaseName+".Ringtone");
            if (p != null && ringtone != null) {
                p.setSummary(ringtone.getTitle(this));
            }
        }
    }
}
