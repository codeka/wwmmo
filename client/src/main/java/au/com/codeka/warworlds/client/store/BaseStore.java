package au.com.codeka.warworlds.client.store;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Base class for "store" class. */
public abstract class BaseStore {
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
