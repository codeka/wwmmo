package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.ColonyHelper;
import com.google.common.base.Preconditions;
import com.squareup.wire.Wire;
import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * The layout for the {@link SolarSystemScreen}.
 */
// TODO: it probably makes sense to split these into a bunch of sub-views.
public class SolarSystemLayout extends DrawerLayout {
  public interface Callbacks {
    void onBuildClick(int planetIndex);
    void onFocusClick(int planetIndex);
    void onSitrepClick();
    void onViewColonyClick(int planetIndex);
    void onFleetClick(long fleetId);
  }

  private static final Log log = new Log("SolarSystemLayout");


  private final Callbacks callbacks;

  private final SunAndPlanetsView sunAndPlanetsView;
  private final TextView planetName;
  private final TextView storedGoods;
  private final TextView totalGoods;
  private final TextView deltaGoods;
  private final View storedGoodsIcon;
  private final TextView storedMinerals;
  private final TextView totalMinerals;
  private final TextView deltaMinerals;
  private final View storedMineralsIcon;
  private final TextView storedEnergy;
  private final TextView totalEnergy;
  private final TextView deltaEnergy;
  private final View storedEnergyIcon;
  private final FleetListSimple fleetList;
  private final View congenialityContainer;
  private final ProgressBar populationCongenialityProgressBar;
  private final TextView populationCongenialityTextView;
  private final ProgressBar farmingCongenialityProgressBar;
  private final TextView farmingCongenialityTextView;
  private final ProgressBar miningCongenialityProgressBar;
  private final TextView miningCongenialityTextView;
  private final ProgressBar energyCongenialityProgressBar;
  private final TextView energyCongenialityTextView;
  private final ViewGroup bottomLeftPane;
  private final Button emptyViewButton;
  private final View colonyDetailsContainer;
  private final View enemyColonyDetailsContainer;
  private final TextView populationCountTextView;
  private final ColonyFocusView colonyFocusView;

  @Nonnull private Star star;
  private int planetIndex;

  /**
   * Constructs a new {@link SolarSystemLayout}.
   *
   * @param context The {@link Context}.
   * @param star The {@link Star} to display initially.
   * @param planetIndex The index of the planet to have initially select (or -1 for no planet).
   */
  public SolarSystemLayout(Context context, Callbacks callbacks, @Nonnull Star star, int planetIndex) {
    super(context);
    inflate(context, R.layout.solarsystem, this);

    this.callbacks = callbacks;
    this.star = star;
    this.planetIndex = planetIndex;

    sunAndPlanetsView = findViewById(R.id.solarsystem_view);
    final Button buildButton = findViewById(R.id.solarsystem_colony_build);
    final Button focusButton = findViewById(R.id.solarsystem_colony_focus);
    final Button sitrepButton = findViewById(R.id.sitrep_btn);
    final Button planetViewButton = findViewById(R.id.enemy_empire_view);
    planetName = findViewById(R.id.planet_name);
    storedGoods = findViewById(R.id.stored_goods);
    totalGoods = findViewById(R.id.total_goods);
    deltaGoods = findViewById(R.id.delta_goods);
    storedGoodsIcon = findViewById(R.id.stored_goods_icon);
    storedMinerals = findViewById(R.id.stored_minerals);
    totalMinerals = findViewById(R.id.total_minerals);
    deltaMinerals = findViewById(R.id.delta_minerals);
    storedMineralsIcon = findViewById(R.id.stored_minerals_icon);
    storedEnergy = findViewById(R.id.stored_energy);
    totalEnergy = findViewById(R.id.total_energy);
    deltaEnergy = findViewById(R.id.delta_energy);
    storedEnergyIcon = findViewById(R.id.stored_energy_icon);
    fleetList = findViewById(R.id.fleet_list);
    congenialityContainer = findViewById(R.id.congeniality_container);
    populationCongenialityProgressBar = findViewById(R.id.solarsystem_population_congeniality);
    populationCongenialityTextView = findViewById(R.id.solarsystem_population_congeniality_value);
    farmingCongenialityProgressBar = findViewById(R.id.solarsystem_farming_congeniality);
    farmingCongenialityTextView = findViewById(R.id.solarsystem_farming_congeniality_value);
    miningCongenialityProgressBar = findViewById(R.id.solarsystem_mining_congeniality);
    miningCongenialityTextView = findViewById(R.id.solarsystem_mining_congeniality_value);
    energyCongenialityProgressBar = findViewById(R.id.solarsystem_energy_congeniality);
    energyCongenialityTextView = findViewById(R.id.solarsystem_energy_congeniality_value);
    bottomLeftPane = findViewById(R.id.bottom_left_pane);
    emptyViewButton = findViewById(R.id.empty_view_btn);
    colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details);
    enemyColonyDetailsContainer = findViewById(R.id.enemy_colony_details);
    populationCountTextView = findViewById(R.id.population_count);
    colonyFocusView = findViewById(R.id.colony_focus_view);

