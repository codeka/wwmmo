package au.com.codeka.warworlds.client.game.sitrep

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.common.proto.SituationReport
import au.com.codeka.warworlds.common.proto.Star
import com.google.common.base.Preconditions

class SitReportLayout(context: Context, callback: Callback) : RelativeLayout(context) {
  interface Callback {
    fun onStarClick(star: Star?)
  }

  private val adapter: SitReportAdapter

  init {
    View.inflate(context, R.layout.sitreport, this)
    // TODO: actually do something with this.
    Preconditions.checkNotNull(callback)

    @Suppress("deprecation") // We need to support API level 21
    setBackgroundColor(context.resources.getColor(R.color.default_background))
    val rv = findViewById<RecyclerView>(R.id.sit_reports)
    adapter = SitReportAdapter(LayoutInflater.from(context))
    rv.adapter = adapter
    rv.layoutManager = LinearLayoutManager(context)
  }

  fun refresh(sitReports: List<SituationReport>) {
    adapter.refresh(sitReports)
  }

  fun onStarUpdated(star: Star) {
    adapter.onStarUpdated(star)
  }
}
