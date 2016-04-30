package au.com.codeka.warworlds.client.opengl;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * Helper class for converting between various dimension units.
 */
public class DimensionResolver {
  private final DisplayMetrics displayMetrics;

  public DimensionResolver(Context context) {
    this.displayMetrics = context.getResources().getDisplayMetrics();
  }

  public float dp2px(float dp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
  }
}
