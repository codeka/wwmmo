package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.Group;

import java.util.Locale;

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
  private View view;
  private Star star;
  private Planet planet;
  private boolean isAttachedToWindow;
  private Colony colony;

  public PlanetDetailsView(Context context, AttributeSet attrs) {
    super(context, attrs);
    view = inflate(context, R.layout.planet_details_ctrl, null);
    addView(view);
  }

  public void setup(Star star, Planet planet, Colony colony) {
    this.star = star;
    this.planet = planet;
    this.colony = colony;

    if (isAttachedToWindow) {
      refresh();
    }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (isInEditMode()) {
      return;
    }
    isAttachedToWindow = true;

    if (star == null || planet == null) {
      return;
    }

    ImageManager.eventBus.register(mEventHandler);
    refresh();
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    isAttachedToWindow = false;

    ImageManager.eventBus.unregister(mEventHandler);
  }

  private void refresh() {
    Group congenialityContainer = view.findViewById(R.id.congeniality_group);
    Group colonyContainer = view.findViewById(R.id.colony_group);
    ImageView planetIcon = view.findViewById(R.id.planet_icon);
    ProgressBar populationCongenialityProgressBar =
        view.findViewById(R.id.population_congeniality);
    ProgressBar farmingCongenialityProgressBar =
        view.findViewById(R.id.farming_congeniality);
    ProgressBar miningCongenialityProgressBar =
        view.findViewById(R.id.mining_congeniality);
    TextView populationCongenialityTextView =
        view.findViewById(R.id.population_congeniality_value);
    TextView farmingCongenialityTextView =
        view.findViewById(R.id.farming_congeniality_value);
    TextView miningCongenialityTextView =
        view.findViewById(R.id.mining_congeniality_value);
    TextView populationCount = view.findViewById(R.id.population_count);
    StarStorageView starStorageView = view.findViewById(R.id.star_storage);

    if (star == null || planet == null) {
      planetIcon.setVisibility(View.GONE);
      congenialityContainer.setVisibility(View.GONE);
      colonyContainer.setVisibility(View.GONE);
      starStorageView.setVisibility(View.GONE);
    } else {
      planetIcon.setVisibility(View.VISIBLE);
      starStorageView.refresh(star);

      Sprite planetSprite = PlanetImageManager.getInstance().getSprite(planet);
      if (planetSprite != null) {
        planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));
      }

      populationCongenialityProgressBar.setMax(1000);
      populationCongenialityProgressBar.setProgress(planet.getPopulationCongeniality());
      populationCongenialityTextView.setText(
          String.format(Locale.US, "%d", planet.getPopulationCongeniality()));
      farmingCongenialityProgressBar.setMax(100);
      farmingCongenialityProgressBar.setProgress(planet.getFarmingCongeniality());
      farmingCongenialityTextView.setText(
          String.format(Locale.US, "%d", planet.getFarmingCongeniality()));
      miningCongenialityProgressBar.setMax(100);
      miningCongenialityProgressBar.setProgress(planet.getMiningCongeniality());
      miningCongenialityTextView.setText(
          String.format(Locale.US, "%d", planet.getMiningCongeniality()));
      if (colony == null || colony.getEmpireKey() == null ||
          !colony.getEmpireKey().equals(EmpireManager.i.getEmpire().getKey())) {
        colonyContainer.setVisibility(View.GONE);
      } else {
        colonyContainer.setVisibility(View.VISIBLE);

        populationCount.setText(String.format(Locale.US, "Pop: %d / %d",
            (int) colony.getPopulation(), (int) colony.getMaxPopulation()));
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
