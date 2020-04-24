package au.com.codeka.warworlds.client.game.solarsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.util.NumberFormatter
import au.com.codeka.warworlds.common.proto.Planet

/**
 * View that displays the "congeniality" of a planet.
 */
class CongenialityView(context: Context?, attributeSet: AttributeSet?) : RelativeLayout(context, attributeSet) {
  private val populationProgressBar: ProgressBar
  private val populationTextView: TextView
  private val farmingProgressBar: ProgressBar
  private val farmingTextView: TextView
  private val miningProgressBar: ProgressBar
  private val miningTextView: TextView
  private val energyProgressBar: ProgressBar
  private val energyTextView: TextView
  fun setPlanet(planet: Planet) {
    populationTextView.text = NumberFormatter.format(planet.population_congeniality)
    populationProgressBar.progress = (populationProgressBar.max
        * (planet.population_congeniality / 1000.0)).toInt()
    farmingTextView.text = NumberFormatter.format(planet.farming_congeniality)
    farmingProgressBar.progress = (farmingProgressBar.max * (planet.farming_congeniality / 100.0)).toInt()
    miningTextView.text = NumberFormatter.format(planet.mining_congeniality)
    miningProgressBar.progress = (miningProgressBar.max * (planet.mining_congeniality / 100.0)).toInt()
    energyTextView.text = NumberFormatter.format(planet.energy_congeniality)
    energyProgressBar.progress = (miningProgressBar.max * (planet.energy_congeniality / 100.0)).toInt()
  }

  init {
    View.inflate(context, R.layout.solarsystem_congeniality, this)
    populationProgressBar = findViewById(R.id.population_congeniality)
    populationTextView = findViewById(R.id.population_congeniality_value)
    farmingProgressBar = findViewById(R.id.farming_congeniality)
    farmingTextView = findViewById(R.id.farming_congeniality_value)
    miningProgressBar = findViewById(R.id.mining_congeniality)
    miningTextView = findViewById(R.id.mining_congeniality_value)
    energyProgressBar = findViewById(R.id.energy_congeniality)
    energyTextView = findViewById(R.id.energy_congeniality_value)
  }
}