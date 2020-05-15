package au.com.codeka.warworlds.client.opengl

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * Helper class for converting between various dimension units.
 */
class DimensionResolver(context: Context) {
  private val displayMetrics: DisplayMetrics = context.resources.displayMetrics

  fun dp2px(dp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)
  }

  fun px2dp(px: Float): Float {
    return px / displayMetrics.density
  }
}