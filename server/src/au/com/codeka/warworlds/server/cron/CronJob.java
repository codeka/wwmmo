package au.com.codeka.warworlds.server.cron;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CronJob {
  /** The display name of this cron job, shown to the admin. */
  String name();

  /** A more complete description of the job. Could be empty. */
  String desc() default "";
}
