package au.com.codeka.warworlds.client.game.build

import android.content.Context
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.common.proto.Building
import au.com.codeka.warworlds.common.proto.Colony
import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Star
import com.google.common.base.Preconditions
import java.util.*

class UpgradeBottomPane : RelativeLayout, BottomPaneContentView {
  interface Callback {
    fun onUpgrade(building: Building?)
  }

  private val star: Star?
  private val colony: Colony?
  private val design: Design?
  private val building: Building?
  private val callback: Callback?
  private val buildIcon: ImageView?
  private val buildName: TextView?
  private val buildDescription: TextView?
  private val buildTime: TextView?
  private val buildMinerals: TextView?
  private val currentLevel: TextView?

  constructor(context: Context?) : super(context) {
    Preconditions.checkState(isInEditMode)
    star = null
    colony = null
    design = null
    building = null
    callback = null
    buildIcon = null
    buildName = null
    buildDescription = null
    buildTime = null
    buildMinerals = null
    currentLevel = null
  }

  constructor(
      context: Context?,
      star: Star?,
      colony: Colony?,
      design: Design,
      building: Building?,
      callback: Callback?) : super(context) {
    this.star = star
    this.colony = colony
    this.design = design
    this.building = building
    this.callback = callback
    View.inflate(context, R.layout.build_upgrade_bottom_pane, this)
    buildIcon = findViewById(R.id.build_icon)
    buildName = findViewById(R.id.build_name)
    buildDescription = findViewById(R.id.build_description)
    buildTime = findViewById(R.id.build_timetobuild)
    buildMinerals = findViewById(R.id.build_mineralstobuild)
    currentLevel = findViewById(R.id.upgrade_current_level)
    findViewById<View>(R.id.build_button).setOnClickListener { v: View? -> upgrade() }
    currentLevel.text = String.format(Locale.US, "%d", building!!.level)
    BuildViewHelper.setDesignIcon(design, buildIcon)
    buildName.text = design.display_name
    buildDescription.text = Html.fromHtml(design.description)
    updateBuildTime()
  }

  override fun refresh(star: Star?) {}
  private fun updateBuildTime() {
    BuildTimeCalculator(star, colony).calculateUpgradeTime(design, building
    ) { time: String?, minerals: String?, mineralsColor: Int ->
      buildTime!!.text = time
      buildMinerals!!.text = minerals
      buildMinerals.setTextColor(mineralsColor)
    }
  }

  private fun upgrade() {
    callback!!.onUpgrade(building)
  }
}