package au.com.codeka.common.safetynet;

public enum ValidationFailureReason {
  NONE,

  /** The JWS string is missing, or indicated an error on the client. */
  JSW_MISSING,

  /** The JWS string we got on the server was invalid (we couldn't parse it). */
  INVALID_JWS,

  /** There was an error verifying the signature of the JWS attestation. */
  JWS_SIGNATURE_ERROR,

  /** The signature of the JWS attestation isn't for attest.android.com. */
  JWS_SIGNATURE_HOSTNAME_MISMATCH,
}
