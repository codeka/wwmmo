package au.com.codeka.warworlds.client.util;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.UUID;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.BuildConfig;

/** Wrapper class around our {@link SharedPreferences} instance. */
public class GameSettings {
  public static GameSettings i = new GameSettings();

  private enum ValueType {
    BOOLEAN,
    INT,
    STRING,
    ENUM
  }

  public enum ChatProfanityFilter {
    AllowAll,
    AllowMild,
    AllowNone,
  }

  /** An enumeration for the current state of the sign in process. */
  public enum SignInState {
    /** The initial state: you're an 'anonymous' user, with no email address. */
    ANONYMOUS,

    /** You've entered an email address, but we're currently awaiting your verification. */
    AWAITING_VERIFICATION,

    /** You've verified your email address. */
    VERIFIED,
  }

  public enum Key {
    /** If true, we'll automatically translate chat messages to English. */
    CHAT_AUTO_TRANSLATE(ValueType.BOOLEAN, false),

    /** How much we should filter chat message which contain profanity. */
    CHAT_PROFANITY_FILTER(ChatProfanityFilter.AllowAll),

    /** The cookie used to authenicate with the server. */
    COOKIE(ValueType.STRING, ""),

    /** If you've associated with an email address, this is it. */
    EMAIL_ADDR(ValueType.STRING, ""),

    /** A unique ID that's gauranteed to not change (as long as the user doesn't clear app data) */
    INSTANCE_ID(ValueType.STRING, ""),

    /** Your current {@link SignInState}. */
    SIGN_IN_STATE(SignInState.ANONYMOUS),

    /** The base URL of the server. */
    SERVER(ValueType.STRING, BuildConfig.DEFAULT_SERVER),

    /** Set to true after you've seen the warm welcome, so we don't show it again. */
    WARM_WELCOME_SEEN(ValueType.BOOLEAN, false);

    private final ValueType valueType;
    @Nullable private final Class<? extends Enum> enumType;
    private final Object defValue;

    Key(ValueType valueType, Object defValue) {
      this.valueType = valueType;
      this.enumType = null;
      this.defValue = defValue;
    }

    Key(Enum defValue) {
      this.valueType = ValueType.ENUM;
      this.enumType = defValue.getClass();
      this.defValue = defValue;
    }
  }

  public interface SettingChangeHandler {
    void onSettingChanged(Key key);
  }

  public class Editor {
    private SharedPreferences.Editor editor;
    private boolean committed;

    @SuppressLint("CommitPrefEdits") // we have our own commit() method.
    private Editor(SharedPreferences sharedPreferences) {
      editor = sharedPreferences.edit();
    }

    public Editor setBoolean(Key key, boolean value) {
      Preconditions.checkState(key.valueType == ValueType.BOOLEAN);
      editor.putBoolean(key.toString(), value);
      return this;
    }

    public <T extends Enum> Editor setEnum(Key key, T value) {
      Preconditions.checkArgument(key.valueType == ValueType.ENUM);
      Preconditions.checkArgument(key.enumType == value.getClass());
      editor.putString(key.toString(), value.toString());
      return this;
    }

    public Editor setString(Key key, String value) {
      Preconditions.checkState(key.valueType == ValueType.STRING);
      editor.putString(key.toString(), value);
      return this;
    }

    public void commit() {
      editor.apply();
      committed = true;
    }

    @Override
    public void finalize() throws Throwable {
      super.finalize();

      if (!committed) {
        // TODO: log, or something?
        commit();
      }
    }
  }

  private SharedPreferences sharedPreferences;
  private final ArrayList<SettingChangeHandler> settingChangeHandlers = new ArrayList<>();

  private GameSettings() {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.i);
    sharedPreferences.registerOnSharedPreferenceChangeListener(onPrefChangedListener);

    if (getString(Key.INSTANCE_ID).equals("")) {
      edit().setString(Key.INSTANCE_ID, UUID.randomUUID().toString()).commit();
    }
  }

  public void addSettingChangedHandler(SettingChangeHandler handler) {
    settingChangeHandlers.add(handler);
  }

  public void removeSettingChangedHandler(SettingChangeHandler handler) {
    settingChangeHandlers.remove(handler);
  }

  public boolean getBoolean(Key key) {sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.i);
    Preconditions.checkArgument(key.valueType == ValueType.BOOLEAN);
    return sharedPreferences.getBoolean(key.toString(), (boolean) key.defValue);
  }

  public <T extends Enum> T getEnum(Key key, Class<? extends T> enumType) {
    Preconditions.checkArgument(key.valueType == ValueType.ENUM);
    Preconditions.checkArgument(key.enumType == enumType);
    String strValue = sharedPreferences.getString(key.toString(), key.defValue.toString());
    return (T) T.valueOf(enumType, strValue);
  }

  public int getInt(Key key) {
    Preconditions.checkArgument(key.valueType == ValueType.INT);
    return sharedPreferences.getInt(key.toString(), (int) key.defValue);
  }

  public String getString(Key key) {
    Preconditions.checkArgument(key.valueType == ValueType.STRING);
    return sharedPreferences.getString(key.toString(), (String) key.defValue);
  }

  public Editor edit() {
    return new Editor(sharedPreferences);
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener onPrefChangedListener
      = new SharedPreferences.OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {
      Key key;
      try {
        key = Key.valueOf(keyName);
      } catch (IllegalArgumentException e) {
        // This will happen if the setting isn't one of ours. Ignore.
        return;
      }
      for (SettingChangeHandler handler : settingChangeHandlers) {
        handler.onSettingChanged(key);
      }
    }
  };
}
