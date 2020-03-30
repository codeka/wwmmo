package au.com.codeka.common.safetynet;

import java.nio.ByteBuffer;

import au.com.codeka.common.Log;

/**
 * NonceVerifier is the counterpart to {@link NonceBuilder}. It takes a nonce and extracts the
 * information we added to it in the builder.
 */
public class NonceVerifier {
  private static final Log log = new Log("NonceVerifier");
  private final ByteBuffer nonce;

  private long timestamp;
  private byte[] random;
  private int empireId;

  public NonceVerifier(byte[] nonce) {
    this.nonce = ByteBuffer.wrap(nonce);
  }

  /**
   * Validate the nonce. Once it's been validated, you'll be able to query for the various
   * properties that have been included.
   */
  public boolean validate() {
    int version = nonce.get();
    if (version == 1) {
      return validateVersion1();
    } else {
      log.warning("Nonce cannot be validated, version %d unexpected.", version);
      return false;
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getEmpireId() {
    return empireId;
  }

  private boolean validateVersion1() {
    if (nonce.remaining() != 23) {
      log.warning("Nonce cannot be validated, %d bytes long, instead of 23.", nonce.remaining());
      return false;
    }

    // First. we get the time, with some adjustments to allow it to fit in 4 bytes. We'll convert
    // the time back into an epoch time, relative to System.currentTimeMillis()
    int time = nonce.getInt();
    timestamp = ((long) time * 1000) + Internal.EPOCH_OFFSET;

    // Next, 15 random bytes.
    // TODO: verify that we haven't seen these bytes before.
    random = new byte[15];
    nonce.get(random);

    // Finally, the empire ID
    empireId = nonce.getInt();
    if (empireId <= 0) {
      log.warning("Nonce cannot be validated, empireId (%d) is negative.", empireId);
      return false;
    }

    return true;
  }
}
