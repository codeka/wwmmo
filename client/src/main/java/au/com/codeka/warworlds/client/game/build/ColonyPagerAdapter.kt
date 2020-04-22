package au.com.codeka.warworlds.client.game.build

import android.content.Context
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter.format
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Star

/**
 * A [PagerAdapter] used by the [BuildLayout] to allow you to quickly swipe between your
 * colonies.
 */
class ColonyPagerAdapter(
    private val context: Context,
    private var star: Star?,
    private var colonies: List<Colony?>?,
    private val buildLayout: BuildLayout) : PagerAdapter() {
  private val views = SparseArray<ColonyView>()
  fun refresh(star: Star?, colonies: List<Colony?>?) {
    this.star = star

    // TODO: what if the current list of colonies has changed?
    this.colonies = colonies
    notifyDataSetChanged()
    for (i in 0 until views.size()) {
      val position = views.keyAt(i)
      val view = views.valueAt(i)
      view.refresh(star, colonies!![position])
    }
  }

  /**
   * Gets the [ColonyView] at the specified position (or null if it hasn't been created
   * yet).
   */
  fun getView(position: Int): ColonyView? {
    return views[position]
  }

  override fun instantiateItem(parent: ViewGroup, position: Int): Any {
    log.info("instantiating item %d", position)
    val colony = colonies!![position]
    val view = ColonyView(context, star, colony, buildLayout)
    view.layoutParams = ViewPager.LayoutParams()
    parent.addView(view)
    views.put(position, view)
    return view
  }

  override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
    views.remove(position)
    collection.removeView(view as View)
  }

  override fun getCount(): Int {
    return colonies!!.size
  }

  override fun isViewFromObject(view: View, `object`: Any): Boolean {
    return view === `object`
  }

  override fun getPageTitle(position: Int): CharSequence? {
    var planetIndex = 0
    for (planet in star!!.planets) {
      if (planet.colony != null && planet.colony.id == colonies!![position]!!.id) {
        planetIndex = planet.index
      }
    }
    return String.format("%s %s", star!!.name, format(planetIndex + 1))
  }

  companion object {
    private val log = Log("ColonyPagerAdapter")
  }

}