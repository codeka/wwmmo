package au.com.codeka.warworlds.client.game.starsearch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.RelativeLayout
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.StarManager
import au.com.codeka.warworlds.common.proto.Star

class StarSearchLayout(context: Context, callback: Callback) : RelativeLayout(context) {
  interface Callback {
    fun onStarClick(star: Star?)
  }

  private val adapter: StarSearchListAdapter

  init {
    View.inflate(context, R.layout.starsearch, this)
    setBackgroundColor(context.resources.getColor(R.color.default_background))
    val lv = findViewById<ListView>(R.id.search_result)
    adapter = StarSearchListAdapter(LayoutInflater.from(context))
    lv.adapter = adapter
    adapter.setCursor(StarManager.myStars)
    lv.onItemClickListener = OnItemClickListener { adapterView: AdapterView<*>?, view: View?, position: Int, l: Long ->
      val star = adapter.getStar(position)
      callback.onStarClick(star)
    }
  }
}