package au.com.codeka.warworlds.client.ui

import android.content.Context
import android.view.View
import com.google.common.base.Preconditions
import java.util.*

/**
 * Container class for holding shared views that we want to animate specially between screen
 * transitions.
 */
class SharedViews private constructor(sharedViews: ArrayList<SharedView>) {
  val sharedViews: ArrayList<SharedView>

  fun toDebugString(context: Context?): String {
    val sb = StringBuilder()
    for (i in sharedViews.indices) {
      if (i == 0) {
        sb.append("[")
      } else {
        sb.append(", ")
      }
      sb.append(sharedViews[i].toDebugString(context))
    }
    sb.append("]")
    return sb.toString()
  }

  class SharedView {
    val viewId: Int
    val fromView: View?
    val fromViewId: Int
    val toViewId: Int

    constructor(viewId: Int) {
      this.viewId = viewId
      fromView = null
      fromViewId = 0
      toViewId = 0
    }

    constructor(fromViewId: Int, toViewId: Int) {
      viewId = 0
      fromView = null
      this.fromViewId = fromViewId
      this.toViewId = toViewId
    }

    constructor(fromView: View?, toViewId: Int) {
      viewId = 0
      this.fromView = fromView
      fromViewId = 0
      this.toViewId = toViewId
    }

    fun toDebugString(context: Context?): String {
      return if (viewId != 0) {
        context!!.resources.getResourceName(viewId)
      } else {
        var str: String
        str = fromView?.toString() ?: context!!.resources.getResourceName(fromViewId)
        str += " -> "
        str += context!!.resources.getResourceName(toViewId)
        str
      }
    }
  }

  class Builder {
    private val sharedViews = ArrayList<SharedView>()
    fun addSharedView(viewId: Int): Builder {
      sharedViews.add(SharedView(viewId))
      return this
    }

    fun addSharedView(fromViewId: Int, toViewId: Int): Builder {
      sharedViews.add(SharedView(fromViewId, toViewId))
      return this
    }

    fun addSharedView(fromView: View?, toViewId: Int): Builder {
      sharedViews.add(SharedView(fromView, toViewId))
      return this
    }

    fun build(): SharedViews {
      return SharedViews(sharedViews)
    }
  }

  companion object {
    @JvmStatic
    fun builder(): Builder {
      return Builder()
    }
  }

  init {
    this.sharedViews = Preconditions.checkNotNull(sharedViews)
  }
}