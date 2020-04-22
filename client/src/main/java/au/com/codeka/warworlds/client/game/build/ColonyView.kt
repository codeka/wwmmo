package au.com.codeka.warworlds.client.game.build

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionManager
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ui.views.TabPlusContentView
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Star
import com.google.android.material.tabs.TabLayout

/**
 * View which contains the tabs for a single colony. Each tab in turn contains a list of buildings,
 * ships and the queue.
 */
class ColonyView(
    context: Context, private var star: Star?, private var colony: Colony?,
    private val buildLayout: BuildLayout): TabPlusContentView(context) {
  private var contentView: TabContentView? = null
  fun refresh(star: Star?, colony: Colony?) {
    this.star = star
    this.colony = colony
    if (contentView != null) {
      contentView!!.refresh(star, colony)
    }
  }

  override fun onTabSelected(tab: TabLayout.Tab?, index: Int) {
    val tabContent: ViewGroup = tabContent
    TransitionManager.beginDelayedTransition(tabContent)
    buildLayout.hideBottomSheet()
    tabContent.removeAllViews()
    if (tab!!.position == 0) {
      contentView = BuildingsView(context, star, colony, buildLayout)
    } else if (tab.position == 1) {
      contentView = ShipsView(context, star, colony, buildLayout)
    } else if (tab.position == 2) {
      contentView = QueueView(context, star, colony)
    }
    tabContent.addView(contentView as View?)
  }

  init {
    addTab(R.string.buildings)
    addTab(R.string.ships)
    addTab(R.string.queue)
  }
}