package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.common.proto.Planet;

/**
 * View that displays the "congeniality" of a planet.
 */
public class CongenialityView extends RelativeLayout {
  private final ProgressBar populationProgressBar;
  private final TextView populationTextView;
  private final ProgressBar farmingProgressBar;
  private final TextView farmingTextView;
  private final ProgressBar miningProgressBar;
  private final TextView miningTextView;
  private final ProgressBar energyProgressBar;
  private final TextView energyTextView;

  public CongenialityView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    inflate(context, R.layout.solarsystem_congeniality, this);

    populationProgressBar = findViewById(R.id.population_congeniality);
    populationTextView = findViewById(R.id.population_congeniality_value);
    farmingProgressBar = findViewById(R.id.farming_congeniality);
    farmingTextView = findViewById(R.id.farming_congeniality_value);
    miningProgressBar = findViewById(R.id.mining_congeniality);
    miningTextView = findViewById(R.id.mining_congeniality_value);
    energyProgressBar = findViewById(R.id.energy_congeniality);
    energyTextView = findViewById(R.id.energy_congeniality_value);
  }

  public void setPlanet(Planet planet) {
    populationTextView.setText(NumberFormatter.format(planet.population_congeniality));
    populationProgressBar.setProgress(
        (int) (populationProgressBar.getMax()
            * (planet.population_congeniality / 1000.0)));

    farmingTextView.setText(NumberFormatter.format(planet.farming_congeniality));
    farmingProgressBar.setProgress(
        (int)(farmingProgressBar.getMax() * (planet.farming_congeniality / 100.0)));

    miningTextView.setText(NumberFormatter.format(planet.mining_congeniality));
    miningProgressBar.setProgress(
        (int)(miningProgressBar.getMax() * (planet.mining_congeniality / 100.0)));

    energyTextView.setText(NumberFormatter.format(planet.energy_congeniality));
    energyProgressBar.setProgress(
        (int)(miningProgressBar.getMax() * (planet.energy_congeniality / 100.0)));
  }
}
