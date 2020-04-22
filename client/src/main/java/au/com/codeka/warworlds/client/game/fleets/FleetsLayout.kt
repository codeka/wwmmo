package au.com.codeka.warworlds.client.game.fleets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ExpandableListView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.transition.TransitionManager
import au.com.codeka.warworlds.client.MainActivity
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.StarCollection
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.FleetHelper

/** Layout for the [FleetsScreen]. */
class FleetsLayout(context: Context?, starCollection: StarCollection?) : RelativeLayout(context) {
  private val starCollection: StarCollection?
  private val listView: ExpandableListView
  private val adapter = FleetExpandableStarListAdapter(LayoutInflater.from(context), starCollection)
  private val bottomPane: FrameLayout

  /**
   * Select the given fleet. This is expensive and should be avoided except when there's only
   * one (or a small, finite number of) stars.
   */
  fun selectFleet(fleetId: Long) {
    for (groupPosition in 0 until starCollection!!.size()) {
      val star = starCollection[groupPosition]
      if (FleetHelper.findFleet(star, fleetId) != null) {
        listView.expandGroup(groupPosition)
        adapter.setSelectedFleetId(star, fleetId)
        break
      }
    }
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    adapter.destroy()
  }

  fun showActionsPane() {
    showStarfield(false /* visible */)
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.removeAllViews()
    bottomPane.addView(ActionBottomPane(context, actionBottomPaneCallback))
  }

  fun showMovePane(star: Star, fleetId: Long) {
    showStarfield(true /* visible */)

    // TODO: the cast seems... not great.
    val starfieldManager = (context as MainActivity).starfieldManager
    val moveBottomPane = MoveBottomPane(context, starfieldManager) { showActionsPane() }
    moveBottomPane.setFleet(star, fleetId)
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.removeAllViews()
    bottomPane.addView(moveBottomPane)
  }

  fun showSplitPane(star: Star, fleetId: Long) {
    showStarfield(false /* visible */)
    val splitBottomPane = SplitBottomPane(context) { showActionsPane() }
    splitBottomPane.setFleet(star, fleetId)
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.removeAllViews()
    bottomPane.addView(splitBottomPane)
  }

  fun showMergePane(star: Star, fleetId: Long) {
    showStarfield(false /* visible */)
    val mergeBottomPane = MergeBottomPane(context, adapter) { showActionsPane() }
    mergeBottomPane.setFleet(star, fleetId)
    TransitionManager.beginDelayedTransition(bottomPane)
    bottomPane.removeAllViews()
    bottomPane.addView(mergeBottomPane)
  }

  fun showStarfield(visible: Boolean) {
    if (visible) {
      listView.visibility = View.GONE
      background = null
    } else {
      listView.visibility = View.VISIBLE
      setBackgroundResource(R.color.default_background)
    }
  }

  private val actionBottomPaneCallback: ActionBottomPane.Callback = object : ActionBottomPane.Callback {
    override fun onMoveClick() {
      val fleetId = adapter.selectedFleetId ?: return
      val star = adapter.selectedStar ?: return
      showMovePane(star, fleetId)
    }

    override fun onSplitClick() {
      val fleetId = adapter.selectedFleetId ?: return
      val star = adapter.selectedStar ?: return
      showSplitPane(star, fleetId)
    }

    override fun onMergeClick() {
      val fleetId = adapter.selectedFleetId ?: return
      val star = adapter.selectedStar ?: return
      showMergePane(star, fleetId)
    }
  }

  init {
    View.inflate(context, R.layout.fleets, this)
    this.starCollection = starCollection
    bottomPane = findViewById(R.id.bottom_pane)
    listView = findViewById(R.id.fleet_list)
    listView.setAdapter(adapter)
    if (starCollection!!.size() == 1) {
      // if it's just one star, just expand it now.
      listView.expandGroup(0)
    }
    listView.setOnChildClickListener { lv: ExpandableListView?, v: View?, groupPosition: Int, childPosition: Int, id: Long ->
      adapter.onItemClick(groupPosition, childPosition)
      false
    }

    // Actions pane by default.
    bottomPane.addView(ActionBottomPane(getContext(), actionBottomPaneCallback))
  }

}