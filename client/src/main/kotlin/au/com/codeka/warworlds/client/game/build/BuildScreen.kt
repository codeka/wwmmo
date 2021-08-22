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
import au.com.codeka.warworlds.client.util.Callback
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Star
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
class BuildScreen(private var star: Star, planetIndex: Int) : Screen() {
  private var context: ScreenContext? = null
  private var colonies: MutableList<Colony> = ArrayList()
  private var currColony: Colony
  private lateinit var layout: BuildLayout

  init {
    currColony = extractColonies(star, planetIndex)
  }

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    this.context = context
    layout = BuildLayout(context.activity, star, colonies, colonies.indexOf(currColony))
    layout.refreshColonyDetails(currColony)
    App.eventBus.register(eventHandler)
  }

  override fun onShow(): ShowInfo {
    // Refresh immediately on show
    layout.post(refreshRunnable)
    return builder().view(layout).build()
  }

  override fun onHide() {
    layout.removeCallbacks(refreshRunnable)
  }

  override fun onDestroy() {
    App.eventBus.unregister(eventHandler)
  }

  /* TODO: redraw callback */
  override val title: CharSequence
    get() {
      val ssb = SpannableStringBuilder()
      ssb.append("○ ")
      ssb.append(star.name)
      ssb.append(" • Build")
      ImageHelper.bindStarIcon(
          ssb, 0, 1, context!!.activity, star, 24, object : Callback<SpannableStringBuilder> {
        override fun run(param: SpannableStringBuilder) {
          // TODO: handle this
        }
      })
      return ssb
    }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onStarUpdated(s: Star) {
      if (star.id == s.id) {
        updateStar(s)
      }
    }
  }

  private fun updateStar(s: Star) {
    log.info("Updating star %d [%s]...", s.id, s.name)
    val oldColony = currColony
    star = s
    currColony = extractColonies(star, -1)
    for (colony in colonies) {
      if (colony.id == oldColony.id) {
        currColony = colony
      }
    }
    layout.refresh(star, colonies)
  }

  private fun extractColonies(star: Star, planetIndex: Int): Colony {
    val myEmpire = EmpireManager.getMyEmpire()
    var selectedColony: Colony? = null
    colonies.clear()
    for (planet in star.planets) {
      val colony = planet.colony ?: continue
      if (colony.empire_id != null && colony.empire_id == myEmpire.id) {
        colonies.add(colony)
        if (planet.index == planetIndex) {
          selectedColony = colony
        }
      }
    }
    if (selectedColony == null) {
      throw IllegalStateException("Given planet doesn't have a colony.")
    }
    return selectedColony
  }

  private val refreshRunnable: Runnable = object : Runnable {
    private var refreshCount = 0
    override fun run() {
      refreshCount++
      if (refreshCount % 10 == 0) {
        // Every tenth refresh, we'll re-simulate the star
        StarManager.queueSimulateStar(star)
      } else {
        layout.refresh(star, colonies)
      }
      layout.postDelayed(this, REFRESH_DELAY_MS)
    }
  }

  companion object {
    private val log = Log("BuildScreen")

    /** We'll let the layout know to refresh progress and so on at this frequency.  */
    private const val REFRESH_DELAY_MS = 1000L
  }

}