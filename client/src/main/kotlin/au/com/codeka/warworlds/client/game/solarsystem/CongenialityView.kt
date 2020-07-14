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
import au.com.codeka.warworlds.common.sim.ColonyHelper

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
    val farmingCongeniality = ColonyHelper.getFarmingCongeniality(planet)
    farmingTextView.text = NumberFormatter.format(farmingCongeniality)
    farmingProgressBar.progress = (farmingProgressBar.max * (farmingCongeniality / 100.0)).toInt()
    val miningCongeniality = ColonyHelper.getMiningCongeniality(planet)
    miningTextView.text = NumberFormatter.format(miningCongeniality)
    miningProgressBar.progress = (miningProgressBar.max * (miningCongeniality / 100.0)).toInt()
    val energyCongeniality = ColonyHelper.getEnergyCongeniality(planet)
    energyTextView.text = NumberFormatter.format(energyCongeniality)
    energyProgressBar.progress = (miningProgressBar.max * (energyCongeniality / 100.0)).toInt()
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