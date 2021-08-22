package au.com.codeka.warworlds.client.game.build

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.fromHtml
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.ColonyFocus
import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Design.DesignType
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.google.common.base.Preconditions
import java.util.*

/** Bottom pane of the build fragment for when we're building something new. */
@SuppressLint("ViewConstructor") // Must be constructed in code.
class BuildBottomPane (
  context: Context?,
  private val star: Star,
  private val colony: Colony,
  private val design: Design,
  private val callback: Callback
) : RelativeLayout(context), BottomPaneContentView {
  interface Callback {
    fun onBuild(designType: DesignType?, count: Int)
  }

  private val buildIcon: ImageView
  private val buildName: TextView
  private val buildDescription: TextView
  private val buildTime: TextView
  private val buildMinerals: TextView
  private val buildCountContainer: ViewGroup
  private val buildCountSeek: SeekBar
  private val buildCount: EditText

  init {
    View.inflate(context, R.layout.build_build_bottom_pane, this)
    buildIcon = findViewById(R.id.build_icon)
    buildName = findViewById(R.id.build_name)
    buildDescription = findViewById(R.id.build_description)
    buildCountContainer = findViewById(R.id.build_count_container)
    buildTime = findViewById(R.id.build_timetobuild)
    buildMinerals = findViewById(R.id.build_mineralstobuild)
    buildCountSeek = findViewById(R.id.build_count_seek)
    buildCount = findViewById(R.id.build_count_edit)
    buildCountSeek.max = 1000
    val buildCountSeekBarChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, userInitiated: Boolean) {
        if (!userInitiated) {
          return
        }
        buildCount.setText(String.format(Locale.US, "%d", progress))
        updateBuildTime()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
    buildCountSeek.setOnSeekBarChangeListener(buildCountSeekBarChangeListener)
    findViewById<View>(R.id.build_button).setOnClickListener { build() }
    buildCount.setText("1")
    buildCountSeek.progress = 1
    BuildViewHelper.setDesignIcon(design, buildIcon)
    buildName.text = design.display_name
    buildDescription.text = fromHtml(design.description)
    if (design.design_kind == Design.DesignKind.SHIP) {
      // You can only build more than ship at a time (not buildings).
      buildCountContainer.visibility = View.VISIBLE
    } else {
      buildCountContainer.visibility = View.GONE
    }
    updateBuildTime()
  }

  private fun updateBuildTime() {
    var count = 1
    if (design.design_kind == Design.DesignKind.SHIP) {
      count = buildCount.text.toString().toInt()
    }
    BuildTimeCalculator(star, colony).calculateBuildTime(design, count
    ) { time: String?, minerals: String?, mineralsColor: Int ->
      buildTime.text = time
      buildMinerals.text = minerals
      buildMinerals.setTextColor(mineralsColor)
    }
  }

  override fun refresh(star: Star?) {
    // TODO
  }

  /** Start building the thing we currently have showing.  */
  fun build() {
    val str = buildCount.text.toString()
    val count: Int = try {
      str.toInt()
    } catch (e: NumberFormatException) {
      1
    }
    if (count <= 0) {
      return
    }
    callback.onBuild(design.type, count)
  }
}