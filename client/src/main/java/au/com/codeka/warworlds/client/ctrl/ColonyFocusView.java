package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.wire.Wire;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Star;

public class ColonyFocusView extends FrameLayout {
  public ColonyFocusView(Context context, AttributeSet attrs) {
    super(context, attrs);

    addView(inflate(context, R.layout.ctrl_colony_focus_view, null));
  }

  public void refresh(Star star, Colony colony) {
    int defence = (int) (0.25 * colony.population * Wire.get(colony.defence_bonus, 1.0f));
    if (defence < 1) {
      defence = 1;
    }
    TextView defenceTextView = (TextView) findViewById(R.id.colony_defence);
    defenceTextView.setText(String.format(Locale.US, "Defence: %d", defence));
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
    ProgressBar farmingFocus = (ProgressBar) findViewById(R.id.solarsystem_colony_farming_focus);
    farmingFocus.setProgress((int) (100.0f * colony.focus.farming));
    TextView farmingValue = (TextView) findViewById(R.id.solarsystem_colony_farming_value);
    farmingValue.setText(String.format(Locale.US, "%s%d / hr",
        Wire.get(colony.delta_goods, 0.0f) < 0 ? "-" : "+",
        Math.abs(Math.round(Wire.get(colony.delta_goods, 0.0f)))));

    ProgressBar miningFocus = (ProgressBar) findViewById(R.id.solarsystem_colony_mining_focus);
    miningFocus.setProgress((int) (100.0f * colony.focus.mining));
    TextView miningValue = (TextView) findViewById(R.id.solarsystem_colony_mining_value);
    miningValue.setText(String.format(Locale.US, "%s%d / hr",
        Wire.get(colony.delta_minerals, 0.0f) < 0 ? "-" : "+",
        Math.abs(Math.round(Wire.get(colony.delta_minerals, 0.0f)))));

    ProgressBar constructionFocus =
        (ProgressBar) findViewById(R.id.solarsystem_colony_construction_focus);
    constructionFocus.setProgress((int) (100.0f * colony.focus.construction));
    TextView constructionValue =
        (TextView) findViewById(R.id.solarsystem_colony_construction_value);
    int numBuildRequests = colony.build_requests == null ? 0 : colony.build_requests.size();
    if (numBuildRequests == 0) {
      constructionValue.setText(getContext().getString(R.string.idle));
    } else {
      constructionValue.setText(String.format(Locale.US, "Q: %d", numBuildRequests));
    }
  }
}
