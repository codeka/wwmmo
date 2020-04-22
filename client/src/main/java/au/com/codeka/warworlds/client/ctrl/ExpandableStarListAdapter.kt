package au.com.codeka.warworlds.client.ctrl

import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.StarCollection
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.proto.Star

/**
 * Base adapter class for adapter which show an expandable list of stars which you can expand and
 * collapse and show other things inside of.
 *
 * We assume the children we care about are all of type `T`.
 */
abstract class ExpandableStarListAdapter<T>(private val stars: StarCollection):
    BaseExpandableListAdapter() {

  /**
   * Destroy this [ExpandableStarListAdapter]. Must be called to unregister us from the event
   * bus when you're finished.
   */
  fun destroy() {
    App.i.eventBus.unregister(eventListener)
  }

  override fun hasStableIds(): Boolean {
    return true
  }

  override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
    return true
  }

  override fun getGroupCount(): Int {
    return stars.size()
  }

  override fun getGroup(groupPosition: Int): Star {
    return getStar(groupPosition)
  }

  protected abstract fun getNumChildren(star: Star?): Int
  protected abstract fun getChild(star: Star?, index: Int): T
  protected abstract fun getStarView(star: Star?, convertView: View?, parent: ViewGroup?): View
  protected abstract fun getChildView(
      star: Star?, index: Int, convertView: View?, parent: ViewGroup?): View

  protected abstract fun getChildId(star: Star?, childPosition: Int): Long
  override fun getChildrenCount(groupPosition: Int): Int {
    val star = getStar(groupPosition)
    return getNumChildren(star)
  }

  override fun getChild(groupPosition: Int, childPosition: Int): T {
    val star = getStar(groupPosition)
    return getChild(star, childPosition)
  }

  override fun getGroupId(groupPosition: Int): Long {
    return stars[groupPosition].id
  }

  override fun getChildId(groupPosition: Int, childPosition: Int): Long {
    return getChildId(stars[groupPosition], childPosition)
  }

  override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View,
                            parent: ViewGroup): View {
    val star = getStar(groupPosition)
    return getStarView(star, convertView, parent)
  }

  override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean,
                            convertView: View, parent: ViewGroup): View {
    val star = getStar(groupPosition)
    return getChildView(star, childPosition, convertView, parent)
  }

  protected fun getStar(index: Int): Star {
    return stars[index]
  }

  private val eventListener: Any = object : Any() {
    @EventHandler(thread = Threads.UI)
    fun onStarUpdated(star: Star?) {
      if (stars.notifyStarModified(star)) {
        notifyDataSetChanged()
      }
    }
  }

  init {
    App.i.eventBus.register(eventListener)
  }
}