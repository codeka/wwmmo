package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.StarModification

/**
 * An exception that is thrown when a suspicious modification to a star happens.
 */
class SuspiciousModificationException(
    val starId: Long,
    val modification: StarModification,
    fmt: String?,
    vararg args: Any?) : Exception(String.format(fmt!!, *args))