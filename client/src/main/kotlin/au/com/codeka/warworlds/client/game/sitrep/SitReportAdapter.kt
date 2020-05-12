package au.com.codeka.warworlds.client.game.sitrep

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.build.BuildViewHelper
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.SituationReport
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class SitReportAdapter(private val layoutInflater: LayoutInflater)
  : RecyclerView.Adapter<SitReportAdapter.SitReportViewHolder>() {
  private val rows = ArrayList<RowData>()
  private val starRowMap = HashMap<Long, Set<Int>>()

  companion object {
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH)
    val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm\na", Locale.ENGLISH)
  }

  fun refresh(sitReports: List<SituationReport>) {
    Threads.checkOnThread(Threads.UI)

    rows.clear()
    starRowMap.clear()
    var lastDate: LocalDate? = null
    for ((i, sitReport) in sitReports.withIndex()) {
      val date =
          LocalDateTime.ofEpochSecond(sitReport.report_time / 1000, 0, ZoneOffset.UTC).toLocalDate()
      if (lastDate == null || lastDate != date) {
        rows.add(RowData(date, null))
        lastDate = date
      }

      rows.add(RowData(null, sitReport))

      val positions = starRowMap[sitReport.star_id] ?: HashSet()
      starRowMap[sitReport.star_id] = positions
      positions.plus(i)
    }

    notifyDataSetChanged()
  }

  fun onStarUpdated(star: Star) {
    val positions = starRowMap[star.id]
    if (positions != null) {
      for (position in positions) {
        notifyItemChanged(position)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SitReportViewHolder {
    val view = layoutInflater.inflate(viewType, parent, false)
    return SitReportViewHolder(viewType, view)
  }

  override fun getItemCount(): Int {
    return rows.size
  }

  override fun getItemViewType(position: Int): Int {
    return when {
      rows[position].date != null -> R.layout.sitreport_row_day
      else -> R.layout.sitreport_row
    }
  }

  override fun onBindViewHolder(holder: SitReportViewHolder, position: Int) {
    holder.bind(rows[position])
  }

  class SitReportViewHolder(viewType: Int, itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val dateViewBinding: DateViewBinding?
    private val sitReportViewBinding: SitReportViewBinding?

    init {
      if (viewType == R.layout.sitreport_row_day) {
        dateViewBinding = DateViewBinding(itemView)
        sitReportViewBinding = null
      } else {
        dateViewBinding = null
        sitReportViewBinding = SitReportViewBinding(itemView)
      }
    }

    fun bind(row: RowData) {
      when {
        row.date != null -> dateViewBinding!!.bind(row.date)
        row.sitReport != null -> sitReportViewBinding!!.bind(row.sitReport)
      }
    }
  }

  class DateViewBinding(view: View) {
    private val dateView: TextView = view as TextView

    fun bind(date: LocalDate) {
      // TODO: special-case TODAY and YESTERDAY
      dateView.text = date.format(dateFormat)
    }
  }

  class SitReportViewBinding(view: View) {
    private val timeView: TextView = view.findViewById(R.id.time)
    private val starIconView: ImageView = view.findViewById(R.id.star_icon)
    private val designIconView: ImageView = view.findViewById(R.id.design_icon)
    private val reportTitleView: TextView = view.findViewById(R.id.report_title)
    private val reportDetailsView: TextView = view.findViewById(R.id.report_details)

    fun bind(sitReport: SituationReport) {
      val res = timeView.context.resources

      val reportTime =
          LocalDateTime.ofEpochSecond(sitReport.report_time / 1000, 0, ZoneOffset.UTC).toLocalTime()
      timeView.text = reportTime.format(timeFormat)

      val star = StarManager.getStar(sitReport.star_id)
      if (star != null) {
        ImageHelper.bindStarIcon(starIconView, star)
      }

      val design: Design?
      when {
        sitReport.build_complete_record != null -> {
          design = DesignHelper.getDesign(sitReport.build_complete_record.design_type)
          reportTitleView.text =
              Html.fromHtml(res.getString(R.string.build_complete, star?.name ?: "..."))

          if (design.design_kind == Design.DesignKind.SHIP) {
            // TODO: handle was_destroyed when we have it.
            reportDetailsView.text =
                res.getString(
                    R.string.fleet_details_not_destroyed,
                    sitReport.build_complete_record.count.toFloat(),
                    design.display_name)
          } else {
            reportDetailsView.text = res.getString(R.string.build_details, design.display_name)
          }
        }
        sitReport.move_complete_record != null -> {
          design = DesignHelper.getDesign(sitReport.move_complete_record.design_type)
          reportTitleView.text =
              Html.fromHtml(res.getString(R.string.fleet_move_complete, star?.name ?: "..."))

          val resId = if (design.design_kind == Design.DesignKind.SHIP
              && sitReport.move_complete_record.was_destroyed == true) {
            R.string.fleet_details_destroyed
          } else /* if (design.design_kind == Design.DesignKind.SHIP) */ {
            R.string.fleet_details_not_destroyed
          }
          reportDetailsView.text =
              res.getString(resId, sitReport.move_complete_record.num_ships, design.display_name)
        }
        else -> {
          design = null
          reportTitleView.text =
              Html.fromHtml(res.getString(R.string.attack_on_star, star?.name ?: "..."))
        }
      }
      if (design != null) {
        BuildViewHelper.setDesignIcon(design, designIconView)
      }
    }
  }

  data class RowData(val date: LocalDate?, val sitReport: SituationReport?)
}
