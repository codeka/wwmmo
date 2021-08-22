package au.com.codeka.warworlds.client.util.eventbus

import au.com.codeka.warworlds.client.concurrency.Threads

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    /** Which thread should the callback be called on, [Threads.UI], etc?  */
    val thread: Threads = Threads.UI)
