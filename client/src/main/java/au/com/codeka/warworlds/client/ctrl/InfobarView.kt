package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.net.ServerPacketEvent
import au.com.codeka.warworlds.client.net.ServerStateEvent
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.proto.Empire

/**
 * The [InfobarView] shows some relevant information like your current empire and some info about
 * network requests and so on.
 */
class InfobarView(context: Context?, attrs: AttributeSet?) : FrameLayout(context!!, attrs) {
  private val empireName: TextView
  private val working: ProgressBar
  private val disconnectedIcon: ImageView
  private var lastConnectionState: ServerStateEvent.ConnectionState? = null
  private var lastPacketTime: Long = 0
  fun hideEmpireName() {
    empireName.visibility = View.GONE
  }

  /** Must be run on the UI thread.  */
  private fun refresh() {
    if (isInEditMode) {
      return
    }
    if (lastConnectionState === ServerStateEvent.ConnectionState.DISCONNECTED) {
      disconnectedIcon.visibility = View.VISIBLE
    } else {
      disconnectedIcon.visibility = View.GONE
    }
    val now = System.currentTimeMillis()
    if (now - lastPacketTime < PROGRESS_TIME_MS) {
      working.visibility = View.VISIBLE
    } else {
      working.visibility = View.GONE
    }
  }

  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (isInEditMode) {
      return
    }
    App.i.eventBus.register(eventHandler)
    refreshEmpire(EmpireManager.getMyEmpire())
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if (isInEditMode) {
      return
    }
    App.i.eventBus.unregister(eventHandler)
  }

  private fun refreshEmpire(empire: Empire) {
    empireName.text = empire.display_name

    // set up the initial state
    refresh()
  }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onEmpireUpdated(empire: Empire) {
      if (empire.id != null && empire.id == EmpireManager.getMyEmpire().id) {
        refreshEmpire(empire)
      }
    }

    @EventHandler(thread = Threads.UI)
    fun onPacket(event: ServerPacketEvent?) {
      lastPacketTime = System.currentTimeMillis()
      refresh()
      handler.postDelayed({ refresh() }, PROGRESS_TIME_MS)
    }

    @EventHandler(thread = Threads.UI)
    fun onServerState(event: ServerStateEvent) {
      lastConnectionState = event.state
      refresh()
    }
  }

  companion object {
    /** Number of milliseconds to display the progress after receiving a packet.  */
    private const val PROGRESS_TIME_MS = 1000L
  }

  init {
    View.inflate(context, R.layout.ctrl_infobar_view, this)
    empireName = findViewById(R.id.empire_name)
    working = findViewById(R.id.working)
    disconnectedIcon = findViewById(R.id.disconnected_icon)
  }
}