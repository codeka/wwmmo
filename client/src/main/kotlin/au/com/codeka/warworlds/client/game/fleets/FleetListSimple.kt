package au.com.codeka.warworlds.client.game.fleets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.build.BuildViewHelper.setDesignIcon
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper
import java.util.*
import kotlin.math.roundToInt

/**
 * Represents a simple list of fleets, shown inside a [LinearLayout].
 */
class FleetListSimple : LinearLayout {
  private var star: Star? = null
  private var fleets: MutableList<Fleet> = ArrayList()
  private var fleetSelectedHandler: FleetSelectedHandler? = null
  private var clickListener: OnClickListener? = null

  /** An interface you can implement to filter the list of fleets we display in the list.  */
  interface FleetFilter {
    fun showFleet(fleet: Fleet?): Boolean
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    orientation = VERTICAL
  }

  constructor(context: Context) : super(context) {
    orientation = VERTICAL
  }

  fun setFleetSelectedHandler(handler: FleetSelectedHandler?) {
    fleetSelectedHandler = handler
  }

  fun setStar(s: Star) {
    setStar(s, s.fleets, null)
  }

  fun setStar(s: Star, f: FleetFilter?) {
    setStar(s, s.fleets, f)
  }

  fun setStar(s: Star, f: List<Fleet>) {
    setStar(s, f, null)
  }

  private fun setStar(s: Star, f: List<Fleet>, filter: FleetFilter?) {
    star = s
    fleets.clear()
    if (star!!.fleets != null) {
      for (fleet in f) {
        if (fleet.state != Fleet.FLEET_STATE.MOVING && fleet.num_ships > 0.01f &&
            (filter == null || filter.showFleet(fleet))) {
          fleets.add(fleet)
        }
      }
    }
    refresh()
  }

  val numFleets: Int
    get() = fleets.size

  private fun refresh() {
    if (clickListener == null) {
      clickListener = OnClickListener { v: View ->
        val fleet = v.tag as Fleet
        if (fleetSelectedHandler != null) {
          fleetSelectedHandler!!.onFleetSelected(fleet)
        }
      }
    }
    removeAllViews()
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        ?: // TODO: huh?
        return
    for (fleet in fleets) {
      val rowView = getRowView(inflater, fleet)
      addView(rowView)
    }
  }

  private fun getRowView(inflater: LayoutInflater, fleet: Fleet): View {
    val view = inflater.inflate(R.layout.ctrl_fleet_list_simple_row, this, false)
    val design = DesignHelper.getDesign(fleet.design_type)
    val icon = view.findViewById<ImageView>(R.id.fleet_icon)
    val row1 = view.findViewById<TextView>(R.id.fleet_row1)
    val row2 = view.findViewById<TextView>(R.id.fleet_row2)
    val fuelLevel = view.findViewById<ProgressBar>(R.id.fuel_level)
    val maxFuel = (design.fuel_size!! * fleet.num_ships).toInt()
    if (fleet.fuel_amount!! >= maxFuel) {
      fuelLevel.visibility = View.GONE
    } else {
      fuelLevel.visibility = View.VISIBLE
      fuelLevel.max = maxFuel
      fuelLevel.progress = (fleet.fuel_amount!!).roundToInt()
    }
    setDesignIcon(design, icon)
    row1.text = FleetListHelper.getFleetName(fleet, design)
    row2.text = FleetListHelper.getFleetStance(fleet)
    view.setOnClickListener(clickListener)
    view.tag = fleet
    return view
  }

  interface FleetSelectedHandler {
    fun onFleetSelected(fleet: Fleet?)
  }

  companion object {
    private val log = Log("FleetListSimple")
  }
}