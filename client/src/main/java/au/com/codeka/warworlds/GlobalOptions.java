package au.com.codeka.warworlds;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;

public class GlobalOptions {
  private SharedPreferences preferences;

  private static CopyOnWriteArrayList<OptionsChangedListener> mOptionsChangedListeners =
      new CopyOnWriteArrayList<>();

  public static void addOptionsChangedListener(OptionsChangedListener listener) {
    if (!mOptionsChangedListeners.contains(listener)) {
      mOptionsChangedListeners.add(listener);
    }
  }

  public static void removeOptionsChangedListener(OptionsChangedListener listener) {
    mOptionsChangedListeners.remove(listener);
  }

  protected void fireOptionsChanged(GlobalOptions newOptions) {
    for (OptionsChangedListener listener : mOptionsChangedListeners) {
      listener.onOptionsChanged(newOptions);
    }
  }

  public GlobalOptions() {
    preferences = PreferenceManager.getDefaultSharedPreferences(App.i);

    preferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
      if (key.startsWith("GlobalOptions.")) {
        fireOptionsChanged(GlobalOptions.this);
      }
    });
  }

  public StarfieldDetail getStarfieldDetail() {
    String val =
        preferences.getString(
            "GlobalOptions.StarfieldDetail", StarfieldDetail.STARS_AND_GAS.toString());
    for (StarfieldDetail d : StarfieldDetail.values()) {
      if (d.toString().equals(val)) {
        return d;
      }
    }

    return StarfieldDetail.STARS_AND_GAS;
  }

  public void setStarfieldDetail(StarfieldDetail detail) {
    if (getStarfieldDetail().equals(detail)) {
      return;
    }

    preferences.edit()
        .putString("GlobalOptions.StarfieldDetail", detail.toString())
        .apply();
  }

  public boolean uniqueStarsAndPlanets() {
    return preferences.getBoolean("GlobalOptions.GenUniqueStarsAndPlanets", false);
  }

  public boolean autoTranslateChatMessages() {
    return preferences.getBoolean("GlobalOptions.AutoTranslateChatMessages", false);
  }

  public void autoTranslateChatMessages(boolean value) {
    preferences.edit()
        .putBoolean("GlobalOptions.AutoTranslateChatMessages", value)
        .apply();
  }

  public ChatProfanityFilterLevel chatProfanityFilterLevel() {
    String s = preferences.getString("GlobalOptions.ChatProfanityFilterLevel",
        ChatProfanityFilterLevel.None.toString());
    return ChatProfanityFilterLevel.valueOf(s);
  }

  public void chatProfanityFilterLevel(ChatProfanityFilterLevel level) {
    preferences.edit()
        .putString("GlobalOptions.ChatProfanityFilterLevel", level.toString())
        .apply();
  }

  public AutoSendCrashReport getAutoSendCrashReport() {
    String str =
        preferences.getString(
            "GlobalOptions.AutoSendCrashReports", AutoSendCrashReport.Ask.toString());
    if (str.equals("0"))
      return AutoSendCrashReport.Ask;
    return AutoSendCrashReport.valueOf(str);
  }

  public void setAutoSendCrashReport(AutoSendCrashReport value) {
    preferences.edit()
        .putString("GlobalOptions.AutoSendCrashReports", value.toString())
        .apply();
  }

  public boolean notificationsEnabled() {
    return preferences.getBoolean("GlobalOptions.EnableNotifications", true);
  }

  public NotificationOptions getNotificationOptions(NotificationKind kind) {
    String baseName = "GlobalOptions.Notifications[" + kind + "].";
    NotificationOptions opt = new NotificationOptions();
    opt.mKind = kind;
    opt.mNotificationEnabled = preferences.getBoolean(baseName + "Enabled", true);
    opt.mLedColour = Color.parseColor(preferences.getString(baseName + "LedColour", "#FF0000"));
    opt.mRingtone = preferences.getString(baseName + "Ringtone",
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
    return opt;
  }

  public Map<NotificationKind, NotificationOptions> getNotificationOptions() {
    TreeMap<NotificationKind, NotificationOptions> options = new TreeMap<NotificationKind, NotificationOptions>();

    for (NotificationKind kind : NotificationKind.values()) {
      options.put(kind, getNotificationOptions(kind));
    }

    return options;
  }

  public ArrayList<Integer> getMutedConversations() {
    String name =
        "GlobalOptions.MutedConversations[" + RealmContext.i.getCurrentRealm().getID() + "]";
    String value = preferences.getString(name, "");
    ArrayList<Integer> conversationIDs = new ArrayList<Integer>();
    for (String s : value.split(",")) {
      try {
        conversationIDs.add(Integer.parseInt(s));
      } catch (NumberFormatException e) {
        // we can get this if there's no conversations muted (i.e. if value is an empty string)
      }
    }
    return conversationIDs;
  }

  public void muteConversation(int convID, boolean mute) {
    ArrayList<Integer> mutedConversations = getMutedConversations();
    if (mute && !mutedConversations.contains(convID)) {
      mutedConversations.add(convID);
    } else if (!mute && mutedConversations.contains(convID)) {
      mutedConversations.remove(Integer.valueOf(convID));
    }

    StringBuilder sb = new StringBuilder();
    for (Integer id : mutedConversations) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append(id);
    }

    String name =
        "GlobalOptions.MutedConversations[" + RealmContext.i.getCurrentRealm().getID() + "]";
    preferences.edit()
        .putString(name, sb.toString())
        .apply();
  }

  public boolean isConversationMuted(int convID) {
    ArrayList<Integer> mutedConversations = getMutedConversations();
    return mutedConversations.contains(convID);
  }

  public static class DefaultInitialFocus {
    public float focusPopulation = 0.25f;
    public float focusFarming = 0.25f;
    public float focusMining = 0.25f;
    public float focusConstruction = 0.25f;

    public DefaultInitialFocus() {
    }

    public DefaultInitialFocus(
        float focusPopulation, float focusFarming, float focusMining, float focusConstruction) {
      this.focusPopulation = focusPopulation;
      this.focusFarming = focusFarming;
      this.focusMining = focusMining;
      this.focusConstruction = focusConstruction;
    }
  }

  public DefaultInitialFocus getDefaultInitialFocus() {
    String val = preferences.getString("GlobalOptions.DefaultInitialFocus", "");
    DefaultInitialFocus dif = new DefaultInitialFocus();
    String[] split = val.split(",");
    if (split.length != 4) {
      return dif;
    }

    dif.focusPopulation = Float.parseFloat(split[0]);
    dif.focusFarming = Float.parseFloat(split[1]);
    dif.focusMining = Float.parseFloat(split[2]);
    dif.focusConstruction = Float.parseFloat(split[3]);
    return dif;
  }

  public void setDefaultInitialFocus(DefaultInitialFocus dif) {
    String val = String.format(
        Locale.ENGLISH, "%.2f,%.2f,%.2f,%.2f", dif.focusPopulation, dif.focusFarming,
        dif.focusMining, dif.focusConstruction);
    preferences.edit()
        .putString("GlobalOptions.DefaultInitialFocus", val)
        .apply();
  }

  /**
   * Pass this to \c addOnOptionsChangedListener to be notified when options change.
   */
  public interface OptionsChangedListener {
    void onOptionsChanged(GlobalOptions newOptions);
  }

  public enum ChatProfanityFilterLevel {
    All(0),
    AllowMild(1),
    None(2);

    private int mValue;

    ChatProfanityFilterLevel(int value) {
      mValue = value;
    }

    public int getValue() {
      return mValue;
    }

    public static ChatProfanityFilterLevel fromValue(int value) {
      for (ChatProfanityFilterLevel l : ChatProfanityFilterLevel.values()) {
        if (l.getValue() == value) {
          return l;
        }
      }

      return ChatProfanityFilterLevel.None;
    }
  }

  public enum StarfieldDetail {
    BLACK(0),
    STARS(1),
    STARS_AND_GAS(2);

    private int mValue;

    StarfieldDetail(int value) {
      mValue = value;
    }

    public int getValue() {
      return mValue;
    }

    public static StarfieldDetail fromValue(int value) {
      for (StarfieldDetail d : StarfieldDetail.values()) {
        if (d.getValue() == value) {
          return d;
        }
      }

      return StarfieldDetail.STARS_AND_GAS;
    }
  }

  public enum AutoSendCrashReport {
    Ask,
    Never,
    Always
  }

  public enum NotificationKind {
    OTHER,
    BUILDING_BUILD_COMPLETE,
    FLEET_BUILD_COMPLETE,
    FLEET_MOVE_COMPLETE,
    FLEET_UNDER_ATTACK,
    FLEET_DESTROYED,
    FLEET_VICTORIOUS,
    COLONY_DESTROYED,
    COLONY_ATTACKED,
    STAR_GOODS_ZERO,
    CHAT_MESSAGE
  }

  public static class NotificationOptions {
    private NotificationKind mKind;
    private boolean mNotificationEnabled;
    private int mLedColour;
    private String mRingtone;

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

    public String getRingtone() {
      return mRingtone;
    }

    public void setRingtone(String ringtone) {
      mRingtone = ringtone;
    }
  }
}
