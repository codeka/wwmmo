package au.com.codeka.warworlds.client.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Base class for "store" class that has key type K and value type V.
 */
public abstract class BaseStore<K, V> {
  private final String name;
  private final SQLiteOpenHelper helper;

  public BaseStore(String name, SQLiteOpenHelper helper) {
    this.name = name;
    this.helper = helper;
  }

  /**
   * Called by the {@link DataStore} when the database is created.
   */
  public abstract void onCreate(SQLiteDatabase db);

  /**
   * Called by the {@link DataStore} when we need to upgrade. By default, does nothing.
   */
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }
}