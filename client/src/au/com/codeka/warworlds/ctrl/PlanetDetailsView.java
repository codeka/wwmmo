package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;

public class PlanetDetailsView extends FrameLayout {
    private View mView;
    private Star mStar;
    private Planet mPlanet;
    private boolean mIsAttachedToWindow;
    private Colony mColony;

    public PlanetDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mView = inflate(context, R.layout.planet_details_ctrl, null);
        addView(mView);
    }

    public void setup(Star star, Planet planet, Colony colony) {
        mStar = star;
        mPlanet = planet;
        mColony = colony;

        if (mIsAttachedToWindow) {
            refresh();
        }
    }

    @Override
    public void onAttachedToWindow() {
        if (isInEditMode()) {
            return;
        }
        mIsAttachedToWindow = true;

        if (mStar == null || mPlanet == null) {
            return;
        }

        ImageManager.eventBus.register(mEventHandler);
        refresh();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;

        ImageManager.eventBus.unregister(mEventHandler);
    }

    private void refresh() {
        View congenialityContainer = mView.findViewById(R.id.congeniality_container);
        View colonyContainer = mView.findViewById(R.id.colony_container);
        ImageView planetIcon = (ImageView) mView.findViewById(R.id.planet_icon);
        ProgressBar populationCongenialityProgressBar = (ProgressBar) mView.findViewById(R.id.population_congeniality);
        ProgressBar farmingCongenialityProgressBar = (ProgressBar) mView.findViewById(R.id.farming_congeniality);
        ProgressBar miningCongenialityProgressBar = (ProgressBar) mView.findViewById(R.id.mining_congeniality);
        TextView populationCongenialityTextView = (TextView) mView.findViewById(R.id.population_congeniality_value);
        TextView farmingCongenialityTextView = (TextView) mView.findViewById(R.id.farming_congeniality_value);
        TextView miningCongenialityTextView = (TextView) mView.findViewById(R.id.mining_congeniality_value);
        TextView populationCount = (TextView) mView.findViewById(R.id.population_count);
        ColonyFocusView colonyFocusView = (ColonyFocusView) mView.findViewById(R.id.colony_focus_view);

        if (mStar == null || mPlanet == null) {
            planetIcon.setVisibility(View.GONE);
            congenialityContainer.setVisibility(View.GONE);
            colonyContainer.setVisibility(View.GONE);
        } else {
            planetIcon.setVisibility(View.VISIBLE);

            Sprite planetSprite = PlanetImageManager.getInstance().getSprite(mPlanet);
            if (planetSprite != null) {
                planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));
            }

            if (mColony == null || mColony.getEmpireKey() == null ||
                    !mColony.getEmpireKey().equals(EmpireManager.i.getEmpire().getKey())) {
                congenialityContainer.setVisibility(View.VISIBLE);
                colonyContainer.setVisibility(View.GONE);

                populationCongenialityProgressBar.setMax(1000);
                populationCongenialityProgressBar.setProgress(mPlanet.getPopulationCongeniality());
                populationCongenialityTextView.setText(Integer.toString(mPlanet.getPopulationCongeniality()));
                farmingCongenialityProgressBar.setMax(100);
                farmingCongenialityProgressBar.setProgress(mPlanet.getFarmingCongeniality());
                farmingCongenialityTextView.setText(Integer.toString(mPlanet.getFarmingCongeniality()));
                miningCongenialityProgressBar.setMax(100);
                miningCongenialityProgressBar.setProgress(mPlanet.getMiningCongeniality());
                miningCongenialityTextView.setText(Integer.toString(mPlanet.getMiningCongeniality()));
            } else {
                congenialityContainer.setVisibility(View.GONE);
                colonyContainer.setVisibility(View.VISIBLE);

                populationCount.setText(String.format("Pop: %d / %d",
                        (int) mColony.getPopulation(), (int) mColony.getMaxPopulation()));
                colonyFocusView.refresh(mStar, mColony);
            }
        }
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onSpriteGenerated(ImageManager.SpriteGeneratedEvent event) {
            refresh();
        }
    };
}
