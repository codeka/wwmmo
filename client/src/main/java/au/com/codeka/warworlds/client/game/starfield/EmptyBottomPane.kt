package au.com.codeka.warworlds.client.game.starfield

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import au.com.codeka.warworlds.client.R

/**
 * The bottom pane when you have nothing selected.
 */
class EmptyBottomPane(context: Context?) : FrameLayout(context!!, null) {
  init {
    View.inflate(context, R.layout.starfield_bottom_pane_empty, this)
  }
}