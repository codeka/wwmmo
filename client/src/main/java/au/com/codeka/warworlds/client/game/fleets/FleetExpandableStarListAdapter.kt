package au.com.codeka.warworlds.client.game.fleets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.ExpandableStarListAdapter
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarCollection
import au.com.codeka.warworlds.client.util.NumberFormatter.format
import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.google.common.base.Preconditions
import com.google.common.base.Predicate
import com.squareup.picasso.Picasso
import java.lang.RuntimeException
import java.util.*

/**
 * Represents an [ExpandableStarListAdapter] which shows a list of fleets under each star.
 */
class FleetExpandableStarListAdapter(private val inflater: LayoutInflater, stars: StarCollection?)
  : ExpandableStarListAdapter<Fleet?>(stars!!) {
  private val myEmpireId: Long
  private var multiSelect = false
  var selectedFleetId: Long? = null
    private set

  /**
   * Returns the currently-selected star. This will be non-null if you have one or more fleets
   * selected under the given star. If you have no fleets selected, or you have fleets selected
   * under multiple stars, this will return null.
   *
   * @return The currently-selected [Star], or null.
   */
  var selectedStar: Star? = null
    private set

  /** A list of fleets that are currently selected, contains more than one in multi-select mode.  */
  private val selectedFleetIds: MutableSet<Long?> = HashSet()

  /** A list of fleets which are currently disabled (you can't select them).  */
  private val disabledFleetIds: MutableSet<Long> = HashSet()

  fun getSelectedFleetIds(): Collection<Long?> {
    return selectedFleetIds
  }

  /** Disable all fleets that match the given predicate.  */
  fun disableFleets(predicate: (Fleet) -> Boolean) {
    disabledFleetIds.clear()
    for (i in 0 until groupCount) {
      val star = getStar(i)
      for (fleet in star.fleets) {
        if (fleet.empire_id != null && fleet.empire_id == myEmpireId) {
          if (predicate(fleet)) {
            disabledFleetIds.add(fleet.id)
          }
        }
      }
    }
  }

  /** After disabling fleets, this will re-enable all fleets again.  */
  fun enableAllFleets() {
    disabledFleetIds.clear()
  }

  /**
   * Sets whether or not we'll allow multi-select.
   *
   *
   * When in multi-select mode, [.getSelectedFleetId] will return the first
   * selected fleet, and [.getSelectedFleetIds] will return all of them. When not in
   * multi-select mode, [.getSelectedFleetIds] will return a list of the one selected fleet.
   */
  fun setMultiSelect(multiSelect: Boolean) {
    this.multiSelect = multiSelect
    if (!multiSelect) {
      selectedFleetIds.clear()
      if (selectedFleetId != null) {
        selectedFleetIds.add(selectedFleetId)
      }
    }
  }

  fun setSelectedFleetId(star: Star?, fleetId: Long?) {
    selectedFleetId = fleetId
    selectedStar = star
    if (multiSelect) {
      selectedFleetIds.add(fleetId)
    } else {
      selectedFleetIds.clear()
      selectedFleetIds.add(fleetId)
    }
    notifyDataSetChanged()
  }

  fun onItemClick(groupPosition: Int, childPosition: Int) {
    val star = getGroup(groupPosition)
    val fleet = getChild(groupPosition, childPosition)
    selectedStar = if (selectedStar == null || selectedStar!!.id == star.id) {
      star
    } else {
      null
    }
    if (multiSelect) {
      if (!selectedFleetIds.remove(fleet!!.id)) {
        selectedFleetIds.add(fleet.id)
      }
    } else {
      selectedFleetId = fleet!!.id
      selectedFleetIds.clear()
      selectedFleetIds.add(fleet.id)
    }
    notifyDataSetChanged()
  }

  public override fun getNumChildren(star: Star?): Int {
    var numFleets = 0
    for (i in star!!.fleets.indices) {
      val empireID = star.fleets[i].empire_id
      if (empireID != null && empireID == myEmpireId) {
        numFleets++
      }
    }
    return numFleets
  }

  public override fun getChild(star: Star?, index: Int): Fleet {
    var fleetIndex = 0
    for (i in star!!.fleets.indices) {
      val empireID = star.fleets[i].empire_id
      if (empireID != null && empireID == myEmpireId) {
        if (fleetIndex == index) {
          return star.fleets[i]
        }
        fleetIndex++
      }
    }

    throw RuntimeException("TODO: shouldn't get here")
  }

  override fun getChildId(star: Star?, childPosition: Int): Long {
    return star!!.fleets[childPosition].id
  }

  public override fun getStarView(star: Star?, convertView: View?, parent: ViewGroup?): View {
    var view = convertView
    if (view == null) {
      view = inflater.inflate(R.layout.fleets_star_row, parent, false)
    }
    val starIcon = view!!.findViewById<ImageView>(R.id.star_icon)
    val starName = view.findViewById<TextView>(R.id.star_name)
    val starType = view.findViewById<TextView>(R.id.star_type)
    val fightersTotal = view.findViewById<TextView>(R.id.fighters_total)
    val nonFightersTotal = view.findViewById<TextView>(R.id.nonfighters_total)
    if (star == null) {
      starIcon.setImageBitmap(null)
      starName.text = ""
      starType.text = ""
      fightersTotal.text = "..."
      nonFightersTotal.text = "..."
    } else {
      Picasso.get()
          .load(ImageHelper.getStarImageUrl(view.context, star, 16, 16))
          .into(starIcon)
      starName.text = star.name
      starType.text = star.classification.toString()
      val myEmpire = Preconditions.checkNotNull(EmpireManager.i.myEmpire)
      var numFighters = 0.0f
      var numNonFighters = 0.0f
      for (fleet in star.fleets) {
        if (fleet.empire_id == null || fleet.empire_id != myEmpire.id) {
          continue
        }
        if (fleet.design_type == Design.DesignType.FIGHTER) {
          numFighters += fleet.num_ships
        } else {
          numNonFighters += fleet.num_ships
        }
      }
      fightersTotal.text = String.format(Locale.ENGLISH, "%s", format(numFighters))
      nonFightersTotal.text = String.format(Locale.ENGLISH, "%s", format(numNonFighters))
    }
    return view
  }

  public override fun getChildView(star: Star?, index: Int, convertView: View?, parent: ViewGroup?): View {
    var view = convertView
    if (view == null) {
      view = inflater.inflate(R.layout.fleets_fleet_row, parent, false)
    }
    if (star != null) {
      val fleet = getChild(star, index)
      if (fleet != null) {
        FleetListHelper.populateFleetRow(
            view as ViewGroup?, fleet, DesignHelper.getDesign(fleet.design_type))
        if (disabledFleetIds.contains(fleet.id)) {
          view!!.setBackgroundResource(R.color.list_item_disabled)
        } else if (selectedFleetIds.contains(fleet.id)) {
          view!!.setBackgroundResource(R.color.list_item_selected)
        } else {
          view!!.setBackgroundResource(android.R.color.transparent)
        }
      }
    }
    return view!!
  }

  init {
    myEmpireId = Preconditions.checkNotNull(EmpireManager.i.myEmpire).id
  }
}