package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Debug
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.net.ServerPacketEvent
import au.com.codeka.warworlds.client.opengl.FrameCounter
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import com.google.common.base.Preconditions
import java.util.*

/**
 * This is a view that's displayed over the top of the activity and shows up a little bit of
 * debugging information.
 */
class DebugView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null):
    FrameLayout(context!!, attrs) {
  private val view: View
  private var isAttached = false
  private var frameCounter: FrameCounter? = null
  fun setFrameCounter(frameCounter: FrameCounter) {
    this.frameCounter = Preconditions.checkNotNull(frameCounter)
  }

  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!isInEditMode) {
      isAttached = true
      App.i.eventBus.register(eventListener)
      queueRefresh()
    }
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if (!isInEditMode) {
      isAttached = false
      App.i.eventBus.unregister(eventListener)
    }
  }

  private fun queueRefresh() {
    if (!isAttached) {
      return
    }
    handler!!.postDelayed({
      refresh()
      queueRefresh()
    }, 1000)
  }

  fun refresh() {
    val memoryGraph = view.findViewById<View>(R.id.memory_graph) as ImageView
    memoryGraph.setImageBitmap(createMemoryGraph(memoryGraph.width, memoryGraph.height))
    val messagesContainer = view.findViewById<View>(R.id.messages) as LinearLayout
    for (i in 0 until messagesContainer.childCount) {
      val tv = messagesContainer.getChildAt(i) as TextView
      if (messages.size > i) {
        tv.text = messages[i].toString()
      } else {
        tv.text = ""
      }
    }
    val old = SystemClock.elapsedRealtime() - 5000
    var i = 0
    while (i < messages.size) {
      if (messages[i].createTime < old) {
        messages.removeAt(i)
        i--
      }
      i++
    }
    val counter = frameCounter
    if (counter != null) {
      (findViewById<View>(R.id.fps) as TextView).text = String.format(Locale.US, "%.1f fps", counter.framesPerSecond)
    } else {
      (findViewById<View>(R.id.fps) as TextView).text = ""
    }
  }

  /**
   * Draws a bar graph with current memory usage split between dalvik, native and "other" (i.e.
   * GL memory, mostly). Shows a watermark at the current maximum for each of those bars as well.
   */
  private fun createMemoryGraph(width: Int, height: Int): Bitmap? {
    if (width == 0 || height == 0) {
      return null
    }

    // Grab the current memory snapshot and update the maximums, if needed.
    val memoryInfo = Debug.MemoryInfo()
    Debug.getMemoryInfo(memoryInfo)
    val dalvikKb = memoryInfo.dalvikPrivateDirty.toLong()
    val nativeKb = memoryInfo.nativePrivateDirty.toLong()
    val otherKb = memoryInfo.otherPrivateDirty.toLong()
    if (maxDalvikKb < dalvikKb) {
      maxDalvikKb = dalvikKb
    }
    if (maxNativeKb < nativeKb) {
      maxNativeKb = nativeKb
    }
    if (maxOtherKb < otherKb) {
      maxOtherKb = otherKb
    }
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val p = Paint()
    p.setARGB(80, 0, 0, 0)
    canvas.drawRect(Rect(0, 0, width, height), p)
    val max = Math.max(maxDalvikKb, Math.max(maxNativeKb, maxOtherKb)).toDouble()

    // green = dalvik memory
    p.setARGB(255, 0, 100, 0)
    canvas.drawRect(Rect(0, height - (dalvikKb / max * height).toInt(),
        (width * 0.33).toInt(), height), p)
    canvas.drawRect(Rect(0, height - (maxDalvikKb / max * height).toInt(),
        (width * 0.33).toInt(), height - (maxDalvikKb / max * height).toInt() + 2), p)

    // blue = native memory
    p.setARGB(255, 100, 100, 255)
    canvas.drawRect(Rect((width * 0.33).toInt() + 1, height - (nativeKb / max * height).toInt(),
        (width * 0.66).toInt(), height), p)
    canvas.drawRect(Rect((width * 0.33).toInt() + 1,
        height - (maxNativeKb / max * height).toInt(),
        (width * 0.66).toInt(), height - (maxNativeKb / max * height).toInt() + 2), p)

    // red = other (= GL)
    p.setARGB(255, 255, 100, 100)
    canvas.drawRect(Rect((width * 0.66).toInt() + 1, height - (otherKb / max * height).toInt(),
        width, height), p)
    canvas.drawRect(Rect((width * 0.66).toInt() + 1,
        height - (maxOtherKb / max * height).toInt(),
        width, height - (maxOtherKb / max * height).toInt() + 2), p)
    p.setARGB(255, 255, 255, 255)
    p.textSize = 16f
    p.textAlign = Paint.Align.CENTER
    canvas.drawText(String.format(Locale.US, "%d", dalvikKb / 1024), (0.00f + 0.167f) * width, height - 10.toFloat(), p)
    canvas.drawText(String.format(Locale.US, "%d", nativeKb / 1024), (0.33f + 0.167f) * width, height - 10.toFloat(), p)
    canvas.drawText(String.format(Locale.US, "%d", otherKb / 1024), (0.66f + 0.167f) * width, height - 10.toFloat(), p)
    return bmp
  }

  private class MessageInfo(var msg: String) {
    var createTime: Long
    override fun toString(): String {
      return msg
    }

    init {
      createTime = SystemClock.elapsedRealtime()
    }
  }

  private val eventListener: Any = object : Any() {
    @EventHandler
    fun onServerPacketEvent(event: ServerPacketEvent) {
      val sb = StringBuilder()
      when (event.direction) {
        ServerPacketEvent.Direction.Sent -> sb.append(">> ")
        ServerPacketEvent.Direction.Received -> sb.append("<< ")
      }
      sb.append(event.packetDebug)
      messages.add(MessageInfo(sb.toString()))
    }
  }

  companion object {
    // Maximum values for the various memory stats we track, for the 'water level' bars.
    private var maxDalvikKb: Long = 0
    private var maxNativeKb: Long = 0
    private var maxOtherKb: Long = 0
    private val messages = ArrayList<MessageInfo>()
  }

  init {
    view = View.inflate(context, R.layout.ctrl_debug, null)
    addView(view)
  }
}