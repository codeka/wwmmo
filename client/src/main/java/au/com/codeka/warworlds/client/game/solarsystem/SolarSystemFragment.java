package au.com.codeka.warworlds.client.game.solarsystem;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.wire.Wire;

import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.game.build.BuildFragment;
import au.com.codeka.warworlds.client.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.game.fleets.FleetsFragment;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.ColonyHelper;

/**
 * This is a fragment which displays details about a single solar system.
 */
public class SolarSystemFragment extends BaseFragment {
  private static final Log log = new Log("SolarSystemFragment");

  public static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private Planet planet;
  private long starID;

  private SolarSystemView solarSystemView;
  private TextView planetName;
  private TextView storedGoods;
  private TextView totalGoods;
  private TextView deltaGoods;
  private View storedGoodsIcon;
  private TextView storedMinerals;
  private TextView totalMinerals;
  private TextView deltaMinerals;
  private View storedMineralsIcon;
  private TextView storedEnergy;
  private TextView totalEnergy;
  private TextView deltaEnergy;
  private View storedEnergyIcon;
  private FleetListSimple fleetList;
  private View congenialityContainer;
  private ProgressBar populationCongenialityProgressBar;
  private TextView populationCongenialityTextView;
  private ProgressBar farmingCongenialityProgressBar;
  private TextView farmingCongenialityTextView;
  private ProgressBar miningCongenialityProgressBar;
  private TextView miningCongenialityTextView;
  private ProgressBar energyCongenialityProgressBar;
  private TextView energyCongenialityTextView;
  private ViewGroup bottomLeftPane;
  private Button emptyViewButton;
  private View colonyDetailsContainer;
  private View enemyColonyDetailsContainer;
  private TextView populationCountTextView;
  private ColonyFocusView colonyFocusView;

  public static Bundle createArguments(long starID) {
    return createArguments(starID, -1);
  }

