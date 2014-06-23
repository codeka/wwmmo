package au.com.codeka.warworlds.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    public static final int ANY_THREAD = 0;
    public static final int UI_THREAD = 1;

    /** Which thread should the callback be called on, {#ANY_THREAD} or {#UI_THREAD}? */
    int thread() default 0;
}
