package au.com.codeka.warworlds.client.game.starfield

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.PlanetListSimple
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.common.TimeFormatter
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.StarHelper
import com.squareup.picasso.Picasso
import java.util.*

class ScoutReportBottomPane(
    context: Context, private val star: Star?, private val callback: Callback)
  : RelativeLayout(context) {
  interface Callback {
    fun onBackClicked()
  }

  private val planetList: PlanetListSimple
  private val fleetList: FleetListSimple
  private val starName: TextView
  private val starKind: TextView
  private val starIcon: ImageView
  private val reportDate: TextView

  init {
    View.inflate(context, R.layout.starfield_bottom_pane_scout_report, this)
    planetList = findViewById(R.id.planet_list)
    fleetList = findViewById(R.id.fleet_list)
    starName = findViewById(R.id.star_name)
    starKind = findViewById(R.id.star_kind)
    starIcon = findViewById(R.id.star_icon)
    reportDate = findViewById(R.id.report_date)
    findViewById<View>(R.id.back_btn).setOnClickListener { callback.onBackClicked() }
    if (!isInEditMode) {
      refresh()
    }
  }

  fun refresh() {
    if (star!!.scout_reports.size != 1) {
      // This is an error!
      return
    }
    val scoutReport = star.scout_reports[0]
    fleetList.setStar(star, scoutReport.fleets)
    planetList.setStar(star, scoutReport.planets, scoutReport.fleets)
    reportDate.text = TimeFormatter.create()
        .withTimeInPast(true)
        .format(scoutReport.report_time - System.currentTimeMillis())
    starName.text = star.name
    starKind.text = String.format(Locale.ENGLISH, "%s %s", star.classification,
        StarHelper.getCoordinateString(star))
    Picasso.get()
        .load(ImageHelper.getStarImageUrl(context, star, 40, 40))
        .into(starIcon)
  }
}