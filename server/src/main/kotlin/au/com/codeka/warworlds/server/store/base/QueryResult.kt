package au.com.codeka.warworlds.server.store.base

import java.sql.Connection
import java.sql.ResultSet

class QueryResult internal constructor(private val conn: Connection?, private val rs: ResultSet) : AutoCloseable {
  operator fun next(): Boolean {
    return rs.next()
  }

  fun getString(columnIndex: Int): String {
    return rs.getString(columnIndex + 1)
  }

  fun getStringOrNull(columnIndex: Int): String? {
    return rs.getString(columnIndex + 1)
  }

  fun getInt(columnIndex: Int): Int {
    return rs.getInt(columnIndex + 1)
  }

  fun getLong(columnIndex: Int): Long {
    return rs.getLong(columnIndex + 1)
  }

  fun getBytes(columnIndex: Int): ByteArray {
    return rs.getBytes(columnIndex + 1)
  }

  override fun close() {
    rs.close()
    conn?.close()
  }

}