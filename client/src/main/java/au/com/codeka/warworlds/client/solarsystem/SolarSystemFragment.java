package au.com.codeka.warworlds.client.solarsystem;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.wire.Wire;
import com.transitionseverywhere.TransitionManager;

import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.client.ctrl.FleetListSimple;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.ColonyHelper;

/**
 * This is a fragment which displays details about a single solar system.
 */
public class SolarSystemFragment extends BaseFragment {
  private static final Log log = new Log("SolarSystemFragment");

  private static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private Planet planet;
  private boolean isFirstRefresh;
  private long starID;

  private SolarSystemView solarSystemView;
  private TextView planetName;
  private TextView storedGoods;
  private TextView deltaGoods;
  private View storedGoodsIcon;
  private TextView storedMinerals;
  private TextView deltaMinerals;
  private View storedMineralsIcon;
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
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.frag_solarsystem, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    solarSystemView = (SolarSystemView) view.findViewById(R.id.solarsystem_view);
    final Button buildButton = (Button) view.findViewById(R.id.solarsystem_colony_build);
    final Button focusButton = (Button) view.findViewById(R.id.solarsystem_colony_focus);
    final Button sitrepButton = (Button) view.findViewById(R.id.sitrep_btn);
    final Button planetViewButton = (Button) view.findViewById(R.id.enemy_empire_view);
    planetName = (TextView) view.findViewById(R.id.planet_name);
    storedGoods = (TextView) view.findViewById(R.id.stored_goods);
    deltaGoods = (TextView) view.findViewById(R.id.delta_goods);
    storedGoodsIcon = view.findViewById(R.id.stored_goods_icon);
    storedMinerals = (TextView) view.findViewById(R.id.stored_minerals);
    deltaMinerals = (TextView) view.findViewById(R.id.delta_minerals);
    storedMineralsIcon = view.findViewById(R.id.stored_minerals_icon);
    fleetList = (FleetListSimple) view.findViewById(R.id.fleet_list);
    congenialityContainer = view.findViewById(R.id.congeniality_container);
    populationCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.solarsystem_population_congeniality);
    populationCongenialityTextView = (TextView) view.findViewById(
        R.id.solarsystem_population_congeniality_value);
    farmingCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.solarsystem_farming_congeniality);
    farmingCongenialityTextView = (TextView) view.findViewById(
        R.id.solarsystem_farming_congeniality_value);
    miningCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.solarsystem_mining_congeniality);
    miningCongenialityTextView = (TextView) view.findViewById(
        R.id.solarsystem_mining_congeniality_value);
    energyCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.solarsystem_energy_congeniality);
    energyCongenialityTextView = (TextView) view.findViewById(
        R.id.solarsystem_energy_congeniality_value);
    bottomLeftPane = (ViewGroup) view.findViewById(R.id.bottom_left_pane);
    emptyViewButton = (Button) view.findViewById(R.id.empty_view_btn);
    colonyDetailsContainer = view.findViewById(R.id.solarsystem_colony_details);
    enemyColonyDetailsContainer = view.findViewById(R.id.enemy_colony_details);
    populationCountTextView = (TextView) view.findViewById(R.id.population_count);
    colonyFocusView = (ColonyFocusView) view.findViewById(R.id.colony_focus_view);

    isFirstRefresh = true;
    if (savedInstanceState != null) {
      isFirstRefresh = savedInstanceState.getBoolean("au.com.codeka.warworlds.IsFirstRefresh");
    }

    solarSystemView.setPlanetSelectedHandler(new SolarSystemView.PlanetSelectedHandler() {
        @Override
        public void onPlanetSelected(Planet planet) {
          SolarSystemFragment.this.planet = planet;
          refreshSelectedPlanet();
        }
      });

    buildButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (star == null) {
          return; // can happen before the star loads
        }
        if (planet.colony == null) {
          return; // shouldn't happen, the button should be hidden.
        }

        //Intent intent = new Intent(getActivity(), BuildActivity.class);
        //intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
        //Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
        //mColony.toProtocolBuffer(colony_pb);
        //intent.putExtra("au.com.codeka.warworlds.Colony", colony_pb.build().toByteArray());
        //startActivityForResult(intent, BUILD_REQUEST);
      }
    });

    focusButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (star == null || star.planets == null || planet.colony == null) {
          return;
        }

        getFragmentTransitionManager().replaceFragment(
            PlanetDetailsFragment.class,
            PlanetDetailsFragment.createArguments(star.id, star.planets.indexOf(planet)),
            SharedViewHolder.builder()
                .addSharedView(R.id.bottom_pane, "bottom_pane")
                .build());
      }
    });

    sitrepButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (star == null) {
          return; // can happen before the star loads
        }

