package au.com.codeka.warworlds.client.store

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Base class for "store" class.  */
abstract class BaseStore(private val name: String, private val helper: SQLiteOpenHelper) {
  /**
   * Called by the [DataStore] when the database is created.
   */
  abstract fun onCreate(db: SQLiteDatabase)

  /**
   * Called by the [DataStore] when we need to upgrade. By default, does nothing.
   */
  open fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

}