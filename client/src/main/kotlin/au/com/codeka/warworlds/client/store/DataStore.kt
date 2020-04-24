package au.com.codeka.warworlds.client.store

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import au.com.codeka.warworlds.common.proto.Empire

/**
 * Wraps the main data store, ensures we have it available when it's needed.
 */
class DataStore {
  private var helper: StoreHelper? = null
  fun open(applicationContext: Context?) {
    helper = StoreHelper(applicationContext)
  }

  fun empires(): ProtobufStore<Empire> {
    return helper!!.empireStore
  }

  fun stars(): StarStore {
    return helper!!.starStore
  }

  fun chat(): ChatStore {
    return helper!!.chatStore
  }

  /**
   * Most of our data stores are basically long-&gt;blob mappings stored in a sqlite database. This
   * class manages a single instance of a sqlite database.
   */
  private class StoreHelper(applicationContext: Context?) : SQLiteOpenHelper(applicationContext, "objects.db", null, 1) {
    val empireStore: ProtobufStore<Empire>
    val starStore: StarStore
    val chatStore: ChatStore

    /**
     * This is called the first time we open the database, in order to create the required
     * tables, etc.
     */
    override fun onCreate(db: SQLiteDatabase) {
      empireStore.onCreate(db)
      starStore.onCreate(db)
      chatStore.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      empireStore.onUpgrade(db, oldVersion, newVersion)
      starStore.onUpgrade(db, oldVersion, newVersion)
      chatStore.onUpgrade(db, oldVersion, newVersion)
    }

    init {
      empireStore = ProtobufStore("empires", Empire::class.java, this)
      starStore = StarStore("stars", this)
      chatStore = ChatStore("chat", this)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        setWriteAheadLoggingEnabled(true)
      }
    }
  }
}