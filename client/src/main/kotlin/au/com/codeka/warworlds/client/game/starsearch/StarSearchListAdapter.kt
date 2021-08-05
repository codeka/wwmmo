package au.com.codeka.warworlds.client.game.starsearch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.store.StarCursor
import au.com.codeka.warworlds.common.proto.EmpireStorage
import au.com.codeka.warworlds.common.proto.Star
import com.google.common.base.Preconditions
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A list adapter for showing a list of stars.
 */
internal class StarSearchListAdapter(private val inflater: LayoutInflater) : BaseAdapter() {
  private var cursor: StarCursor? = null
  private val recentStars: List<Star>

  /** Sets the [StarCursor] that we'll use to display stars.  */
  fun setCursor(cursor: StarCursor) {
    this.cursor = Preconditions.checkNotNull(cursor)
    notifyDataSetChanged()
  }

  override fun getViewTypeCount(): Int {
    return NUM_VIEW_TYPES
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == recentStars.size - 1) {
      VIEW_TYPE_SEPARATOR
    } else {
      VIEW_TYPE_STAR
    }
  }

  override fun getCount(): Int {
    var count = recentStars.size - 1
    if (cursor != null) {
      count += cursor!!.size + 1 // +1 for the spacer view
    }
    return count
  }

  override fun getItem(position: Int): Any {
    return getStar(position)!!
  }

  fun getStar(position: Int): Star? {
    return if (position < recentStars.size - 1) {
      recentStars[position + 1]
    } else if (position == recentStars.size - 1) {
      null
    } else {
      cursor!!.getValue(position - recentStars.size)
    }
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView ?: {
      if (position == recentStars.size - 1) {
        // it's just a spacer
        val view = View(inflater.context)
        view.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20)
        view
      } else {
        inflater.inflate(R.layout.solarsystem_starlist_row, parent, false)
      }
    }()
    if (position == recentStars.size - 1) {
      return view
    }
    val star = getItem(position) as Star
    val starIcon = view.findViewById<ImageView>(R.id.star_icon)
    val starName = view.findViewById<TextView>(R.id.star_name)
    val starType = view.findViewById<TextView>(R.id.star_type)
    val starGoodsDelta = view.findViewById<TextView>(R.id.star_goods_delta)
    val starGoodsTotal = view.findViewById<TextView>(R.id.star_goods_total)
    val starMineralsDelta = view.findViewById<TextView>(R.id.star_minerals_delta)
    val starMineralsTotal = view.findViewById<TextView>(R.id.star_minerals_total)
    if (starIcon == null) {
      throw RuntimeException(Integer.toString(position) + " " + view.toString())
    }
    if (star == null) {
      starIcon.setImageBitmap(null)
      starName.text = ""
      starType.text = ""
      starGoodsDelta.text = ""
      starGoodsTotal.text = "???"
      starMineralsDelta.text = ""
      starMineralsTotal.text = "???"
    } else {
      Picasso.get()
          .load(ImageHelper.getStarImageUrl(inflater.context, star, 36, 36))
          .into(starIcon)
      starName.text = star.name
      starType.text = star.classification.toString()
      val myEmpire = EmpireManager.getMyEmpire()
      var storage: EmpireStorage? = null
      for (i in star.empire_stores.indices) {
        if (star.empire_stores[i].empire_id != null
            && star.empire_stores[i].empire_id == myEmpire.id) {
          storage = star.empire_stores[i]
          break
        }
      }
      if (storage == null) {
        starGoodsDelta.text = ""
        starGoodsTotal.text = ""
        starMineralsDelta.text = ""
        starMineralsTotal.text = ""
      } else {
        val deltaGoodsPerHour = storage.goods_delta_per_hour!!
        val deltaMineralsPerHour = storage.minerals_delta_per_hour!!
        starGoodsDelta.text = String.format(Locale.ENGLISH, "%s%d/hr",
            if (deltaGoodsPerHour < 0) "-" else "+", abs(deltaGoodsPerHour.roundToInt()))
        if (deltaGoodsPerHour < 0) {
          starGoodsDelta.setTextColor(Color.RED)
        } else {
          starGoodsDelta.setTextColor(Color.GREEN)
        }
        starGoodsTotal.text = String.format(Locale.ENGLISH, "%d / %d",
          storage.total_goods!!.roundToInt(), storage.max_goods!!.roundToInt())
        starMineralsDelta.text = String.format(Locale.ENGLISH, "%s%d/hr",
            if (deltaMineralsPerHour < 0) "-" else "+", abs(deltaMineralsPerHour.roundToInt())
        )
        if (deltaMineralsPerHour < 0) {
          starMineralsDelta.setTextColor(Color.RED)
        } else {
          starMineralsDelta.setTextColor(Color.GREEN)
        }
        starMineralsTotal.text = String.format(Locale.ENGLISH, "%d / %d",
          storage.total_minerals!!.roundToInt(), storage.max_minerals!!.roundToInt())
      }
    }
    return view
  }

  companion object {
    private const val VIEW_TYPE_STAR = 0
    private const val VIEW_TYPE_SEPARATOR = 1
    private const val NUM_VIEW_TYPES = 2
  }

  init {
    recentStars = StarRecentHistoryManager.recentStars
  }
}