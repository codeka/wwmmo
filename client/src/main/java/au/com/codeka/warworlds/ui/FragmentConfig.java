package au.com.codeka.warworlds.ui;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This is an annotation we put on our fragments which will give some config info to
 * @{link MainActivity}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FragmentConfig {
  /**
   * Whether this fragment doesn't want the toolbar visible at all (for example,
   * {@link au.com.codeka.warworlds.WelcomeFragment}).
   */
  boolean hideToolbar() default false;
}
