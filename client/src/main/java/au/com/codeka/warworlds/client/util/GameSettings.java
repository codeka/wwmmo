package au.com.codeka.warworlds.client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.App;

/** Wrapper class around our {@link SharedPreferences} instance. */
public class GameSettings {
  public static GameSettings i = new GameSettings();

  private enum ValueType {
    BOOLEAN,
    INT,
    STRING
  }

  public enum Key {
    /** The cookie used to authenicate with the server. */
    COOKIE("Cookie", ValueType.STRING, ""),

    /** Set to true after you've seen the warm welcome, so we don't show it again. */
    WARM_WELCOME_SEEN("WarmWelcomeSeen", ValueType.BOOLEAN, false);

    private String name;
    private ValueType valueType;
    private Object defValue;

    Key(String name, ValueType valueType, Object defValue) {
      this.name = name;
      this.valueType = valueType;
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
      editor.putBoolean(key.name, value);
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
    sharedPreferences = App.i.getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
  }

  public boolean getBoolean(Key key) {
    Preconditions.checkArgument(key.valueType == ValueType.BOOLEAN);
    return sharedPreferences.getBoolean(key.name, (boolean) key.defValue);
  }

  public int getInt(Key key) {
    Preconditions.checkArgument(key.valueType == ValueType.INT);
    return sharedPreferences.getInt(key.name, (int) key.defValue);
  }

  public String getString(Key key) {
    Preconditions.checkArgument(key.valueType == ValueType.STRING);
    return sharedPreferences.getString(key.name, (String) key.defValue);
  }

  public Editor edit() {
    return new Editor(sharedPreferences);
  }
}
