package au.com.codeka.warworlds.client.game.build

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.fromHtml
import au.com.codeka.warworlds.client.game.fleets.FleetListHelper
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.BuildHelper
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.google.common.base.Preconditions
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor") // Must be constructed in code.
class ShipsView(
    context: Context, star: Star, private val colony: Colony, buildLayout: BuildLayout)
  : ListView(context), TabContentView {
  companion object {
    private const val HEADING_TYPE = 0
    private const val EXISTING_SHIP_TYPE = 1
    private const val NEW_SHIP_TYPE = 2
  }

  private val shipListAdapter: ShipListAdapter

  override fun refresh(star: Star, colony: Colony) {
    val myEmpire = Preconditions.checkNotNull(EmpireManager.getMyEmpire())
    val fleets = ArrayList<Fleet>()
    for (fleet in star.fleets) {
      if (fleet.empire_id != null && myEmpire.id == fleet.empire_id) {
        fleets.add(fleet)
      }
    }
    val buildRequests = ArrayList<BuildRequest>()
    for (br in BuildHelper.getBuildRequests(star)) {
      val design = DesignHelper.getDesign(br.design_type)
      if (design.design_kind == Design.DesignKind.SHIP) {
        buildRequests.add(br)
      }
    }
    shipListAdapter.refresh(fleets, buildRequests)
  }

  /** This adapter is used to populate the list of ship designs in our view.  */
  private inner class ShipListAdapter : BaseAdapter() {
    private var entries: MutableList<ItemEntry> = ArrayList()

    fun refresh(fleets: ArrayList<Fleet>, buildRequests: ArrayList<BuildRequest>) {
      entries.clear()
      entries.add(ItemEntry("New Ships"))
      for (design in DesignHelper.getDesigns(Design.DesignKind.SHIP)) {
        entries.add(ItemEntry(design))
      }
      entries.add(ItemEntry("Existing Ships"))
      for (fleet in fleets) {
        if (fleet.state != Fleet.FLEET_STATE.IDLE) {
          continue
        }
        val entry = ItemEntry(fleet)
        for (buildRequest in buildRequests) {
          // if (buildRequest.getExistingFleetID() != null
          //      && ((int) buildRequest.getExistingFleetID()) == Integer.parseInt(fleet.getKey())) {
          //   entry.buildRequest = buildRequest;
          // }
        }
        entries.add(entry)
      }
      for (buildRequest in buildRequests) {
        //if (buildRequest.getExistingFleetID() != null) {
        //  continue;
        // }
        entries.add(ItemEntry(buildRequest))
      }
      notifyDataSetChanged()
    }

    /**
     * We have three types of items, the "headings", the list of existing buildings and the list
     * of building designs.
     */
    override fun getViewTypeCount(): Int {
      return 3
    }

    override fun isEnabled(position: Int): Boolean {
      return getItemViewType(position) != Companion.HEADING_TYPE
    }

    override fun getCount(): Int {
      return entries.size
    }

    override fun getItemViewType(position: Int): Int {
      if (entries[position].heading != null) return Companion.HEADING_TYPE
      return (
        if (entries[position].design != null) Companion.NEW_SHIP_TYPE
        else Companion.EXISTING_SHIP_TYPE)
    }

    override fun getItem(position: Int): Any {
      return entries[position]
    }

    override fun getItemId(position: Int): Long {
      return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val entry = entries[position]
      val view = convertView ?:
          (if (entry.heading != null) TextView(context)
          else (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.ctrl_build_design, parent, false))

      if (entry.heading != null) {
        val tv = view as TextView
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.text = entry.heading
      } else if (entry.fleet != null || entry.buildRequest != null) {
        // existing fleet/upgrading fleet
        val icon = view.findViewById<ImageView>(R.id.building_icon)
        val row1 = view.findViewById<TextView>(R.id.design_row1)
        val row2 = view.findViewById<TextView>(R.id.design_row2)
        val row3 = view.findViewById<TextView>(R.id.design_row3)
        val level = view.findViewById<TextView>(R.id.build_level)
        val levelLabel = view.findViewById<TextView>(R.id.build_level_label)
        val progress = view.findViewById<ProgressBar>(R.id.build_progress)
        val notes = view.findViewById<TextView>(R.id.notes)
        val fleet = entry.fleet
        val buildRequest = entry.buildRequest
        val design = DesignHelper.getDesign(fleet?.design_type ?: buildRequest?.design_type!!)
        BuildViewHelper.setDesignIcon(design, icon)
        val numUpgrades = design.upgrades.size
        if (numUpgrades == 0 || fleet == null) {
          level.visibility = View.GONE
          levelLabel.visibility = View.GONE
        } else {
          // TODO
          level.text = "?"
          level.visibility = View.VISIBLE
          levelLabel.visibility = View.VISIBLE
        }
        if (fleet == null) {
          row1.text = FleetListHelper.getFleetName(buildRequest, design)
        } else {
          row1.text = FleetListHelper.getFleetName(fleet, design)
        }
        if (buildRequest != null) {
          val verb = if (fleet == null) "Building" else "Upgrading"
          row2.text = fromHtml(String.format(Locale.ENGLISH,
              "<font color=\"#0c6476\">%s:</font> %d %%, %s left", verb,
            (buildRequest.progress!! * 100.0f).roundToInt(),
              BuildHelper.formatTimeRemaining(buildRequest)))
          row3.visibility = View.GONE
          progress.visibility = View.VISIBLE
          progress.progress = (buildRequest.progress!! * 100.0f).roundToInt()
        } else {
          val upgrades = ""
          for (upgrade in design.upgrades) {
            //if (fleet != null && !fleet.hasUpgrade(upgrade.getID())) {
            //  if (upgrades.length() > 0) {
            //    upgrades += ", ";
            //  }
            //  upgrades += upgrade.getDisplayName();
            //}
          }
          progress.visibility = View.GONE
          if (upgrades.isEmpty()) {
            row2.text = fromHtml("Upgrades: <i>none</i>")
          } else {
            row2.text = String.format(Locale.US, "Upgrades: %s", upgrades)
          }
          val requiredHtml = DesignHelper.getDependenciesHtml(colony, design)
          row3.visibility = View.VISIBLE
          row3.text = fromHtml(requiredHtml)
        }
        if (fleet?.notes != null) {
          notes.text = fleet.notes
          notes.visibility = View.VISIBLE
          //} else if (buildRequest != null && buildRequest.getNotes() != null) {
          //  notes.setText(buildRequest.getNotes());
          //  notes.setVisibility(View.VISIBLE);
        } else {
          notes.text = ""
          notes.visibility = View.GONE
        }
      } else {
        // new fleet
        val icon = view.findViewById<ImageView>(R.id.building_icon)
        val row1 = view.findViewById<TextView>(R.id.design_row1)
        val row2 = view.findViewById<TextView>(R.id.design_row2)
        val row3 = view.findViewById<TextView>(R.id.design_row3)
        view.findViewById<View>(R.id.build_progress).visibility = View.GONE
        view.findViewById<View>(R.id.build_level).visibility = View.GONE
        view.findViewById<View>(R.id.build_level_label).visibility = View.GONE
        view.findViewById<View>(R.id.notes).visibility = View.GONE
        val design = entry.design
        BuildViewHelper.setDesignIcon(design!!, icon)
        row1.text = FleetListHelper.getFleetName(null as Fleet?, design)
        val requiredHtml = DesignHelper.getDependenciesHtml(colony, design)
        row2.text = fromHtml(requiredHtml)
        row3.visibility = View.GONE
      }
      return view
    }
  }

  class ItemEntry {
    var design: Design? = null
    var fleet: Fleet? = null
    var buildRequest: BuildRequest? = null
    var heading: String? = null

    internal constructor(design: Design?) {
      this.design = design
    }

    internal constructor(buildRequest: BuildRequest?) {
      this.buildRequest = buildRequest
    }

    internal constructor(fleet: Fleet?) {
      this.fleet = fleet
    }

    internal constructor(heading: String?) {
      this.heading = heading
    }
  }

  init {
    shipListAdapter = ShipListAdapter()
    refresh(star, colony)
    adapter = shipListAdapter
    onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
      val entry = shipListAdapter.getItem(position) as ItemEntry
      if (entry.fleet == null && entry.buildRequest == null) {
        // It's a new fleet
        buildLayout.showBuildSheet(entry.design)
      } else if (entry.fleet != null && entry.buildRequest == null) {
        // TODO: upgrade
        buildLayout.showBuildSheet(entry.design)
      } else {
        buildLayout.showProgressSheet(entry.buildRequest!!)
      }
    }
    onItemLongClickListener = OnItemLongClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
     // val entry = shipListAdapter.getItem(position) as ItemEntry
      true
    }
  }
}