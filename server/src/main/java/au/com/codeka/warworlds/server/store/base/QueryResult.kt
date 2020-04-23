package au.com.codeka.warworlds.server.store.base

import au.com.codeka.warworlds.server.store.StoreException
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class QueryResult internal constructor(private val conn: Connection?, private val rs: ResultSet) : AutoCloseable {
  operator fun next(): Boolean {
    return try {
      rs.next()
    } catch (e: SQLException) {
      throw StoreException(e)
    }
  }

  fun getString(columnIndex: Int): String {
    return try {
      rs.getString(columnIndex + 1)
    } catch (e: SQLException) {
      throw StoreException(e)
    }
  }

  fun getInt(columnIndex: Int): Int {
    return try {
      rs.getInt(columnIndex + 1)
    } catch (e: SQLException) {
      throw StoreException(e)
    }
  }

  fun getLong(columnIndex: Int): Long {
    return try {
      rs.getLong(columnIndex + 1)
    } catch (e: SQLException) {
      throw StoreException(e)
    }
  }

  fun getBytes(columnIndex: Int): ByteArray {
    return try {
      rs.getBytes(columnIndex + 1)
    } catch (e: SQLException) {
      throw StoreException(e)
    }
  }

  override fun close() {
    rs.close()
    conn?.close()
  }

}