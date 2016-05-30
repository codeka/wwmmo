package au.com.codeka.warworlds.client.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;

import com.google.common.base.Preconditions;
import com.squareup.wire.Message;

import java.io.File;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Wraps the main data store, ensures we have it available when it's needed.
 */
public class DataStore {
  private StoreHelper helper;

  public void open(Context applicationContext) {
    helper = new StoreHelper(applicationContext);
  }

  public ProtobufStore<Empire> empires() {
    return helper.empireStore;
  }

  public ProtobufStore<Star> stars() {
    return helper.starStore;
  }

  /**
   * Most of our data stores are basically long-&gt;blob mappings stored in a sqlite database. This
   * class manages a single instance of a sqlite database.
   */
  private static class StoreHelper extends SQLiteOpenHelper {
    private ProtobufStore<Empire> empireStore;
    private ProtobufStore<Star> starStore;

    public StoreHelper(Context applicationContext) {
      super(applicationContext, "objects.db", null, 1);
      empireStore = new ProtobufStore<>("empires", Empire.class, this);
      starStore = new ProtobufStore<>("stars", Star.class, this);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        getWritableDatabase();
        setWriteAheadLoggingEnabled(true);
      }
    }

    /**
     * This is called the first time we open the database, in order to create the required
     * tables, etc.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
      empireStore.onCreate(db);
      starStore.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
  }
}