//        Intent intent = new Intent(getActivity(), SitrepActivity.class);
//        intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
//        startActivity(intent);
      }
    });

    planetViewButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onViewColony();
      }
    });

    emptyViewButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onViewColony();
      }
    });

    fleetList.setFleetSelectedHandler(new FleetListSimple.FleetSelectedHandler() {
      @Override
      public void onFleetSelected(Fleet fleet) {
//        Intent intent = new Intent(getActivity(), FleetActivity.class);
//        intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
//        intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
 //       startActivity(intent);
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    Bundle args = getArguments();
    starID = args.getLong(STAR_ID_KEY);
    App.i.getEventBus().register(eventHandler);

    log.info("Getting star: %d", starID);
    star = StarManager.i.getStar(starID);
    onStarFetched(star);
  }

  @Override
  public void onPause() {
    super.onPause();
    App.i.getEventBus().unregister(eventHandler);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star star) {
      if (starID == star.id) {
        onStarFetched(star);
      }
    }
  };

  private void onStarFetched(Star star) {
    // If we don't have a star yet, we'll need to figure out which planet to select initially from
    // the arguments that started us. Otherwise, we'll want to select whatever planet we have
    // currently.
    int selectedPlanetIndex;
    if (isFirstRefresh) {
      selectedPlanetIndex = getArguments().getInt(PLANET_INDEX_KEY, -1);
    } else {
      selectedPlanetIndex = -1;
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
      isFirstRefresh = false;
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
      storedGoodsIcon.setVisibility(View.GONE);
      storedMinerals.setVisibility(View.GONE);
      deltaMinerals.setVisibility(View.GONE);
      storedMineralsIcon.setVisibility(View.GONE);
    } else {
      storedGoods.setVisibility(View.VISIBLE);
      deltaGoods.setVisibility(View.VISIBLE);
      storedGoodsIcon.setVisibility(View.VISIBLE);
      storedMinerals.setVisibility(View.VISIBLE);
      deltaMinerals.setVisibility(View.VISIBLE);
      storedMineralsIcon.setVisibility(View.VISIBLE);

      storedGoods.setText(String.format(Locale.ENGLISH, "%d / %d",
          Math.round(storage.total_goods), Math.round(storage.max_goods)));
      storedMinerals.setText(String.format(Locale.ENGLISH, "%d / %d",
          Math.round(storage.total_minerals), Math.round(storage.max_minerals)));

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
    }
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putBoolean("au.com.codeka.warworlds.IsFirstRefresh", false);
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

      Empire colonyEmpire = EmpireManager.i.getEmpire(planet.colony.empire_id);
      if (colonyEmpire != null) {
        Empire myEmpire = EmpireManager.i.getMyEmpire();
        if (myEmpire != null && myEmpire.id.equals(colonyEmpire.id)) {
          colonyDetailsContainer.setVisibility(View.VISIBLE);
          refreshColonyDetails();
        } else {
          enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
          refreshEnemyColonyDetails(colonyEmpire);
        }
      }
    }
  }

  private void refreshUncolonizedDetails() {
    populationCountTextView.setText("Uncolonized");
  }

  private void refreshColonyDetails() {
    StringBuilder pop = new StringBuilder();
    pop.append("Pop: ");
    pop.append(Math.round(planet.colony.population));
    pop.append(" <small>");
    pop.append(String.format(Locale.US, "(%s%d / hr)",
        Wire.get(planet.colony.delta_population, 0.0f) < 0 ? "-" : "+",
        Math.abs(Math.round(Wire.get(planet.colony.delta_population, 0.0f)))));
    pop.append("</small> / ");
    pop.append(ColonyHelper.getMaxPopulation(planet));
    populationCountTextView.setText(Html.fromHtml(pop.toString()));

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
}
