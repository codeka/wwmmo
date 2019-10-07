package au.com.codeka.warworlds;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.text.Html;
import android.view.MenuItem;
import android.widget.FrameLayout;

import java.util.Locale;

import au.com.codeka.warworlds.ctrl.PreferenceFragment;

public class GlobalOptionsActivity extends BaseActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FrameLayout frame = new FrameLayout(this);
    frame.setId(R.id.content);
    setContentView(frame);
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.content, new GlobalOptionsFragment(), null)
        .commit();

    // We have to offset the drawerLayout a bit because the action bar will be covering it.
    final TypedArray styledAttributes =
        getTheme().obtainStyledAttributes(new int[] {android.R.attr.actionBarSize});
    int actionBarHeight = (int) styledAttributes.getDimension(0, 0);
    styledAttributes.recycle();
    ((FrameLayout.LayoutParams) frame.getLayoutParams()).topMargin = actionBarHeight;

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setTitle("Options");
  }

  /** We want an action bar, so we override this to return true. */
  @Override
  protected boolean wantsActionBar() {
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public static class GlobalOptionsFragment extends PreferenceFragment
      implements SharedPreferences.OnSharedPreferenceChangeListener {
    final static String[] STARFIELD_DETAIL_VALUES = {
        GlobalOptions.StarfieldDetail.BLACK.toString(),
        GlobalOptions.StarfieldDetail.STARS.toString(),
        GlobalOptions.StarfieldDetail.STARS_AND_GAS.toString() };
    final static CharSequence[] STARFIELD_DETAIL_DISPLAY = {
        "Solid black (fastest)", "Background stars only", "Background stars and Nebulae" };

    final static String[] COLOUR_VALUES = {"#FFFFFF", "#FF0000", "#00FF00", "#0000FF"};
    final static CharSequence[] COLOUR_DISPLAY = {
        Html.fromHtml("<font color=\"#ffffff\">White</font>"),
        Html.fromHtml("<font color=\"#ff0000\">Red</font>"),
        Html.fromHtml("<font color=\"#00ff00\">Green</font>"),
        Html.fromHtml("<font color=\"#0000ff\">Blue</font>"),
    };

    final static String[] AUTO_SEND_CRASH_REPORT_VALUES = {
        GlobalOptions.AutoSendCrashReport.Ask.toString(),
        GlobalOptions.AutoSendCrashReport.Never.toString(),
        GlobalOptions.AutoSendCrashReport.Always.toString() };
    final static String[] AUTO_SEND_CRASH_REPORT_DISPLAY = {"Ask", "Never", "Always (recommended)"};

    final static String[] CHAT_PROFANITY_FILTER_LEVEL_VALUES = {
        GlobalOptions.ChatProfanityFilterLevel.All.toString(),
        GlobalOptions.ChatProfanityFilterLevel.AllowMild.toString(),
        GlobalOptions.ChatProfanityFilterLevel.None.toString()};
    final static String[] CHAT_PROFANITY_FILTER_LEVEL_DISPLAY = {
        "Filter all profanity", "Filter only strong profanity", "Filter nothing"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.global_options);

      ListPreference starfieldDetail = (ListPreference) findPreference(
          "GlobalOptions.StarfieldDetail");
      starfieldDetail.setEntryValues(STARFIELD_DETAIL_VALUES);
      starfieldDetail.setEntries(STARFIELD_DETAIL_DISPLAY);

      ListPreference autoSendCrashReport = (ListPreference) findPreference(
          "GlobalOptions.AutoSendCrashReports");
      autoSendCrashReport.setEntryValues(AUTO_SEND_CRASH_REPORT_VALUES);
      autoSendCrashReport.setEntries(AUTO_SEND_CRASH_REPORT_DISPLAY);

      ListPreference chatProfanityFilterLevel = (ListPreference) findPreference(
          "GlobalOptions.ChatProfanityFilterLevel");
      chatProfanityFilterLevel.setEntryValues(CHAT_PROFANITY_FILTER_LEVEL_VALUES);
      chatProfanityFilterLevel.setEntries(CHAT_PROFANITY_FILTER_LEVEL_DISPLAY);

      for (GlobalOptions.NotificationKind kind : GlobalOptions.NotificationKind.values()) {
        ListPreference colour = (ListPreference) findPreference(
            String.format("GlobalOptions.Notifications[%s].LedColour", kind));
        if (colour != null) {
          colour.setEntryValues(COLOUR_VALUES);
          colour.setEntries(COLOUR_DISPLAY);
        }

        RingtonePreference ringtone = (RingtonePreference) findPreference(
            String.format("GlobalOptions.Notifications[%s].Ringtone", kind));
        if (ringtone != null) {
          ringtone.setDefaultValue(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }
      }
    }

    @Override
    public void onStart() {
      super.onStart();
      getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

      refreshPreferenceSummaries();
    }

    @Override
    public void onStop() {
      super.onStop();
      getPreferenceManager().getSharedPreferences()
          .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      refreshPreferenceSummaries();
    }

    private void refreshPreferenceSummaries() {
      GlobalOptions opts = new GlobalOptions();

      Preference p = findPreference("GlobalOptions.StarfieldDetail");
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

      p = findPreference("GlobalOptions.GenUniqueStarsAndPlanets");
      if (opts.uniqueStarsAndPlanets()) {
        p.setSummary("Generate unique star and planet images");
      } else {
        p.setSummary("Use generic star and planet images");
      }

      p = findPreference("GlobalOptions.NonSecureServerConnection");
      p.setSummary("Not recommended, only try this if secure connection does not work.");

      p = findPreference("GlobalOptions.AutoTranslateChatMessages");
      if (opts.autoTranslateChatMessages()) {
        p.setSummary("Auto-translate non-English chat messages to English.");
      } else {
        p.setSummary("Display chat messages in their native language.");
      }

      p = findPreference("GlobalOptions.ChatProfanityFilterLevel");
      p.setSummary(CHAT_PROFANITY_FILTER_LEVEL_DISPLAY[opts.chatProfanityFilterLevel().getValue()]);

      p = findPreference("GlobalOptions.EnableNotifications");
      if (opts.notificationsEnabled()) {
        p.setSummary("Notifications are enabled");
      } else {
        p.setSummary("You will not receive any notifications.");
      }

      p = findPreference("GlobalOptions.AutoSendCrashReports");
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
        p = findPreference(prefBaseName);
        if (p == null) {
          continue;
        }

        GlobalOptions.NotificationOptions options = opts.getNotificationOptions(kind);

        Ringtone ringtone = RingtoneManager.getRingtone(
            getActivity(), Uri.parse(options.getRingtone()));
        CharSequence colourName = "Unknown";
        for (int i = 0; i < COLOUR_VALUES.length; i++) {
          int c = Color.parseColor(COLOUR_VALUES[i]);
          if (options.getLedColour() == c) {
            colourName = COLOUR_DISPLAY[i];
          }
        }

        if (!options.isEnabled()) {
          p.setSummary("Disabled");
        } else if (ringtone != null) {
          p.setSummary(
              String.format(Locale.ENGLISH, "%s, LED: %s",
                  ringtone.getTitle(getActivity()), colourName));
        } else {
          p.setSummary(String.format(Locale.ENGLISH, "LED: %s", colourName));
        }

        p = findPreference(prefBaseName + ".LedColour");
        if (p != null) {
          p.setSummary(colourName);
        }


        p = findPreference(prefBaseName + ".Ringtone");
        if (p != null && ringtone != null) {
          p.setSummary(ringtone.getTitle(getActivity()));
        }
      }
    }
  }
}
