package au.com.codeka.warworlds.client.util

/** Helper interface for callbacks that take a single parameter.  */
interface Callback<T> {
  fun run(param: T)
}