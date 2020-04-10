package au.com.codeka.warworlds.client.ui;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.transition.ChangeBounds;
import androidx.transition.ChangeImageTransform;
import androidx.transition.ChangeTransform;
import androidx.transition.Fade;
import androidx.transition.TransitionSet;

/**
 * Helper class for creating transitions between fragments.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Transitions {
  public static class Transform extends TransitionSet {
    public Transform() {
      init();
    }

    private void init() {
      setOrdering(ORDERING_TOGETHER);
      this.addTransition(new ChangeBounds())
          .addTransition(new ChangeTransform())
          .addTransition(new ChangeImageTransform());
    }
  }

  public static Transform transform() {
    return new Transform();
  }

  public static Fade fade() {
    return new Fade();
  }
}
