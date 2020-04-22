package au.com.codeka.warworlds.client.ui

import android.view.View

class ShowInfo private constructor(builder: Builder) {
  val view: View?
  val toolbarVisible: Boolean

  class Builder {
    var view: View? = null
    var toolbarVisible = true
    fun view(view: View?): Builder {
      this.view = view
      return this
    }

    fun toolbarVisible(toolbarVisible: Boolean): Builder {
      this.toolbarVisible = toolbarVisible
      return this
    }

    fun build(): ShowInfo {
      return ShowInfo(this)
    }
  }

  companion object {
    @JvmStatic
    fun builder(): Builder {
      return Builder()
    }
  }

  init {
    view = builder.view
    toolbarVisible = builder.toolbarVisible
  }
}