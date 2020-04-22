package au.com.codeka.warworlds.client.game.solarsystem

import android.text.SpannableStringBuilder
import android.view.ViewGroup
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.ColonyFocus
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.StarModification
import com.google.common.base.Preconditions

/**
 * Activity for interacting with enemy planets (note it's not necessarily an enemy, per se, it
 * could also be an ally or faction member).
 */
class PlanetDetailsScreen(private val star: Star, private val planet: Planet) : Screen() {
  private var context: ScreenContext? = null
  private var layout: PlanetDetailsLayout? = null
  override fun onCreate(context: ScreenContext?, container: ViewGroup?) {
    super.onCreate(context, container)
    this.context = context
    layout = PlanetDetailsLayout(context!!.activity, star, planet, layoutCallbacks)
  }

  override fun onShow(): ShowInfo? {
    return builder().view(layout).build()
  }

  /* TODO: redraw callback */
  override val title: CharSequence
    get() {
      val ssb = SpannableStringBuilder()
      ssb.append("○ ")
      ssb.append(star.name)
      ssb.append(" ")
      ssb.append(RomanNumeralFormatter.format(planet.index + 1))
      ImageHelper.bindStarIcon(
          ssb, 0, 1, context!!.activity, star, 24,  /* TODO: redraw callback */null)
      return ssb
    }

  private val layoutCallbacks: PlanetDetailsLayout.Callbacks = object : PlanetDetailsLayout.Callbacks {
    override fun onSaveFocusClick(
        farmingFocus: Float, miningFocus: Float, energyFocus: Float, constructionFocus: Float) {
      Preconditions.checkState(planet.colony != null && planet.colony.id != null)
      StarManager.i.updateStar(star, StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.ADJUST_FOCUS)
          .colony_id(planet.colony.id)
          .focus(ColonyFocus.Builder()
              .farming(farmingFocus)
              .mining(miningFocus)
              .energy(energyFocus)
              .construction(constructionFocus)
              .build()))
      context!!.popScreen()
    }

    override fun onAttackClick() {
      if (planet.colony == null) {
        return
      }
      val myEmpire = Preconditions.checkNotNull(EmpireManager.i.myEmpire)
      StarManager.i.updateStar(star, StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.ATTACK_COLONY)
          .empire_id(myEmpire.id)
          .colony_id(planet.colony.id))
      context!!.popScreen()
    }

    override fun onColonizeClick() {
      val myEmpire = Preconditions.checkNotNull(EmpireManager.i.myEmpire)
      StarManager.i.updateStar(star, StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.COLONIZE)
          .empire_id(myEmpire.id)
          .planet_index(planet.index))
      context!!.popScreen()
    }
  }

  companion object {
    private val log = Log("PlanetDetailsScreen")
  }

}