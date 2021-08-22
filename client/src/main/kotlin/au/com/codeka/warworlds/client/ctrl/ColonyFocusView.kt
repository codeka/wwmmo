package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.common.proto.Colony
import com.squareup.wire.get
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class ColonyFocusView(context: Context?, attrs: AttributeSet?) : FrameLayout(context!!, attrs) {
  fun refresh(colony: Colony) {
    var defence = (0.25 * colony.population * get(colony.defence_bonus, 1.0f)).toInt()
    if (defence < 1) {
      defence = 1
    }
    val defenceTextView = findViewById<View>(R.id.colony_defence) as TextView
    defenceTextView.text = String.format(Locale.US, "Defence: %d", defence)
    /*
    ProgressBar populationFocus =
        (ProgressBar) findViewById(R.id.solarsystem_colony_population_focus);
    populationFocus.setProgress((int) (100.0f * colony.focus.population));
    TextView populationValue = (TextView) findViewById(R.id.solarsystem_colony_population_value);
    String deltaPopulation = String.format(Locale.US, "%s%d / hr",
        (Wire.get(colony.delta_population, 0.0f) > 0 ? "+" : "-"),
        Math.abs(Math.round(Wire.get(colony.delta_population, 0.0f))));
    boolean isInCooldown = (colony.cooldown_end_time < new Date().getTime());
    if (Wire.get(colony.delta_population, 0.0f) < 0 && colony.population < 110.0 && isInCooldown) {
      deltaPopulation = "<font color=\"#ffff00\">" + deltaPopulation + "</font>";
    }
    populationValue.setText(Html.fromHtml(deltaPopulation));
*/
    val farmingFocus = findViewById<View>(R.id.solarsystem_colony_farming_focus) as ProgressBar
    farmingFocus.progress = (100.0f * colony.focus.farming).toInt()
    val farmingValue = findViewById<View>(R.id.solarsystem_colony_farming_value) as TextView
    farmingValue.text = String.format(Locale.US, "%s%d / hr",
        if (get(colony.delta_goods, 0.0f) < 0) "-" else "+",
        abs(get(colony.delta_goods, 0.0f).roundToInt())
    )
    val miningFocus = findViewById<View>(R.id.solarsystem_colony_mining_focus) as ProgressBar
    miningFocus.progress = (100.0f * colony.focus.mining).toInt()
    val miningValue = findViewById<View>(R.id.solarsystem_colony_mining_value) as TextView
    miningValue.text = String.format(Locale.US, "%s%d / hr",
        if (get(colony.delta_minerals, 0.0f) < 0) "-" else "+",
        abs(get(colony.delta_minerals, 0.0f).roundToInt())
    )
    val constructionFocus = findViewById<View>(R.id.solarsystem_colony_construction_focus) as ProgressBar
    constructionFocus.progress = (100.0f * colony.focus.construction).toInt()
    val constructionValue = findViewById<View>(R.id.solarsystem_colony_construction_value) as TextView
    val numBuildRequests = colony.build_requests.size
    if (numBuildRequests == 0) {
      constructionValue.text = context.getString(R.string.idle)
    } else {
      constructionValue.text = String.format(Locale.US, "Q: %d", numBuildRequests)
    }
  }

  init {
    addView(View.inflate(context, R.layout.ctrl_colony_focus_view, null))
  }
}