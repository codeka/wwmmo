package au.com.codeka.warworlds.client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.App;

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

  public enum Key {
    /** If true, we'll automatically translate chat messages to English. */
    CHAT_AUTO_TRANSLATE(ValueType.BOOLEAN, false),

    /** How much we should filter chat message which contain profanity. */
    CHAT_PROFANITY_FILTER(ValueType.ENUM, ChatProfanityFilter.class, ChatProfanityFilter.AllowAll),

    /** The cookie used to authenicate with the server. */
    COOKIE(ValueType.STRING, ""),

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

    Key(
        ValueType valueType,
        @NonNull Class<? extends Enum> enumType,
        Object defValue) {
      this.valueType = valueType;
      this.enumType = enumType;
      this.defValue = defValue;
    }
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

  private GameSettings() {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.i);
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
}
