package au.com.codeka.warworlds.client.util;

/** Helper interface for callbacks that take a single parameter. */
public interface Callback<T> {
  void run(T param);
}
