package au.com.codeka.warworlds.client.game.fleets

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.fromHtml
import au.com.codeka.warworlds.client.game.starfield.scene.StarfieldManager
import au.com.codeka.warworlds.client.game.starfield.scene.StarfieldManager.TapListener
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.client.opengl.SceneObject
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Vector2
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.StarModification
import au.com.codeka.warworlds.common.sim.DesignHelper
import au.com.codeka.warworlds.common.sim.StarHelper
import com.google.common.base.Preconditions
import java.util.*
import kotlin.math.floor

/**
 * Bottom pane of the fleets view that contains the "move" function.
 */
@SuppressLint("ViewConstructor") // Must be constructed in code.
class MoveBottomPane(
    context: Context,
    private val starfieldManager: StarfieldManager,
    private val callback: () -> Unit) : RelativeLayout(context, null) {

  private lateinit var star: Star
  private lateinit var fleet: Fleet
  private var destStar: Star? = null
  private var fleetMoveIndicator: SceneObject? = null
  private var fleetMoveIndicatorFraction = 0f

  init {
    View.inflate(context, R.layout.ctrl_fleet_move_bottom_pane, this)
    findViewById<View>(R.id.move_btn).setOnClickListener { onMoveClick() }
    findViewById<View>(R.id.cancel_btn).setOnClickListener { onCancelClick() }
  }

  fun setFleet(star: Star, fleetId: Long) {
    for (fleet in star.fleets) {
      if (fleet.id == fleetId) {
        setFleet(star, fleet)
      }
    }
  }

  private fun setFleet(star: Star, fleet: Fleet) {
    this.star = star
    this.fleet = fleet
    if (isAttachedToWindow) {
      refreshStarfield()
    }
  }

  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    refreshStarfield()

    starfieldManager.addTapListener(tapListener)
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    starfieldManager.removeTapListener(tapListener)
    destroyFleetMoveIndicator()
  }

  /** Called when we've got a fleet and need to setup the starfield.  */
  private fun refreshStarfield() {
    starfieldManager.warpTo(star)
    starfieldManager.setSelectedStar(null)
    refreshMoveIndicator()
  }

  /**
   * Refreshes the "move indicator", which is the little ship icon on the starfield that shows
   * where you're going to move to.
   */
  private fun refreshMoveIndicator() {
    destroyFleetMoveIndicator()

    if (destStar != null) {
      val distanceInParsecs = StarHelper.distanceBetween(star, destStar!!)
      val leftDetails = String.format(Locale.ENGLISH, "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
          destStar!!.name, distanceInParsecs)
      (findViewById<View>(R.id.star_details_left) as TextView).text = fromHtml(leftDetails)
      val design = DesignHelper.getDesign(fleet.design_type)
      val timeInHours = distanceInParsecs / design.speed_px_per_hour!!
      val hrs = floor(timeInHours).toInt()
      val mins = floor((timeInHours - hrs) * 60.0f).toInt()
      val estimatedFuel = design.fuel_cost_per_px!! * distanceInParsecs * fleet.num_ships
      val actualFuel: Double = fleet.fuel_amount.toDouble()
      val fuel = String.format(Locale.US, "%.1f / %.1f", estimatedFuel, actualFuel)
      var fontOpen = ""
      var fontClose = ""
      if (estimatedFuel > actualFuel) {
        fontOpen = "<font color=\"#ff0000\">"
        fontClose = "</font>"
      }
      val rightDetails = String.format(
          Locale.ENGLISH,
          "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Energy:</b> %s%s",
          hrs, mins, fontOpen, fuel, fontClose)
      (findViewById<View>(R.id.star_details_right) as TextView).text = fromHtml(rightDetails)
    }
    val starSceneObject = starfieldManager.getStarSceneObject(star.id) ?: return
    val fmi = starfieldManager.createFleetSprite(fleet)

    fleetMoveIndicator = fmi
    fmi.setDrawRunnable {
      if (destStar != null) {
        fleetMoveIndicatorFraction += 0.032f // TODO: pass in dt here?
        while (fleetMoveIndicatorFraction > 1.0f) {
          fleetMoveIndicatorFraction -= 1.0f
        }
        val dir = StarHelper.directionBetween(star, destStar!!)
        val dirUnit = Vector2(dir.x, dir.y)
        dirUnit.normalize()
        val angle = Vector2.angleBetween(dirUnit, Vector2(0.0, -1.0))
        fmi.setRotation(angle, 0f, 0f, 1f)
        dir.scale(fleetMoveIndicatorFraction.toDouble())
        fmi.setTranslation(0.0f, dir.length().toFloat())
      }
    }
    starSceneObject.addChild(fmi)
  }

  private fun destroyFleetMoveIndicator() {
    val fmi = fleetMoveIndicator ?: return
    val fmiParent = fmi.parent ?: return
    fmiParent.removeChild(fmi)
    fleetMoveIndicator = null
  }

  private fun onMoveClick() {
    Preconditions.checkNotNull(star)
    Preconditions.checkNotNull(fleet)
    if (destStar == null) {
      return
    }
    StarManager.updateStar(star, StarModification(
        type = StarModification.MODIFICATION_TYPE.MOVE_FLEET,
        fleet_id = fleet.id,
        star_id = destStar!!.id))
    callback()
  }

  private fun onCancelClick() {
    callback()
  }

  private val tapListener: TapListener = object : TapListener {
    override fun onStarTapped(star: Star) {
      if (star.id == this@MoveBottomPane.star.id) {
        destStar = null
        refreshMoveIndicator()
        return
      }
      destStar = star
      refreshMoveIndicator()
    }

    override fun onFleetTapped(star: Star, fleet: Fleet) {
      // TODO: indicate that we don't want to select this fleet.
    }

    override fun onEmptySpaceTapped() {
      destStar = null
      refreshMoveIndicator()
    }
  }
}