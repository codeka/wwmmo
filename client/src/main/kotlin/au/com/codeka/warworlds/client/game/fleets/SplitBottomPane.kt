package au.com.codeka.warworlds.client.game.fleets

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.StarModification
import au.com.codeka.warworlds.common.sim.DesignHelper
import java.util.*
import kotlin.math.floor

/**
 * Bottom pane of the fleets view that contains the "split" function.
 */
@SuppressLint("ViewConstructor") // Must be constructed in code.
class SplitBottomPane(
    context: Context?,
    private val cancelCallback: () -> Unit) : RelativeLayout(context, null) {
  private val fleetDetails: ViewGroup
  private val splitLeft: EditText
  private val splitRight: EditText
  private val splitRatio: SeekBar

  /** If true, we should ignore callbacks because we're currently editing it in code.  */
  private var ignoreEdits = false

  /** The fleet we're splitting, may be null if [.setFleet] hasn't been called.  */
  var fleet: Fleet? = null

  /** The star of the fleet we're splitting.  */
  var star: Star? = null

  init {
    View.inflate(context, R.layout.ctrl_fleet_split_bottom_pane, this)
    fleetDetails = findViewById(R.id.fleet)
    splitLeft = findViewById(R.id.split_left)
    splitRight = findViewById(R.id.split_right)
    splitRatio = findViewById(R.id.split_ratio)
    findViewById<View>(R.id.split_btn).setOnClickListener { onSplitClick() }
    findViewById<View>(R.id.cancel_btn).setOnClickListener { onCancelClick() }
    splitLeft.addTextChangedListener(SplitTextWatcher(true))
    splitRight.addTextChangedListener(SplitTextWatcher(false))
    splitRatio.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && !ignoreEdits && fleet != null) {
          update(progress, floor(fleet!!.num_ships.toDouble()).toInt() - progress)
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    })
  }

  /** Set the fleet we're displaying to the one with the given ID on the given star.  */
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
    FleetListHelper.populateFleetRow(
        fleetDetails, fleet, DesignHelper.getDesign(fleet.design_type))
    val leftCount = floor(fleet.num_ships.toDouble()).toInt() / 2
    val rightCount = floor(fleet.num_ships.toDouble()).toInt() - leftCount
    update(leftCount, rightCount)
  }

  private fun update(leftCount: Int, rightCount: Int) {
    ignoreEdits = true
    splitLeft.setText(String.format(Locale.ENGLISH, "%d", leftCount))
    splitRight.setText(String.format(Locale.ENGLISH, "%d", rightCount))
    splitRatio.max = leftCount + rightCount - 1
    splitRatio.progress = leftCount - 1
    ignoreEdits = false
  }

  private fun onSplitClick() {
    if (star == null || fleet == null) {
      return
    }
    StarManager.updateStar(star!!, StarModification(
        type = StarModification.Type.SPLIT_FLEET,
        fleet_id = fleet!!.id,
        count = splitRatio.max - splitRatio.progress))
    cancelCallback()
  }

  private fun onCancelClick() {
    cancelCallback()
  }

  private inner class SplitTextWatcher(private val isLeft: Boolean) : TextWatcher {
    override fun afterTextChanged(editable: Editable) {
      if (fleet == null || ignoreEdits) {
        return
      }
      if (editable.toString().isEmpty()) {
        // You've deleted the whole text. No biggie.
        return
      }
      val n: Int = try {
        editable.toString().toInt()
      } catch (e: NumberFormatException) {
        // Invalid number format, just ignore for now.
        return
      }
      val leftCount: Int
      val rightCount: Int
      if (isLeft) {
        leftCount = n
        rightCount = floor(fleet!!.num_ships.toDouble()).toInt() - leftCount
      } else {
        rightCount = n
        leftCount = floor(fleet!!.num_ships.toDouble()).toInt() - rightCount
      }
      update(leftCount, rightCount)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
  }
}