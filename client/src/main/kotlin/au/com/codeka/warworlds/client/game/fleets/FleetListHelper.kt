package au.com.codeka.warworlds.client.game.fleets

import android.content.Context
import android.text.Html
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.fromHtml
import au.com.codeka.warworlds.client.game.build.BuildViewHelper.setDesignIcon
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.util.Callback
import au.com.codeka.warworlds.common.TimeFormatter
import au.com.codeka.warworlds.common.proto.BuildRequest
import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.google.common.base.CaseFormat
import java.util.*
import kotlin.math.roundToInt

object FleetListHelper {
  fun populateFleetRow(row: ViewGroup?, fleet: Fleet, design: Design) {
    val destinationView = row!!.findViewById<TextView>(R.id.fleet_row3)
    val destination = getFleetDestination(
        row.context, fleet, true,
        object : Callback<SpannableStringBuilder> {
          override fun run(param: SpannableStringBuilder) {
            // We have to set the text to something different, then set it back again for it to
            // actually notice that it's changed.
            destinationView.text = ""
            destinationView.text = param
          }
        })
    destinationView.text = destination
    setDesignIcon(design, row.findViewById(R.id.fleet_icon))
    (row.findViewById<View>(R.id.fleet_row1) as TextView).text = getFleetName(fleet, design)
    (row.findViewById<View>(R.id.fleet_row2) as TextView).text = getFleetStance(fleet)
    val fuelLevelIcon = row.findViewById<ImageView>(R.id.fuel_level_icon)
    val fuelLevelText = row.findViewById<TextView>(R.id.fuel_level_text)
    val fuelLevel = row.findViewById<ProgressBar>(R.id.fuel_level)
    if (fuelLevel != null && fuelLevelText != null && fuelLevelIcon != null) {
      if (fleet.fuel_amount >= design.fuel_size!! * fleet.num_ships) {
        fuelLevel.visibility = View.GONE
        fuelLevelText.visibility = View.GONE
        fuelLevelIcon.visibility = View.GONE
      } else {
        fuelLevel.visibility = View.VISIBLE
        fuelLevelText.visibility = View.VISIBLE
        fuelLevelIcon.visibility = View.VISIBLE
        val fuelPercent = 100 * fleet.fuel_amount / (design.fuel_size!! * fleet.num_ships)
        fuelLevel.progress = fuelPercent.roundToInt()
        fuelLevelText.text = String.format(
            Locale.ENGLISH, "%.0f / %.0f", fleet.fuel_amount,
            design.fuel_size!! * fleet.num_ships)
      }
    }
  }

  /**
   * Gets the name of a fleet.
   *
   * @param fleet The fleet whose name we want to get. Can be null if there is no fleet (actually
   * this is just used to get the count of ships).
   * @param design The ship's [Design].
   */
  fun getFleetName(fleet: Fleet?, design: Design): CharSequence {
    return getFleetName(if (fleet == null) 1 else Math.ceil(fleet.num_ships.toDouble()).toInt(), design)
  }

  /**
   * Gets the name of a fleet.
   *
   * @param buildRequest The [BuildRequest] that we're building, used to get the count.
   * @param design The ship's [Design].
   */
  fun getFleetName(buildRequest: BuildRequest?, design: Design): CharSequence {
    return getFleetName(if (buildRequest == null) 1 else buildRequest.count!!, design)
  }

  /**
   * Gets the name of a fleet.
   *
   * @param count The number of ships in this fleet.
   * @param design The ship's [Design].
   */
  private fun getFleetName(count: Int, design: Design): CharSequence {
    val ssb = SpannableStringBuilder()
    if (count <= 1) {
      val text = String.format(Locale.ENGLISH, "%s",
          DesignHelper.getDesignName(design, false /* plural */))
      ssb.append(text)
    } else  /*if (upgrades.size() == 0) */ {
      val text = String.format(Locale.ENGLISH, "%d × %s",
          count, DesignHelper.getDesignName(design, true /* plural */))
      ssb.append(text)
    } /*else {
      String text = String.format(Locale.ENGLISH, "%d ×", (int) Math.ceil(fleet.getNumShips()));
      addTextToRow(context, row, text, textSize);
      for (BaseFleetUpgrade upgrade : fleet.getUpgrades()) {
        Sprite sprite = SpriteManager.i.getSprite(design.getUpgrade(upgrade.getUpgradeID()).getSpriteName());
        addImageToRow(context, row, sprite, textSize);
      }
      text = String.format(Locale.ENGLISH, "%s", design.getDisplayName(fleet.getNumShips() > 1));
      addTextToRow(context, row, text, textSize);
    }*/
    return ssb
  }

  /** Gets the destination text for the given fleet, or null if the fleet is not moving.  */
  fun getFleetDestination(
      context: Context, fleet: Fleet, includeEta: Boolean,
      needRedrawCallback: Callback<SpannableStringBuilder>): CharSequence? {
    if (fleet.destination_star_id == null) {
      return null
    }
    val destStar = StarManager.getStar(fleet.destination_star_id!!)
    return destStar?.let { getFleetDestination(context, fleet, it, includeEta, needRedrawCallback) }
  }

  private fun getFleetDestination(
      context: Context, fleet: Fleet, dest: Star, includeEta: Boolean,
      needRedrawCallback: Callback<SpannableStringBuilder>): CharSequence {
    val ssb = SpannableStringBuilder()
    val eta = TimeFormatter.create().format(fleet.eta!! - System.currentTimeMillis())

//    float marginHorz = 0;
//    float marginVert = 0;
//    if (dest.classification.getImageScale() > 2.5) {
//      marginHorz = -(float) (sprite.getWidth() / dest.getStarType().getImageScale());
//      marginVert = -(float) (sprite.getHeight() / dest.getStarType().getImageScale());
//    }

//    BoostFleetUpgrade boostUpgrade = (BoostFleetUpgrade) fleet.getUpgrade("boost");
//    if (boostUpgrade != null && boostUpgrade.isBoosting()) {
//      addTextToRow(context, row, "→", 0);
//    }
    ssb.append("→ ○ ")
    ImageHelper.bindStarIcon(
        ssb, ssb.length - 2, ssb.length - 1, context, dest, 16, needRedrawCallback)
    var name = dest.name
    if (dest.classification == Star.CLASSIFICATION.MARKER) {
      name = "<i>Empty Space</i>"
    }
    if (includeEta) {
      val text = String.format("%s <b>ETA:</b> %s", name, eta)
      ssb.append(fromHtml(text))
    } else {
      ssb.append(fromHtml(name))
    }
    return ssb
  }

  fun getFleetStance(fleet: Fleet): CharSequence {
    return String.format("%s (stance: %s)",
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.state.toString()),
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.stance.toString()))
  }
}