package au.com.codeka.warworlds.client.game.starfield

import android.content.Context
import android.os.Handler
import android.text.Html
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.*
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.build.BuildViewHelper.setDesignIcon
import au.com.codeka.warworlds.client.game.fleets.FleetListHelper.getFleetDestination
import au.com.codeka.warworlds.client.game.fleets.FleetListHelper.getFleetName
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.util.Callback
import au.com.codeka.warworlds.common.TimeFormatter
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper
import au.com.codeka.warworlds.common.sim.StarHelper
import com.squareup.picasso.Picasso
import java.util.*

/**
 * Bottom pane for when you have a fleet selected.
 */
class FleetSelectedBottomPane(
    context: Context?,
    star: Star?,
    fleet: Fleet) : FrameLayout(context!!, null) {
  private val star: Star?
  private val fleet: Fleet
  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    refreshFleet()
  }

  private fun refreshFleet() {
    if (!isAttachedToWindow) {
      return
    }
    val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
    val progressText = findViewById<TextView>(R.id.progress_text)
    val destStar = StarManager.getStar(fleet.destination_star_id)
    if (destStar != null) {
      val distanceInParsecs = StarHelper.distanceBetween(star, destStar)
      val startTime = fleet.state_start_time
      val eta = fleet.eta
      val fractionRemaining = 1.0f - (System.currentTimeMillis() - startTime).toFloat() / (eta - startTime).toFloat()
      progressBar.max = 1000
      progressBar.progress = 1000 - (fractionRemaining * 1000.0f).toInt()
      val msg = String.format(Locale.ENGLISH, "<b>ETA</b>: %.1f pc in %s",
          distanceInParsecs * fractionRemaining,
          TimeFormatter.create().format(eta - System.currentTimeMillis()))
      progressText.text = Html.fromHtml(msg)
    }
    handler.postDelayed({ refreshFleet() }, REFRESH_DELAY_MS)
  }

  companion object {
    private const val REFRESH_DELAY_MS = 1000L
  }

  init {
    View.inflate(context, R.layout.starfield_bottom_pane_fleet, this)
    this.fleet = fleet
    this.star = star
    val fleetIcon = findViewById<ImageView>(R.id.fleet_icon)
    val empireIcon = findViewById<ImageView>(R.id.empire_icon)
    val fleetDesign = findViewById<TextView>(R.id.fleet_design)
    val empireName = findViewById<TextView>(R.id.empire_name)
    val fleetDestination = findViewById<TextView>(R.id.fleet_destination)
    val boostBtn = findViewById<Button>(R.id.boost_btn)
    empireName.text = ""
    empireIcon.setImageBitmap(null)
    val design = DesignHelper.getDesign(fleet.design_type)
    val empire = EmpireManager.getEmpire(fleet.empire_id)
    if (empire != null) {
      empireName.text = empire.display_name
      Picasso.get()
          .load(ImageHelper.getEmpireImageUrl(getContext(), empire, 20, 20))
          .into(empireIcon)
    }
    fleetDesign.text = getFleetName(fleet, design /*, 18.0f*/)
    val destination = getFleetDestination(
        getContext(), fleet, true, object : Callback<SpannableStringBuilder> {
      override fun run(param: SpannableStringBuilder) {
        fleetDestination.text = ""
        fleetDestination.text = param
      }
    })
    fleetDestination.text = destination
    setDesignIcon(design, fleetIcon)

    //FleetUpgrade.BoostFleetUpgrade fleetUpgrade = (FleetUpgrade.BoostFleetUpgrade) fleet.getUpgrade("boost");
    //if (fleetUpgrade != null && !fleetUpgrade.isBoosting()) {
    //  boostBtn.setEnabled(true);
    //} else {
    boostBtn.isEnabled = false
    // }
  }
}
