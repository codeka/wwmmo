package au.com.codeka.warworlds.server.store

import java.sql.SQLException

/**
 * Exception that is thrown from the some of the store classes when an unrecoverable error occurs.
 */
class StoreException : Exception {
  /** Constructs a new [StoreException] that wraps the given [SQLException].  */
  constructor(e: SQLException) : super(e.message, e) {}

  /** Constructs a new [StoreException] with the given exception message.  */
  constructor(msg: String?) : super(msg) {}

  /** Constructs a new [StoreException] with the given exception.  */
  constructor(e: Exception?) : super(e) {}
}