    sunAndPlanetsView.setPlanetSelectedHandler(planet -> {
      if (planet == null) {
        SolarSystemLayout.this.planetIndex = -1;
      } else {
        SolarSystemLayout.this.planetIndex = planet.index;
      }
      refreshSelectedPlanet();
    });

    buildButton.setOnClickListener(v -> callbacks.onBuildClick(planetIndex));
    focusButton.setOnClickListener(v -> callbacks.onFocusClick(planetIndex));
    sitrepButton.setOnClickListener(v -> callbacks.onSitrepClick());
    planetViewButton.setOnClickListener(v -> callbacks.onViewColonyClick(planetIndex));
    emptyViewButton.setOnClickListener(v -> callbacks.onViewColonyClick(planetIndex));
    fleetList.setFleetSelectedHandler(fleet -> callbacks.onFleetClick(fleet.id));

    refreshStar(star);
  }

  public void refreshStar(Star star) {
    fleetList.setStar(star);
    sunAndPlanetsView.setStar(star);

    Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
    EmpireStorage storage = null;
    for (EmpireStorage s : star.empire_stores) {
      if (s.empire_id != null && s.empire_id.equals(myEmpire.id)) {
        storage = s;
        break;
      }
    }
    if (storage == null) {
      storedGoods.setVisibility(View.GONE);
      deltaGoods.setVisibility(View.GONE);
      totalGoods.setVisibility(View.GONE);
      storedGoodsIcon.setVisibility(View.GONE);
      storedMinerals.setVisibility(View.GONE);
      deltaMinerals.setVisibility(View.GONE);
      totalMinerals.setVisibility(View.GONE);
      storedMineralsIcon.setVisibility(View.GONE);
      storedEnergy.setVisibility(View.GONE);
      deltaEnergy.setVisibility(View.GONE);
      totalEnergy.setVisibility(View.GONE);
      storedEnergyIcon.setVisibility(View.GONE);
    } else {
      storedGoods.setVisibility(View.VISIBLE);
      deltaGoods.setVisibility(View.VISIBLE);
      totalGoods.setVisibility(View.VISIBLE);
      storedGoodsIcon.setVisibility(View.VISIBLE);
      storedMinerals.setVisibility(View.VISIBLE);
      deltaMinerals.setVisibility(View.VISIBLE);
      totalMinerals.setVisibility(View.VISIBLE);
      storedMineralsIcon.setVisibility(View.VISIBLE);
      storedEnergy.setVisibility(View.VISIBLE);
      deltaEnergy.setVisibility(View.VISIBLE);
      totalEnergy.setVisibility(View.VISIBLE);
      storedEnergyIcon.setVisibility(View.VISIBLE);

      storedGoods.setText(NumberFormatter.format(Math.round(storage.total_goods)));
      totalGoods.setText(String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_goods))));
      storedMinerals.setText(NumberFormatter.format(Math.round(storage.total_minerals)));
      totalMinerals.setText(String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_minerals))));
      storedEnergy.setText(NumberFormatter.format(Math.round(storage.total_energy)));
      totalEnergy.setText(String.format(Locale.ENGLISH, "/ %s",
          NumberFormatter.format(Math.round(storage.max_energy))));

      if (Wire.get(storage.goods_delta_per_hour, 0.0f) >= 0) {
        deltaGoods.setTextColor(Color.GREEN);
        deltaGoods.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(Wire.get(storage.goods_delta_per_hour, 0.0f))));
      } else {
        deltaGoods.setTextColor(Color.RED);
        deltaGoods.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(Wire.get(storage.goods_delta_per_hour, 0.0f))));
      }
      if (Wire.get(storage.minerals_delta_per_hour, 0.0f) >= 0) {
        deltaMinerals.setTextColor(Color.GREEN);
        deltaMinerals.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(Wire.get(storage.minerals_delta_per_hour, 0.0f))));
      } else {
        deltaMinerals.setTextColor(Color.RED);
        deltaMinerals.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(Wire.get(storage.minerals_delta_per_hour, 0.0f))));
      }
      if (Wire.get(storage.energy_delta_per_hour, 0.0f) >= 0) {
        deltaEnergy.setTextColor(Color.GREEN);
        deltaEnergy.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(Wire.get(storage.energy_delta_per_hour, 0.0f))));
      } else {
        deltaEnergy.setTextColor(Color.RED);
        deltaEnergy.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(Wire.get(storage.energy_delta_per_hour, 0.0f))));
      }
    }

    if (planetIndex >= 0) {
      planetName.setText(this.star.name);
    }

    if (planetIndex >= 0) {
      log.debug("Selecting planet #%d", planetIndex);
      sunAndPlanetsView.selectPlanet(planetIndex);
    } else {
      log.debug("No planet selected");
    }

  }

  private void refreshSelectedPlanet() {
    if (planetIndex < 0) {
      return;
    }
    Planet planet = star.planets.get(planetIndex);

    Vector2 planetCentre = sunAndPlanetsView.getPlanetCentre(planet);

    String name = star.name + " " + RomanNumeralFormatter.format(star.planets.indexOf(planet) + 1);
    planetName.setText(name);

    if (planetCentre == null) {
      // this is probably because the SolarSystemView probably hasn't rendered yet. We'll
      // just ignore this then cause it'll fire an onPlanetSelected when it finishes
      // drawing.
      congenialityContainer.setVisibility(View.GONE);
    } else {
      float pixelScale = getContext().getResources().getDisplayMetrics().density;
      double x = planetCentre.x;
      double y = planetCentre.y;

      // hard-coded size of the congeniality container: 85x64 dp
      float offsetX = (85 + 20) * pixelScale;
      float offsetY = (64 + 20) * pixelScale;

      if (x - offsetX < 0) {
        offsetX  = -(20 * pixelScale);
      }
      if (y - offsetY < 20) {
        offsetY = -(20 * pixelScale);
      }

      RelativeLayout.LayoutParams params =
          (RelativeLayout.LayoutParams) congenialityContainer.getLayoutParams();
      params.leftMargin = (int) (x - offsetX);
      params.topMargin = (int) (y - offsetY);
      if (params.topMargin < (40 * pixelScale)) {
        params.topMargin = (int)(40 * pixelScale);
      }

      congenialityContainer.setLayoutParams(params);
      congenialityContainer.setVisibility(View.VISIBLE);
    }

    populationCongenialityTextView.setText(NumberFormatter.format(planet.population_congeniality));
    populationCongenialityProgressBar.setProgress(
        (int) (populationCongenialityProgressBar.getMax()
            * (planet.population_congeniality / 1000.0)));

    farmingCongenialityTextView.setText(NumberFormatter.format(planet.farming_congeniality));
    farmingCongenialityProgressBar.setProgress(
        (int)(farmingCongenialityProgressBar.getMax() * (planet.farming_congeniality / 100.0)));

    miningCongenialityTextView.setText(NumberFormatter.format(planet.mining_congeniality));
    miningCongenialityProgressBar.setProgress(
        (int)(miningCongenialityProgressBar.getMax() * (planet.mining_congeniality / 100.0)));

    energyCongenialityTextView.setText(NumberFormatter.format(planet.energy_congeniality));
    energyCongenialityProgressBar.setProgress(
        (int)(miningCongenialityProgressBar.getMax() * (planet.energy_congeniality / 100.0)));

    TransitionManager.beginDelayedTransition(bottomLeftPane);
    if (planet.colony == null) {
      emptyViewButton.setVisibility(View.VISIBLE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      refreshUncolonizedDetails();
    } else {
      emptyViewButton.setVisibility(View.GONE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      if (planet.colony.empire_id == null) {
        enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
        refreshNativeColonyDetails(planet);
      } else {
        Empire colonyEmpire = EmpireManager.i.getEmpire(planet.colony.empire_id);
        if (colonyEmpire != null) {
          Empire myEmpire = EmpireManager.i.getMyEmpire();
          if (myEmpire.id.equals(colonyEmpire.id)) {
            colonyDetailsContainer.setVisibility(View.VISIBLE);
            refreshColonyDetails(planet);
          } else {
            enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
            refreshEnemyColonyDetails(colonyEmpire, planet);
          }
        } else {
          // TODO: wait for the empire to come in.
        }
      }
    }
  }

  private void refreshUncolonizedDetails() {
    populationCountTextView.setText(getContext().getString(R.string.uncolonized));
  }

  private void refreshColonyDetails(Planet planet) {
    String pop = "Pop: "
        + Math.round(planet.colony.population)
        + " <small>"
        + String.format(Locale.US, "(%s%d / hr)",
        Wire.get(planet.colony.delta_population, 0.0f) < 0 ? "-" : "+",
        Math.abs(Math.round(Wire.get(planet.colony.delta_population, 0.0f))))
        + "</small> / "
        + ColonyHelper.getMaxPopulation(planet);
    populationCountTextView.setText(Html.fromHtml(pop));

    colonyFocusView.setVisibility(View.VISIBLE);
    colonyFocusView.refresh(star, planet.colony);
  }

  private void refreshEnemyColonyDetails(Empire empire, Planet planet) {
    populationCountTextView.setText(String.format(Locale.US, "Population: %d",
        Math.round(planet.colony.population)));

/*    ImageView enemyIcon = (ImageView) mView.findViewById(R.id.enemy_empire_icon);
    TextView enemyName = (TextView) mView.findViewById(R.id.enemy_empire_name);
    TextView enemyDefence = (TextView) mView.findViewById(R.id.enemy_empire_defence);

    int defence = (int) (0.25 * mColony.getPopulation() * mColony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }
    enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
    enemyName.setText(empire.getDisplayName());
    enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
  */}

  private void refreshNativeColonyDetails(Planet planet) {
    String pop = "Pop: "
        + Math.round(planet.colony.population);
    populationCountTextView.setText(Html.fromHtml(pop));
    colonyFocusView.setVisibility(View.GONE);
  }
}
