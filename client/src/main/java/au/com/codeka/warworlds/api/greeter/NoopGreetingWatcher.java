package au.com.codeka.warworlds.api.greeter;

public class NoopGreetingWatcher implements GreetingWatcher {
  @Override
  public void onAuthenticating() {
  }

  @Override
  public void onConnecting() {
  }

  @Override
  public void onFailed(String message, ServerGreeter.GiveUpReason reason) {
  }

  @Override
  public void onRetry(int retries) {
  }

  @Override
  public void onComplete(String serverVersion) {
  }
}
