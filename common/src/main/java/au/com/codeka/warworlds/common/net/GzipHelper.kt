package au.com.codeka.warworlds.common.net

import au.com.codeka.warworlds.common.Log
import com.google.common.io.ByteStreams
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Helper for working with Gzip compression.
 */
object GzipHelper {
  private val log = Log("GzipHelper")

  /** Compress the given byte array.  */
  fun compress(uncompressed: ByteArray?): ByteArray? {
    return try {
      val outs = ByteArrayOutputStream()
      val zip = GZIPOutputStream(outs)
      zip.write(uncompressed)
      zip.close()
      outs.toByteArray()
    } catch (e: IOException) {
      log.warning("Error compressing byte array.", e)
      null
    }
  }

  /** Decompress the given byte array.  */
  fun decompress(compressed: ByteArray?): ByteArray? {
    return try {
      val ins = ByteArrayInputStream(compressed)
      val zip = GZIPInputStream(ins)
      ByteStreams.toByteArray(zip)
    } catch (e: IOException) {
      log.warning("Error decompressing byte array.", e)
      null
    }
  }
}