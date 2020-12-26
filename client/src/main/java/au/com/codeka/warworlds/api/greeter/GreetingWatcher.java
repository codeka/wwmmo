package au.com.codeka.warworlds.api.greeter;

public interface GreetingWatcher {
  void onAuthenticating();

  void onConnecting();

  /**
   * Called when a failure occurs. If the GiveUpReason is NONE then we'll retry in a bit (and
   * call {@link #onRetry(int)} when we do). Otherwise, we're giving up.
   */
  void onFailed(String message, ServerGreeter.GiveUpReason reason);

  void onRetry(int retries);

  void onComplete(String serverVersion);
}
