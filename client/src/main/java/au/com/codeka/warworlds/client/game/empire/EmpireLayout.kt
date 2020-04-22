package au.com.codeka.warworlds.client.game.empire

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionManager
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.fleets.FleetsLayout
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection
import au.com.codeka.warworlds.client.ui.views.TabPlusContentView
import com.google.android.material.tabs.TabLayout

/**
 * Layout for the [EmpireScreen].
 */
class EmpireLayout(context: Context, private val settingsCallbacks: SettingsView.Callback)
  : TabPlusContentView(context) {
  override fun onTabSelected(tab: TabLayout.Tab?, index: Int) {
    val tabContent: ViewGroup = tabContent
    TransitionManager.beginDelayedTransition(tabContent)
    tabContent.removeAllViews()
    var contentView: View? = null
    if (index == 0) {
      contentView = OverviewView(context)
    } else if (index == 1) {
      contentView = ColoniesView(context)
    } else if (index == 2) {
      contentView = BuildQueueView(context)
    } else if (index == 3) {
      contentView = FleetsLayout(context, MyEmpireStarCollection())
    } else if (index == 4) {
      contentView = SettingsView(context, settingsCallbacks)
    }
    tabContent.addView(contentView)
  }

  init {
    setBackgroundColor(context.resources.getColor(R.color.default_background))
    addTab(R.string.overview)
    addTab(R.string.colonies)
    addTab(R.string.build)
    addTab(R.string.fleets)
    addTab(R.string.settings)
  }
}