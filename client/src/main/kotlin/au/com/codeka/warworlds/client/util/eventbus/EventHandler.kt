package au.com.codeka.warworlds.client.util.eventbus

import au.com.codeka.warworlds.client.concurrency.Threads
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
annotation class EventHandler(
    /** Which thread should the callback be called on, [Threads.UI], etc?  */
    val thread: Threads = Threads.UI)