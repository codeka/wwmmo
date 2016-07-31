package au.com.codeka.warworlds.common.net;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import au.com.codeka.warworlds.common.Log;

/**
 * Helper for working with Gzip compression.
 */
public class GzipHelper {
  private static final Log log = new Log("GzipHelper");

  /** Compress the given byte array. */
  public static byte[] compress(byte[] uncompressed) {
    try {
      ByteArrayOutputStream outs = new ByteArrayOutputStream();
      GZIPOutputStream zip = new GZIPOutputStream(outs);
      zip.write(uncompressed);
      zip.close();
      return outs.toByteArray();
    } catch (IOException e) {
      log.warning("Error compressing byte array.", e);
      return null;
    }
  }

  /** Decompress the given byte array. */
  public static byte[] decompress(byte[] compressed) {
    try {
      ByteArrayInputStream ins = new ByteArrayInputStream(compressed);
      GZIPInputStream zip = new GZIPInputStream(ins);
      return ByteStreams.toByteArray(zip);
    } catch (IOException e) {
      log.warning("Error decompressing byte array.", e);
      return null;
    }
  }
}
