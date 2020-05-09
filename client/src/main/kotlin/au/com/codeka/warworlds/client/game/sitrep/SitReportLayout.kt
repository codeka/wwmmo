package au.com.codeka.warworlds.client.game.sitrep

import android.content.Context
import android.view.View
import android.widget.ListView
import android.widget.RelativeLayout
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.common.proto.Star

class SitReportLayout(context: Context, callback: Callback) : RelativeLayout(context) {
  interface Callback {
    fun onStarClick(star: Star?)
  }

  init {
    View.inflate(context, R.layout.sitreport, this)
    setBackgroundColor(context.resources.getColor(R.color.default_background))
    val lv = findViewById<ListView>(R.id.search_result)

    /*
    adapter = StarSearchListAdapter(LayoutInflater.from(context))
    lv.adapter = adapter
    adapter.setCursor(StarManager.myStars)
    lv.onItemClickListener = AdapterView.OnItemClickListener {
      _: AdapterView<*>?, _: View?, position: Int, _: Long ->
        val star = adapter.getStar(position)
        callback.onStarClick(star)
      }
    */
  }
}
