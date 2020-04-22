package au.com.codeka.warworlds.client.game.fleets

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
import com.google.common.base.Preconditions
import java.util.*

/**
 * Bottom pane of the fleets view that contains the "split" function.
 */
class SplitBottomPane(context: Context?, cancelCallback: () -> Unit) : RelativeLayout(context, null) {
  private val cancelCallback: () -> Unit
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
    val leftCount = Math.floor(fleet.num_ships.toDouble()).toInt() / 2
    val rightCount = Math.floor(fleet.num_ships.toDouble()).toInt() - leftCount
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

  private fun onSplitClick(view: View) {
    if (star == null || fleet == null) {
      return
    }
    StarManager.i.updateStar(star, StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.SPLIT_FLEET)
        .fleet_id(fleet!!.id)
        .count(splitRatio.max - splitRatio.progress))
    cancelCallback()
  }

  private fun onCancelClick(view: View) {
    cancelCallback()
  }

  private inner class SplitTextWatcher internal constructor(private val isLeft: Boolean) : TextWatcher {
    override fun afterTextChanged(editable: Editable) {
      if (fleet == null || ignoreEdits) {
        return
      }
      if (editable.toString().isEmpty()) {
        // You've deleted the whole text. No biggie.
        return
      }
      val n: Int
      n = try {
        editable.toString().toInt()
      } catch (e: NumberFormatException) {
        // Invalid number format, just ignore for now.
        return
      }
      val leftCount: Int
      val rightCount: Int
      if (isLeft) {
        leftCount = n
        rightCount = Math.floor(fleet!!.num_ships.toDouble()).toInt() - leftCount
      } else {
        rightCount = n
        leftCount = Math.floor(fleet!!.num_ships.toDouble()).toInt() - rightCount
      }
      update(leftCount, rightCount)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

  }

  init {
    this.cancelCallback = Preconditions.checkNotNull(cancelCallback)
    View.inflate(context, R.layout.ctrl_fleet_split_bottom_pane, this)
    fleetDetails = findViewById(R.id.fleet)
    splitLeft = findViewById(R.id.split_left)
    splitRight = findViewById(R.id.split_right)
    splitRatio = findViewById(R.id.split_ratio)
    findViewById<View>(R.id.split_btn).setOnClickListener { view: View -> onSplitClick(view) }
    findViewById<View>(R.id.cancel_btn).setOnClickListener { view: View -> onCancelClick(view) }
    splitLeft.addTextChangedListener(SplitTextWatcher(true))
    splitRight.addTextChangedListener(SplitTextWatcher(false))
    splitRatio.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && !ignoreEdits && fleet != null) {
          update(progress, Math.floor(fleet!!.num_ships.toDouble()).toInt() - progress)
        }
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    })
  }
}