package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Planet
import au.com.codeka.warworlds.common.proto.Star
import com.squareup.picasso.Picasso
import java.util.*

/**
 * The planet list that shows up on the starfield view. It also includes details about empires
 * around the star.
 */
class PlanetListSimple : LinearLayout {
  private lateinit var star: Star
  private var planets: List<Planet>? = null
  private var fleets: List<Fleet>? = null
  private var planetSelectedHandler: PlanetSelectedHandler? = null
  private var clickListener: OnClickListener? = null

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    orientation = VERTICAL
  }

  constructor(context: Context) : super(context) {
    orientation = VERTICAL
  }

  fun setPlanetSelectedHandler(handler: PlanetSelectedHandler?) {
    planetSelectedHandler = handler
  }

  fun setStar(s: Star) {
    star = s
    planets = ArrayList(s.planets)
    fleets = ArrayList(s.fleets)
    refresh()
  }

  fun setStar(s: Star, p: List<Planet>?, f: List<Fleet>?) {
    star = s
    planets = p
    fleets = f
    refresh()
  }

  private fun refresh() {
    if (clickListener == null) {
      clickListener = OnClickListener { v: View ->
        val planet = v.tag as Planet
        if (planetSelectedHandler != null) {
          planetSelectedHandler!!.onPlanetSelected(planet)
        }
      }
    }
    removeAllViews()
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val myEmpire = EmpireManager.getMyEmpire()
    val empires = HashSet<Long>()
    for (fleet in fleets!!) {
      val empireId = fleet.empire_id ?: continue
      if (empireId == myEmpire.id) {
        continue
      }
      empires.add(empireId)
    }
    for (planet in planets!!) {
      val colony = planet.colony ?: continue
      val empireId = colony.empire_id ?: continue
      if (empireId == myEmpire.id) {
        continue
      }
      empires.add(empireId)
    }
    for (empireID in empires) {
      val rowView = getEmpireRowView(inflater, empireID)
      addView(rowView)
    }
    if (!empires.isEmpty()) {
      // add a spacer...
      var spacer = View(context)
      spacer.layoutParams = LayoutParams(10, 10)
      addView(spacer)
      spacer = View(context)
      spacer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1)
      spacer.setBackgroundColor(0x33ffffff)
      addView(spacer)
    }
    for (i in planets!!.indices) {
      val rowView = getPlanetRowView(inflater, planets!![i], i)
      addView(rowView)
    }
  }

  private fun getPlanetRowView(inflater: LayoutInflater, planet: Planet, planetIndex: Int): View {
    val view = inflater.inflate(R.layout.ctrl_planet_list_simple_row, this, false)
    val icon = view.findViewById<View>(R.id.starfield_planet_icon) as ImageView
    Picasso.get()
        .load(ImageHelper.getPlanetImageUrl(context, star, planetIndex, 32, 32))
        .into(icon)
    val planetTypeTextView = view.findViewById<View>(R.id.starfield_planet_type) as TextView
    planetTypeTextView.text = planet.planet_type.toString()
    val colony = planet.colony
    val colonyTextView = view.findViewById<View>(R.id.starfield_planet_colony) as TextView
    if (colony != null) {
      if (colony.empire_id == null) {
        colonyTextView.text = getContext().getString(R.string.native_colony)
      } else {
        val empire = EmpireManager.getEmpire(colony.empire_id)
        if (empire != null) {
          colonyTextView.text = empire.display_name
        } else {
          colonyTextView.text = context.getString(R.string.colonized)
          // TODO: update when the empire comes around.
        }
      }
    } else {
      colonyTextView.text = ""
    }
    view.setOnClickListener(clickListener)
    view.tag = planet
    return view
  }

  private fun getEmpireRowView(inflater: LayoutInflater, empireID: Long): View {
    val view = inflater.inflate(R.layout.ctrl_planet_list_simple_row, this, false)
    val icon = view.findViewById<ImageView>(R.id.starfield_planet_icon)
    val empireName = view.findViewById<TextView>(R.id.starfield_planet_type)
    val allianceName = view.findViewById<TextView>(R.id.starfield_planet_colony)
    val empire = EmpireManager.getEmpire(empireID)
    if (empire != null) {
      Picasso.get()
          .load(ImageHelper.getEmpireImageUrl(context, empire, 32, 32))
          .into(icon)
      empireName.text = empire.display_name
      //      if (empire.alliance != null) {
//        allianceName.setText(empire.alliance.name);
//      }
    }
    view.setOnClickListener(clickListener)
    view.tag = empireID
    return view
  }

  interface PlanetSelectedHandler {
    fun onPlanetSelected(planet: Planet?)
  }
}