package au.com.codeka.warworlds.client.game.starfield

import android.view.ViewGroup
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.chat.ChatScreen
import au.com.codeka.warworlds.client.game.fleets.FleetsScreen
import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemScreen
import au.com.codeka.warworlds.client.game.starfield.scene.StarfieldManager
import au.com.codeka.warworlds.client.game.starfield.scene.StarfieldManager.TapListener
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.SharedViews
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star

/**
 * This is the main fragment that shows the starfield, lets you navigate around, select stars
 * and fleets and so on.
 */
class StarfieldScreen : Screen() {
  private val log = Log("StarfieldScreen")
  private lateinit var starfieldManager: StarfieldManager
  private lateinit var context: ScreenContext
  private lateinit var layout: StarfieldLayout

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    this.context = context
    layout = StarfieldLayout(context.activity, layoutCallbacks)
    starfieldManager = context.activity.starfieldManager

    val selectedStar = starfieldManager.getSelectedStar()
    if (selectedStar != null) {
      showStarSelectedBottomPane(selectedStar)
    } else {
      showEmptyBottomPane(true)
    }
  }

  override fun onShow(): ShowInfo? {
    starfieldManager.addTapListener(tapListener)
    return ShowInfo.builder().view(layout).build()
  }

  override fun onHide() {
    starfieldManager.removeTapListener(tapListener)
  }

  private fun showEmptyBottomPane(instant: Boolean) {
    val emptyBottomPane = EmptyBottomPane(context.activity)
    layout.showBottomPane(emptyBottomPane, instant)
  }

  private fun showStarSelectedBottomPane(star: Star) {
    val starSelectedBottomPane = StarSelectedBottomPane(
        context.activity, star, object : StarSelectedBottomPane.Callback {
      override fun onStarClicked(star: Star, planet: Planet?) {
        context.pushScreen(
            SolarSystemScreen(star, -1 /* planetIndex */),
            SharedViews.builder()
                .addSharedView(R.id.top_pane)
                .addSharedView(R.id.bottom_pane)
                .build())
      }

      override fun onFleetClicked(star: Star, fleet: Fleet) {
        context.pushScreen(
            FleetsScreen(star, fleet.id),
            SharedViews.builder()
                .addSharedView(R.id.bottom_pane)
                .addSharedView(R.id.top_pane)
                .build())
      }

      override fun onScoutReportClicked(star: Star) {
        showScoutReportBottomPane(star)
      }
    })
    layout.showBottomPane(starSelectedBottomPane, false /* instant */)
  }

  private fun showFleetSelectedBottomPane(star: Star, fleet: Fleet) {
    val fleetSelectedBottomPane = FleetSelectedBottomPane(
        context.activity, star, fleet)
    layout.showBottomPane(fleetSelectedBottomPane, false /* instant */)
  }

  private fun showScoutReportBottomPane(star: Star) {
    val scoutReportBottomPane = ScoutReportBottomPane(
        context.activity, star, object : ScoutReportBottomPane.Callback {
      override fun onBackClicked() {
        showStarSelectedBottomPane(star)
      }
    })
    layout.showBottomPane(scoutReportBottomPane, false /* instant */)
  }

  private val tapListener: TapListener = object : TapListener {
    override fun onStarTapped(star: Star) {
      showStarSelectedBottomPane(star)
    }

    override fun onFleetTapped(star: Star, fleet: Fleet) {
      showFleetSelectedBottomPane(star, fleet)
    }

    override fun onEmptySpaceTapped() {
      showEmptyBottomPane(false)
    }
  }

  private val layoutCallbacks = object : StarfieldLayout.Callbacks {
    override fun onChatClick(roomId: Long?) {
      context.pushScreen(
          ChatScreen(),
          SharedViews.builder()
              .addSharedView(R.id.bottom_pane)
              .build())
    }
  }
}