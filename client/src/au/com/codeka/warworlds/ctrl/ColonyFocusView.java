package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;

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

        int defence = (int) (0.25 * mColony.population * mColony.defence_bonus);
        if (defence < 1) {
            defence = 1;
        }
        TextView defenceTextView = (TextView) findViewById(R.id.colony_defence);
        defenceTextView.setText(String.format("Defence: %d", defence));

        ProgressBar populationFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_population_focus);
        populationFocus.setProgress((int)(100.0f * mColony.focus_population));
        TextView populationValue = (TextView) findViewById(
                R.id.solarsystem_colony_population_value);
        String deltaPopulation = String.format("%s%d / hr",
                (mColony.delta_population > 0 ? "+" : "-"),
                Math.abs((int) (float) mColony.delta_population));
        if (mColony.delta_population < 0 && mColony.population < 110.0 && Model.isInCooldown(mColony)) {
            deltaPopulation = "<font color=\"#ffff00\">"+deltaPopulation+"</font>";
        }
        populationValue.setText(Html.fromHtml(deltaPopulation));

        ProgressBar farmingFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_farming_focus);
        farmingFocus.setProgress((int)(100.0f * mColony.focus_farming));
        TextView farmingValue= (TextView) findViewById(
                R.id.solarsystem_colony_farming_value);
        farmingValue.setText(String.format("%s%d / hr",
                mColony.delta_goods < 0 ? "-" : "+",
                Math.abs((int) (float) mColony.delta_goods)));

        ProgressBar miningFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_mining_focus);
        miningFocus.setProgress((int)(100.0f * mColony.focus_mining));
        TextView miningValue = (TextView) findViewById(
                R.id.solarsystem_colony_mining_value);
        miningValue.setText(String.format("%s%d / hr",
                mColony.delta_minerals < 0 ? "-" : "+",
                Math.abs((int) (float) mColony.delta_minerals)));

        ProgressBar constructionFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_construction_focus);
        constructionFocus.setProgress((int)(100.0f * mColony.focus_construction));
        TextView constructionValue = (TextView) findViewById(
                R.id.solarsystem_colony_construction_value);

        int numBuildRequests = 0;
        for (BuildRequest buildRequest : mStar.build_requests) {
            if (buildRequest.colony_key.equals(mColony.key)) {
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
