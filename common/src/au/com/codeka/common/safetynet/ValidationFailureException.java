package au.com.codeka.common.safetynet;

public class ValidationFailureException extends Exception {
  private static final long serialVersionUID = 1L;

  private ValidationFailureReason reason;

  public ValidationFailureException(ValidationFailureReason reason) {
    super("Validation failed");
    this.reason = reason;
  }

  public ValidationFailureException(ValidationFailureReason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public ValidationFailureException(ValidationFailureReason reason, Throwable inner) {
    super("Validation failed", inner);
    this.reason = reason;
  }

  public ValidationFailureReason getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return super.toString() + ": " + reason;
  }
}
