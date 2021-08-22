package au.com.codeka.warworlds.client.game.build

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.ctrl.fromHtml
import au.com.codeka.warworlds.common.proto.BuildRequest
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.BuildHelper
import au.com.codeka.warworlds.common.sim.DesignHelper
import com.google.common.math.DoubleMath.roundToInt
import java.math.RoundingMode
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Bottom pane for an existing build. Shows the current progress, what we're blocked on (e.g. need
 * more minerals or population to go faster?) and allows you to cancel the build.
 */
@SuppressLint("ViewConstructor") // Must be constructed in code.
class ProgressBottomPane(
    context: Context,
    private var buildRequest: BuildRequest,
    callback: Callback) : RelativeLayout(context), BottomPaneContentView {
  interface Callback {
    fun onCancelBuild()
  }

  private val timeRemaining: TextView
  private val buildProgress: ProgressBar
  private val populationEfficiency: ProgressBar
  private val miningEfficiency: ProgressBar

  override fun refresh(star: Star?) {
    // Get an updated build request
    for (planet in star!!.planets) {
      val colony = planet.colony ?: continue
      for (br in colony.build_requests) {
        if (br.id == buildRequest.id) {
          buildRequest = br
          break
        }
      }
    }
    update()
  }

  private fun update() {
    val progress = roundToInt(BuildHelper.getBuildProgress(buildRequest, System.currentTimeMillis()) * 100.0, RoundingMode.HALF_UP)
    buildProgress.progress = progress

    // These could be null if the star hasn't been simulated recently.
    if (buildRequest.population_efficiency != null) {
      populationEfficiency.progress = (buildRequest.population_efficiency!! * 100).roundToInt()
    }
    if (buildRequest.minerals_efficiency != null) {
      miningEfficiency.progress = (buildRequest.minerals_efficiency!! * 100).roundToInt()
    }
    val verb = "Building" // (buildRequest.build_request_id == null ? "Building" : "Upgrading");
    timeRemaining.text = fromHtml(String.format(Locale.ENGLISH,
        "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
        verb, progress,
        BuildHelper.formatTimeRemaining(buildRequest)))
  }

  init {
    View.inflate(context, R.layout.build_progress_bottom_pane, this)
    val buildIcon = findViewById<ImageView>(R.id.build_icon)
    val buildName = findViewById<TextView>(R.id.build_name)
    timeRemaining = findViewById(R.id.build_time_remaining)
    buildProgress = findViewById(R.id.build_progress)
    populationEfficiency = findViewById(R.id.population_efficiency)
    miningEfficiency = findViewById(R.id.mining_efficiency)
    findViewById<View>(R.id.cancel).setOnClickListener { callback.onCancelBuild() }
    val design = DesignHelper.getDesign(buildRequest.design_type)
    BuildViewHelper.setDesignIcon(design, buildIcon)
    buildName.text = DesignHelper.getDesignName(design, false)
    update()
  }
}