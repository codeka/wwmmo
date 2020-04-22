package au.com.codeka.warworlds.client.game.fleets

import android.view.ViewGroup
import au.com.codeka.warworlds.client.game.world.ArrayListStarCollection
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection
import au.com.codeka.warworlds.client.game.world.StarCollection
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Star

/**
 * This screen contains a list of fleets, and lets you do all the interesting stuff on them (like
 * merge, split, move, etc).
 */
class FleetsScreen(star: Star?, private val initialFleetId: Long?) : Screen() {
  private var starCollection: StarCollection? = null
  private var layout: FleetsLayout? = null

  override fun onCreate(context: ScreenContext?, parent: ViewGroup?) {
    super.onCreate(context, parent)
    layout = FleetsLayout(context!!.activity, starCollection)
    if (initialFleetId != null) {
      layout!!.selectFleet(initialFleetId)
    }
  }

  override fun onShow(): ShowInfo? {
    return builder().view(layout).build()
  }

  /**
   * Construct a new [FleetsScreen].
   *
   * @param star The [Star] to display fleets of. If null, the fleets of all stars will
   * be displayed.
   * @param fleetId If non-null, the ID of the fleet to have initially selected. Only possible when
   * star is also non-null.
   */
  init {
    starCollection = if (star == null) {
      MyEmpireStarCollection()
    } else {
      ArrayListStarCollection.of(star)
    }
  }
}