package au.com.codeka.warworlds.client.store;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import au.com.codeka.warworlds.common.proto.Empire;

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

  public StarStore stars() {
    return helper.starStore;
  }

  public ChatStore chat() {
    return helper.chatStore;
  }

  /**
   * Most of our data stores are basically long-&gt;blob mappings stored in a sqlite database. This
   * class manages a single instance of a sqlite database.
   */
  private static class StoreHelper extends SQLiteOpenHelper {
    private ProtobufStore<Empire> empireStore;
    private StarStore starStore;
    private ChatStore chatStore;

    public StoreHelper(Context applicationContext) {
      super(applicationContext, "objects.db", null, 1);
      empireStore = new ProtobufStore<>("empires", Empire.class, this);
      starStore = new StarStore("stars", this);
      chatStore = new ChatStore("chat", this);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
      chatStore.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      empireStore.onUpgrade(db, oldVersion, newVersion);
      starStore.onUpgrade(db, oldVersion, newVersion);
      chatStore.onUpgrade(db, oldVersion, newVersion);
    }
  }
}
