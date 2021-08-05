package au.com.codeka.warworlds.client.game.build

import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.concurrency.Threads.Companion.checkOnThread
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.BuildHelper
import au.com.codeka.warworlds.common.sim.DesignHelper
import java.util.*
import kotlin.math.roundToInt

class BuildingsView(
    context: Context?, private var star: Star, private var colony: Colony,
    buildLayout: BuildLayout)
  : ListView(context), TabContentView {

  companion object {
    private const val HEADING_TYPE = 0
    private const val EXISTING_BUILDING_TYPE = 1
    private const val NEW_BUILDING_TYPE = 2
  }

  private val adapter: BuildingListAdapter

  override fun refresh(star: Star, colony: Colony) {
    this.star = star
    this.colony = colony
    adapter.refresh(star, colony)
  }

  /** This adapter is used to populate a list of buildings in a list view.  */
  private inner class BuildingListAdapter : BaseAdapter() {
    private var entries: ArrayList<ItemEntry>? = null

    fun refresh(star: Star, colony: Colony) {
      checkOnThread(Threads.UI)
      entries = ArrayList()
      var buildings = colony.buildings
      val existingBuildingEntries = ArrayList<ItemEntry>()
      for (b in buildings) {
        val entry = ItemEntry()
        entry.building = b
        // if the building is being upgraded (i.e. if there's a build request that
        // references this building) then add the build request as well
        for (br in BuildHelper.getBuildRequests(star)) {
          if (br.building_id != null && br.building_id == b.id) {
            entry.buildRequest = br
          }
        }
        existingBuildingEntries.add(entry)
      }
      for (br in colony.build_requests) {
        val design = DesignHelper.getDesign(br.design_type)
        if (design.design_kind == Design.DesignKind.BUILDING && br.building_id == null) {
          val entry = ItemEntry()
          entry.buildRequest = br
          existingBuildingEntries.add(entry)
        }
      }
      existingBuildingEntries.sortWith { lhs: ItemEntry, rhs: ItemEntry ->
        val leftBuilding = lhs.building
        val rightBuilding = rhs.building
        val a = leftBuilding?.design_type ?: lhs.buildRequest!!.design_type
        val b = rightBuilding?.design_type ?: rhs.buildRequest!!.design_type
        a.compareTo(b)
      }
      var title = ItemEntry()
      title.title = "New Buildings"
      entries!!.add(title)
      for (design in DesignHelper.getDesigns(Design.DesignKind.BUILDING)) {
        val maxPerColony = design.max_per_colony
        if (maxPerColony != null && maxPerColony > 0) {
          var numExisting = 0
          for (e in existingBuildingEntries) {
            if (e.building != null) {
              if (e.building!!.design_type == design.type) {
                numExisting++
              }
            } else if (e.buildRequest != null) {
              if (e.buildRequest!!.design_type == design.type) {
                numExisting++
              }
            }
          }
          if (numExisting >= maxPerColony) {
            continue
          }
        }
        // if (bd.getMaxPerEmpire() > 0) {
        //   int numExisting = BuildManager.i.getTotalBuildingsInEmpire(bd.getID());
        //   // If you're building one, we'll still think it's OK to build again, but it's
        //   // actually going to be blocked by the server.
        //   if (numExisting >= bd.getMaxPerEmpire()) {
        //     continue;
        //   }
        // }
        val entry = ItemEntry()
        entry.design = design
        entries!!.add(entry)
      }
      title = ItemEntry()
      title.title = "Existing Buildings"
      entries!!.add(title)
      entries!!.addAll(existingBuildingEntries)
      notifyDataSetChanged()
    }

    /**
     * We have three types of items, the "headings", the list of existing buildings and the list of
     * building designs.
     */
    override fun getViewTypeCount(): Int {
      return 3
    }

    override fun getItemViewType(position: Int): Int {
      if (entries == null) return 0
      if (entries!![position].title != null) return Companion.HEADING_TYPE
      return if (entries!![position].design != null) Companion.NEW_BUILDING_TYPE else Companion.EXISTING_BUILDING_TYPE
    }

    override fun isEnabled(position: Int): Boolean {
      if (position < 0 || position >= entries!!.size) {
        return false
      }
      if (getItemViewType(position) == Companion.HEADING_TYPE) {
        return false
      }

      // also, if it's an existing building that's at the max level it can't be
      // upgraded any more, so also disabled.
      val entry = entries!![position]
      if (entry.building != null) {
        val maxUpgrades = DesignHelper.getDesign(entry.building!!.design_type).upgrades.size
        if (entry.building!!.level > maxUpgrades) {
          return false
        }
      }
      return true
    }

    override fun getCount(): Int {
      return if (entries == null) 0 else entries!!.size
    }

    override fun getItem(position: Int): Any? {
      return if (entries == null) null else entries!![position]
    }

    override fun getItemId(position: Int): Long {
      return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val view = convertView ?: run {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val viewType = getItemViewType(position)
        if (viewType == HEADING_TYPE) {
          TextView(context)
        } else {
          inflater.inflate(R.layout.ctrl_build_design, parent, false)
        }
      }
      val entry = entries!![position]
      if (entry.title != null) {
        val tv = view as TextView
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.text = entry.title
      } else if (entry.building != null || entry.buildRequest != null) {
        // existing building/upgrading building
        val icon = view.findViewById<ImageView>(R.id.building_icon)
        val row1 = view.findViewById<TextView>(R.id.design_row1)
        val row2 = view.findViewById<TextView>(R.id.design_row2)
        val row3 = view.findViewById<TextView>(R.id.design_row3)
        val level = view.findViewById<TextView>(R.id.build_level)
        val levelLabel = view.findViewById<TextView>(R.id.build_level_label)
        val progress = view.findViewById<ProgressBar>(R.id.build_progress)
        val notes = view.findViewById<TextView>(R.id.notes)
        val building = entry.building
        val buildRequest = entry.buildRequest
        val design = DesignHelper.getDesign(
          building?.design_type ?: buildRequest!!.design_type
        )
        BuildViewHelper.setDesignIcon(design, icon)
        val numUpgrades = design.upgrades.size
        if (numUpgrades == 0 || building == null) {
          level.visibility = View.GONE
          levelLabel.visibility = View.GONE
        } else {
          level.text = String.format(Locale.US, "%d", building.level)
          level.visibility = View.VISIBLE
          levelLabel.visibility = View.VISIBLE
        }
        row1.text = design.display_name
        if (buildRequest != null) {
          val verb = if (building == null) "Building" else "Upgrading"
          row2.text = Html.fromHtml(String.format(Locale.ENGLISH,
              "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
              verb, (buildRequest.progress!! * 100.0f).roundToInt(),
              BuildHelper.formatTimeRemaining(buildRequest)))
          row3.visibility = View.GONE
          progress.visibility = View.VISIBLE
          progress.progress = (buildRequest.progress!! * 100.0f).roundToInt()
        } else  /*if (building != null)*/ {
          if (numUpgrades < building!!.level) {
            row2.text = context.getString(R.string.no_more_upgrades)
            row3.visibility = View.GONE
            progress.visibility = View.GONE
          } else {
            progress.visibility = View.GONE
            val requiredHtml = DesignHelper.getDependenciesHtml(colony, design, building.level + 1)
            row2.text = Html.fromHtml(requiredHtml)
            row3.visibility = View.GONE
          }
        }
        if (building?.notes != null) {
          notes.text = building.notes
          notes.visibility = View.VISIBLE
        } /*else if (buildRequest != null && buildRequest.notes != null) {
          notes.setText(buildRequest.getNotes());
          notes.setVisibility(View.VISIBLE);
        } */ else {
          notes.text = ""
          notes.visibility = View.GONE
        }
      } else {
        // new building
        val icon = view.findViewById<ImageView>(R.id.building_icon)
        val row1 = view.findViewById<TextView>(R.id.design_row1)
        val row2 = view.findViewById<TextView>(R.id.design_row2)
        val row3 = view.findViewById<TextView>(R.id.design_row3)
        view.findViewById<View>(R.id.build_progress).visibility = View.GONE
        view.findViewById<View>(R.id.build_level).visibility = View.GONE
        view.findViewById<View>(R.id.build_level_label).visibility = View.GONE
        view.findViewById<View>(R.id.notes).visibility = View.GONE
        val design = entries!![position].design
        BuildViewHelper.setDesignIcon(design!!, icon)
        row1.text = design.display_name
        val requiredHtml = DesignHelper.getDependenciesHtml(colony, design)
        row2.text = Html.fromHtml(requiredHtml)
        row3.visibility = View.GONE
      }
      return view
    }
  }

  internal class ItemEntry {
    var title: String? = null
    var buildRequest: BuildRequest? = null
    var building: Building? = null
    var design: Design? = null
  }

  init {
    adapter = BuildingListAdapter()
    adapter.refresh(star, colony)
    setAdapter(adapter)
    onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
      val entry = adapter.getItem(position) as ItemEntry
      if (entry.building == null && entry.buildRequest == null) {
        buildLayout.showBuildSheet(entry.design)
      } else if (entry.building != null && entry.buildRequest == null) {
        buildLayout.showUpgradeSheet(entry.building)
      } else {
        // entry.buildRequest should not be null here.
        buildLayout.showProgressSheet(null, entry.buildRequest!!)
      }
    }
  }
}