  public static Bundle createArguments(long starID, int planetIndex) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starID);
    if (planetIndex >= 0) {
      args.putInt(PLANET_INDEX_KEY, planetIndex);
    }
    return args;
  }

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_solarsystem;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    solarSystemView = view.findViewById(R.id.solarsystem_view);
    final Button buildButton = view.findViewById(R.id.solarsystem_colony_build);
    final Button focusButton = view.findViewById(R.id.solarsystem_colony_focus);
    final Button sitrepButton = view.findViewById(R.id.sitrep_btn);
    final Button planetViewButton = view.findViewById(R.id.enemy_empire_view);
    planetName = view.findViewById(R.id.planet_name);
    storedGoods = view.findViewById(R.id.stored_goods);
    totalGoods = view.findViewById(R.id.total_goods);
    deltaGoods = view.findViewById(R.id.delta_goods);
    storedGoodsIcon = view.findViewById(R.id.stored_goods_icon);
    storedMinerals = view.findViewById(R.id.stored_minerals);
    totalMinerals = view.findViewById(R.id.total_minerals);
    deltaMinerals = view.findViewById(R.id.delta_minerals);
    storedMineralsIcon = view.findViewById(R.id.stored_minerals_icon);
    storedEnergy = view.findViewById(R.id.stored_energy);
    totalEnergy = view.findViewById(R.id.total_energy);
    deltaEnergy = view.findViewById(R.id.delta_energy);
    storedEnergyIcon = view.findViewById(R.id.stored_energy_icon);
    fleetList = view.findViewById(R.id.fleet_list);
    congenialityContainer = view.findViewById(R.id.congeniality_container);
    populationCongenialityProgressBar = view.findViewById(R.id.solarsystem_population_congeniality);
    populationCongenialityTextView =
        view.findViewById(R.id.solarsystem_population_congeniality_value);
    farmingCongenialityProgressBar = view.findViewById(R.id.solarsystem_farming_congeniality);
    farmingCongenialityTextView = view.findViewById(R.id.solarsystem_farming_congeniality_value);
    miningCongenialityProgressBar = view.findViewById(R.id.solarsystem_mining_congeniality);
    miningCongenialityTextView = view.findViewById(R.id.solarsystem_mining_congeniality_value);
    energyCongenialityProgressBar = view.findViewById(R.id.solarsystem_energy_congeniality);
    energyCongenialityTextView = view.findViewById(R.id.solarsystem_energy_congeniality_value);
    bottomLeftPane = view.findViewById(R.id.bottom_left_pane);
    emptyViewButton = view.findViewById(R.id.empty_view_btn);
    colonyDetailsContainer = view.findViewById(R.id.solarsystem_colony_details);
    enemyColonyDetailsContainer = view.findViewById(R.id.enemy_colony_details);
    populationCountTextView = view.findViewById(R.id.population_count);
    colonyFocusView = view.findViewById(R.id.colony_focus_view);

    solarSystemView.setPlanetSelectedHandler(planet -> {
      SolarSystemFragment.this.planet = planet;
      refreshSelectedPlanet();
    });

    buildButton.setOnClickListener(v -> {
      if (star == null) {
        return; // can happen before the star loads
      }
      if (planet.colony == null) {
        return; // shouldn't happen, the button should be hidden.
      }

      getFragmentTransitionManager()
          .replaceFragment(BuildFragment.class,
              BuildFragment.createArguments(star.id, planet.index),
              SharedViewHolder.builder()
                  .addSharedView(solarSystemView.getPlanetView(planet), "planet_icon")
                  .addSharedView(R.id.bottom_pane, "bottom_pane")
                  .build());
    });

    focusButton.setOnClickListener(v -> {
      if (star == null || star.planets == null || planet.colony == null) {
        return;
      }

      getFragmentTransitionManager().replaceFragment(
          PlanetDetailsFragment.class,
          PlanetDetailsFragment.createArguments(star.id, star.planets.indexOf(planet)),
          SharedViewHolder.builder()
              .addSharedView(R.id.bottom_pane, "bottom_pane")
              .addSharedView(solarSystemView.getPlanetView(planet), "planet_icon")
              .build());
    });

    sitrepButton.setOnClickListener(v -> {
      if (star == null) {
        return; // can happen before the star loads
      }

      // TODO
//        Intent intent = new Intent(getActivity(), SitrepActivity.class);
//        intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
//        startActivity(intent);
    });

    planetViewButton.setOnClickListener(v -> onViewColony());

    emptyViewButton.setOnClickListener(v -> onViewColony());

    fleetList.setFleetSelectedHandler(fleet -> getFragmentTransitionManager().replaceFragment(
        FleetsFragment.class,
        FleetsFragment.createArguments(star.id, fleet.id),
        SharedViewHolder.builder()
            .addSharedView(R.id.bottom_pane, "bottom_pane")
            .build()));
  }

  @Override
  public void onResume() {
    super.onResume();
    Bundle args = getArguments();
    starID = args.getLong(STAR_ID_KEY);
    App.i.getEventBus().register(eventHandler);

    log.info("Getting star: %d", starID);
    onStarFetched(StarManager.i.getStar(starID));
  }

  @Override
  public void onPause() {
    super.onPause();
    App.i.getEventBus().unregister(eventHandler);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star star) {
      if (star != null && starID == star.id) {
        onStarFetched(star);
      }
    }
  };

  private void onStarFetched(Star star) {
    // If we don't have a star yet, we'll need to figure out which planet to select initially from
    // the arguments that started us. Otherwise, we'll want to select whatever planet we have
    // currently selected.
    int selectedPlanetIndex;
    boolean isFirstRefresh = (this.star == null);
    if (isFirstRefresh) {
      selectedPlanetIndex = getArguments().getInt(PLANET_INDEX_KEY, -1);
    } else {
      selectedPlanetIndex = solarSystemView.getSelectedPlanetIndex();
    }

    solarSystemView.setStar(star);
    if (selectedPlanetIndex >= 0) {
      log.debug("Selecting planet #%d", selectedPlanetIndex);
      solarSystemView.selectPlanet(selectedPlanetIndex);
    } else {
      log.debug("No planet selected");
    }

    this.star = star;
    if (selectedPlanetIndex >= 0) {
      this.planet = star.planets.get(selectedPlanetIndex);
    }

    refresh();

    if (this.planet == null) {
      planetName.setText(this.star.name);
    }

    if (isFirstRefresh) {
      Bundle extras = getArguments();
      boolean showScoutReport = extras.getBoolean("au.com.codeka.warworlds.ShowScoutReport");
      if (showScoutReport) {
       //// ScoutReportDialog dialog = new ScoutReportDialog();
      //  dialog.setStar(this.star);
       // dialog.show(getFragmentManager(), "");
      }

      String combatReportKey = extras.getString("au.com.codeka.warworlds.CombatReportKey");
      if (!showScoutReport && combatReportKey != null) {
     //   CombatReportDialog dialog = new CombatReportDialog();
    //    dialog.loadCombatReport(this.star, combatReportKey);
    //    dialog.show(getFragmentManager(), "");
      }
    }
  }

  private void refresh() {
    fleetList.setStar(star);

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
    refreshSelectedPlanet();
  }

  private void onViewColony() {
    if (planet == null) {
      return;
    }

    getFragmentTransitionManager().replaceFragment(
        PlanetDetailsFragment.class,
        PlanetDetailsFragment.createArguments(star.id, star.planets.indexOf(planet)),
        SharedViewHolder.builder()
            .addSharedView(R.id.bottom_pane, "bottom_pane")
            .addSharedView(solarSystemView.getPlanetView(planet), "planet_icon")
            .build());
  }

  private void refreshSelectedPlanet() {
    if (star == null || planet == null) {
      return;
    }

    Vector2 planetCentre = solarSystemView.getPlanetCentre(planet);

    String name = star.name + " " + RomanNumeralFormatter.format(star.planets.indexOf(planet) + 1);
    planetName.setText(name);

    if (planetCentre == null) {
      // this is probably because the SolarSystemView probably hasn't rendered yet. We'll
      // just ignore this then cause it'll fire an onPlanetSelected when it finishes
      // drawing.
      congenialityContainer.setVisibility(View.GONE);
    } else {
      float pixelScale = getResources().getDisplayMetrics().density;
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
        refreshNativeColonyDetails();
      } else {
        Empire colonyEmpire = EmpireManager.i.getEmpire(planet.colony.empire_id);
        if (colonyEmpire != null) {
          Empire myEmpire = EmpireManager.i.getMyEmpire();
          if (myEmpire.id.equals(colonyEmpire.id)) {
            colonyDetailsContainer.setVisibility(View.VISIBLE);
            refreshColonyDetails();
          } else {
            enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
            refreshEnemyColonyDetails(colonyEmpire);
          }
        } else {
          // TODO: wait for the empire to come in.
        }
      }
    }
  }

  private void refreshUncolonizedDetails() {
    populationCountTextView.setText(getString(R.string.uncolonized));
  }

  private void refreshColonyDetails() {
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

  private void refreshEnemyColonyDetails(Empire empire) {
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

  private void refreshNativeColonyDetails() {
    String pop = "Pop: "
        + Math.round(planet.colony.population);
    populationCountTextView.setText(Html.fromHtml(pop));
    colonyFocusView.setVisibility(View.GONE);
  }
}
