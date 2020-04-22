package au.com.codeka.warworlds.client.game.build

import android.text.SpannableStringBuilder
import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Star
import java.util.*
import kotlin.collections.ArrayList

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
class BuildScreen(private var star: Star?, planetIndex: Int) : Screen() {
  private var context: ScreenContext? = null
  private var colonies: MutableList<Colony?> = ArrayList()
  private var currColony: Colony? = null
  private var layout: BuildLayout? = null

  override fun onCreate(context: ScreenContext?, parent: ViewGroup?) {
    super.onCreate(context, parent)
    this.context = context
    layout = BuildLayout(context!!.activity, star, colonies, colonies.indexOf(currColony))
    layout!!.refreshColonyDetails(currColony)
    App.i.eventBus.register(eventHandler)
  }

  override fun onShow(): ShowInfo? {
    // Refresh immediately on show
    layout!!.post(refreshRunnable)
    return builder().view(layout).build()
  }

  override fun onHide() {
    layout!!.removeCallbacks(refreshRunnable)
  }

  override fun onDestroy() {
    App.i.eventBus.unregister(eventHandler)
  }

  /* TODO: redraw callback */
  override val title: CharSequence
    get() {
      val ssb = SpannableStringBuilder()
      ssb.append("○ ")
      ssb.append(star!!.name)
      ssb.append(" • Build")
      ImageHelper.bindStarIcon(
          ssb, 0, 1, context!!.activity, star, 24,  /* TODO: redraw callback */null)
      return ssb
    }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onStarUpdated(s: Star) {
      if (star != null && star!!.id == s.id) {
        updateStar(s)
      }
    }
  }

  private fun updateStar(s: Star) {
    log.info("Updating star %d [%s]...", s.id, s.name)
    val oldColony = currColony
    star = s
    extractColonies(star, -1)
    if (oldColony != null) {
      for (colony in colonies!!) {
        if (colony!!.id == oldColony.id) {
          currColony = colony
        }
      }
    }
    layout!!.refresh(star, colonies)
  }

  private fun extractColonies(star: Star?, planetIndex: Int) {
    val myEmpire = EmpireManager.i.myEmpire
    colonies.clear()
    currColony = null
    for (planet in star!!.planets) {
      if (planet.colony != null && planet.colony.empire_id != null && planet.colony.empire_id == myEmpire.id) {
        colonies.add(planet.colony)
        if (planet.index == planetIndex) {
          currColony = planet.colony
        }
      }
    }
  }

  private val refreshRunnable: Runnable = object : Runnable {
    private var refreshCount = 0
    override fun run() {
      refreshCount++
      if (refreshCount % 10 == 0) {
        // Every tenth refresh, we'll re-simulate the star
        StarManager.i.queueSimulateStar(star)
      } else {
        layout!!.refresh(star, colonies)
      }
      layout!!.postDelayed(this, REFRESH_DELAY_MS)
    }
  }

  companion object {
    private val log = Log("BuildScreen")

    /** We'll let the layout know to refresh progress and so on at this frequency.  */
    private const val REFRESH_DELAY_MS = 1000L
  }

  init {
    extractColonies(star, planetIndex)
    if (currColony == null) {
      // Shouldn't happen, but maybe we were given a bad planetIndex?
      currColony = colonies!![0]
    }
  }
}