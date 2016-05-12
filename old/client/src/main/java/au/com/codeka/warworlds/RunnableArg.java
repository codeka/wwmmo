package au.com.codeka.warworlds;

/** A {@link Runnable} that takes a single parameter. */
public interface RunnableArg<T> {
  void run(T arg);
}
