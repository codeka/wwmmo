package au.com.codeka.warworlds.api;

/**
 * Represents the current request manager state.
 */
public class RequestManagerState {
  public int numInflightRequests;

  public RequestManagerState(int numInflightRequests) {
    this.numInflightRequests = numInflightRequests;
  }
}
