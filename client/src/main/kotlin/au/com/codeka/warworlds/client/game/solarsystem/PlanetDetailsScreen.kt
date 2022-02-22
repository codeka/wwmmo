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
import au.com.codeka.warworlds.client.util.Callback
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
  private lateinit var context: ScreenContext
  private lateinit var layout: PlanetDetailsLayout

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    this.context = context
    layout = PlanetDetailsLayout(context.activity, star, planet, layoutCallbacks)
  }

  override fun onShow(): ShowInfo {
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
          ssb, 0, 1, context.activity, star, 24, object : Callback<SpannableStringBuilder> {
            override fun run(param: SpannableStringBuilder) {
              // TODO: handle this
            }
          })
      return ssb
    }

  private val layoutCallbacks: PlanetDetailsLayout.Callbacks = object : PlanetDetailsLayout.Callbacks {
    override fun onSaveFocusClick(
        farmingFocus: Float, miningFocus: Float, energyFocus: Float, constructionFocus: Float) {
      val colony = planet.colony ?: return
      StarManager.updateStar(star, StarModification(
          type = StarModification.Type.ADJUST_FOCUS,
          colony_id = colony.id,
          focus = ColonyFocus(
              farming = farmingFocus,
              mining = miningFocus,
              energy = energyFocus,
              construction = constructionFocus)))
      context.popScreen()
    }

    override fun onAttackClick() {
      val colony = planet.colony ?: return
      val myEmpire = EmpireManager.getMyEmpire()
      StarManager.updateStar(star, StarModification(
          type = StarModification.Type.ATTACK_COLONY,
          empire_id = myEmpire.id,
          colony_id = colony.id))
      context.popScreen()
    }

    override fun onColonizeClick() {
      val myEmpire = EmpireManager.getMyEmpire()
      StarManager.updateStar(star, StarModification(
          type = StarModification.Type.COLONIZE,
          empire_id = myEmpire.id,
          planet_index = planet.index))
      context.popScreen()
    }
  }
}
