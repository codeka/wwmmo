package au.com.codeka.common.nonce;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

/**
 * NonceBuilder adds some data you pass in to the nonce, and then lets you take the bytes and
 * validate it with {@link NonceVerifier}.
 */
public class NonceBuilder {
  private int time;
  private int version;
  private int empireID;

  private static final Random random = new SecureRandom();

  public NonceBuilder() {
    version = Internal.VERSION;
    time = (int)((System.currentTimeMillis() / 1000) - Internal.EPOCH_OFFSET);
  }

  public NonceBuilder empireID(int empireID) {
    this.empireID = empireID;
    return this;
  }

  public byte[] build() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(24);
    byteBuffer.put((byte) version);
    byteBuffer.putInt(time);

    byte[] randomBytes = new byte[15];
    random.nextBytes(randomBytes);
    byteBuffer.put(randomBytes);

    byteBuffer.putInt(empireID);

    return byteBuffer.array();
  }
}
