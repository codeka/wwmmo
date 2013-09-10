package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Star;

public class ColonyFocusView extends FrameLayout {
    private Star mStar;
    private Colony mColony;

    public ColonyFocusView(Context context, AttributeSet attrs) {
        super(context, attrs);

        addView(inflate(context, R.layout.colony_focus_view_ctrl, null));
    }

    public void refresh(Star star, Colony colony) {
        mStar = star;
        mColony = colony;

        int defence = (int) (0.25 * mColony.getPopulation() * mColony.getDefenceBoost());
        if (defence < 1) {
            defence = 1;
        }
        TextView defenceTextView = (TextView) findViewById(R.id.colony_defence);
        defenceTextView.setText(String.format("Defence: %d", defence));

        ProgressBar populationFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_population_focus);
        populationFocus.setProgress((int)(100.0f * mColony.getPopulationFocus()));
        TextView populationValue = (TextView) findViewById(
                R.id.solarsystem_colony_population_value);
        String deltaPopulation = String.format("%s%d / hr",
                (mColony.getPopulationDelta() > 0 ? "+" : "-"),
                Math.abs((int) mColony.getPopulationDelta()));
        if (mColony.getPopulationDelta() < 0 && mColony.getPopulation() < 110.0 && mColony.isInCooldown()) {
            deltaPopulation = "<font color=\"#ffff00\">"+deltaPopulation+"</font>";
        }
        populationValue.setText(Html.fromHtml(deltaPopulation));

        ProgressBar farmingFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_farming_focus);
        farmingFocus.setProgress((int)(100.0f * mColony.getFarmingFocus()));
        TextView farmingValue= (TextView) findViewById(
                R.id.solarsystem_colony_farming_value);
        farmingValue.setText(String.format("%s%d / hr",
                mColony.getGoodsDelta() < 0 ? "-" : "+",
                Math.abs((int) mColony.getGoodsDelta())));

        ProgressBar miningFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_mining_focus);
        miningFocus.setProgress((int)(100.0f * mColony.getMiningFocus()));
        TextView miningValue = (TextView) findViewById(
                R.id.solarsystem_colony_mining_value);
        miningValue.setText(String.format("%s%d / hr",
                mColony.getMineralsDelta() < 0 ? "-" : "+",
                Math.abs((int) mColony.getMineralsDelta())));

        ProgressBar constructionFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_construction_focus);
        constructionFocus.setProgress((int)(100.0f * mColony.getConstructionFocus()));
        TextView constructionValue = (TextView) findViewById(
                R.id.solarsystem_colony_construction_value);

        int numBuildRequests = 0;
        for (BaseBuildRequest buildRequest : mStar.getBuildRequests()) {
            if (buildRequest.getColonyKey().equals(mColony.getKey())) {
                numBuildRequests ++;
            }
        }
        if (numBuildRequests == 0) {
            constructionValue.setText(String.format("idle"));
        } else {
            constructionValue.setText(String.format("Q: %d", numBuildRequests));
        }
    }
}
