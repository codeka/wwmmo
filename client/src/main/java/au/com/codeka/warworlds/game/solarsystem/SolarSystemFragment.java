package au.com.codeka.warworlds.game.solarsystem;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.Log;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.CombatReportDialog;
import au.com.codeka.warworlds.game.ScoutReportDialog;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.game.build.BuildActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSimulationQueue;

/**
 * This is a fragment which displays details about a single solar system.
 */
public class SolarSystemFragment extends Fragment {
  private static final Log log = new Log("SolarSystemFragment");
  private SolarSystemSurfaceView solarSystemSurfaceView;
  private ProgressBar progressBar;
  private Star star;
  private Planet planet;
  private Colony colony;
  private boolean isFirstRefresh;
  private View view;
  private int starID;

  // needs to be Object so we can do a version check before instantiating the class
  Object solarSystemSurfaceViewOnLayoutChangedListener;

  public Star getStar() {
    return star;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.solarsystem, container, false);

    solarSystemSurfaceView = view.findViewById(R.id.solarsystem_view);
    progressBar = view.findViewById(R.id.progress_bar);
    final Button viewButton = view.findViewById(R.id.solarsystem_colony_view);
    final Button sitrepButton = view.findViewById(R.id.sitrep_btn);
    final Button planetViewButton = view.findViewById(R.id.enemy_empire_view);
    final Button emptyViewButton = view.findViewById(R.id.empty_view_btn);
    final FleetListSimple fleetList = view.findViewById(R.id.fleet_list);
    final SelectionView selectionView = view.findViewById(R.id.selection);
    solarSystemSurfaceView.setSelectionView(selectionView);

    isFirstRefresh = true;
    if (savedInstanceState != null) {
      isFirstRefresh = savedInstanceState.getBoolean("au.com.codeka.warworlds.IsFirstRefresh");
    }

    solarSystemSurfaceView.addPlanetSelectedListener(
        planet -> {
          SolarSystemFragment.this.planet = planet;
          refreshSelectedPlanet();
        });

    sitrepButton.setOnClickListener(v -> {
      if (star == null) {
        return; // can happen before the star loads
      }

      Intent intent = new Intent(getActivity(), SitrepActivity.class);
      intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
      startActivity(intent);
    });

    planetViewButton.setOnClickListener(v -> onViewColony());
    viewButton.setOnClickListener(v -> onViewColony());
    emptyViewButton.setOnClickListener(v -> onViewColony());

    fleetList.setFleetSelectedHandler(fleet -> {
      Intent intent = new Intent(getActivity(), FleetActivity.class);
      intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
      intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
      startActivity(intent);
    });

    return view;
  }

  @SuppressLint("NewApi")
  @Override
  public void onResume() {
    super.onResume();
    Bundle args = getArguments();
    starID = (int) args.getLong("au.com.codeka.warworlds.StarID");
    StarManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);

    star = StarManager.i.getStar(starID);
    if (star != null) {
      onStarFetched(star);
    }

    // this will, on HONEYCOMB+ re-centre the progress back over the solarsystem. It looks better...
    int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      solarSystemSurfaceViewOnLayoutChangedListener =
          (View.OnLayoutChangeListener) (v, left, top, right, bottom, oldLeft,
                                         oldTop, oldRight, oldBottom) -> {
        int containerWidth = right - left;
        int containerHeight = bottom - top;
        int progressWidth = (int) (50 * solarSystemSurfaceView.getPixelScale());
        int progressHeight = progressWidth;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) progressBar.getLayoutParams();
        lp.leftMargin = (containerWidth / 2) - (progressWidth / 2);
        lp.topMargin = (containerHeight / 2) - (progressHeight / 2);
        progressBar.setLayoutParams(lp);
      };

      solarSystemSurfaceView.addOnLayoutChangeListener(
          (View.OnLayoutChangeListener) solarSystemSurfaceViewOnLayoutChangedListener);
    }
  }

  @SuppressLint("NewApi")
  @Override
  public void onPause() {
    super.onPause();
    StarManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);

    int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      solarSystemSurfaceView.removeOnLayoutChangeListener(
          (View.OnLayoutChangeListener) solarSystemSurfaceViewOnLayoutChangedListener);
    }
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star star) {
      if (starID == star.getID()) {
        onStarFetched(star);
      }
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refreshSelectedPlanet();
    }
  };

  private void onStarFetched(Star star) {
    if (StarSimulationQueue.needsSimulation(star)) {
      StarSimulationQueue.i.simulate(star, true);
    }

    // if we don't have a star yet, we'll need to figure out which planet to select
    // initially from the intent that started us. Otherwise, we'll want to select
    // whatever planet we have currently
    int selectedPlanetIndex;
    if (isFirstRefresh) {
      Bundle extras = getArguments();
      selectedPlanetIndex = extras.getInt("au.com.codeka.warworlds.PlanetIndex");
    } else if (planet != null) {
      selectedPlanetIndex = planet.getIndex();
    } else {
      selectedPlanetIndex = -1;
    }

    solarSystemSurfaceView.setStar(star);
    if (selectedPlanetIndex >= 0) {
      log.debug("Selecting planet #%d", selectedPlanetIndex);
      solarSystemSurfaceView.selectPlanet(selectedPlanetIndex);
    } else {
      log.debug("No planet selected");
      solarSystemSurfaceView.redraw();
    }

    BasePlanet planet = null;
    if (selectedPlanetIndex >= 0) {
      for (BasePlanet p : star.getPlanets()) {
        if (p.getIndex() == selectedPlanetIndex) {
          planet = p;
          break;
        }
      }
    }

    this.star = star;
    this.planet = (Planet) planet;
    progressBar.setVisibility(View.GONE);

    refresh();

    if (this.planet == null) {
      TextView planetNameTextView = (TextView) view.findViewById(R.id.planet_name);
      planetNameTextView.setText(this.star.getName());
    }

    if (isFirstRefresh) {
      isFirstRefresh = false;
      Bundle extras = getArguments();
      boolean showScoutReport = extras.getBoolean("au.com.codeka.warworlds.ShowScoutReport");
      if (showScoutReport) {
        ScoutReportDialog dialog = new ScoutReportDialog();
        dialog.setStar(this.star);
        dialog.show(getFragmentManager(), "");
      }

      String combatReportKey = extras.getString("au.com.codeka.warworlds.CombatReportKey");
      if (!showScoutReport && combatReportKey != null) {
        CombatReportDialog dialog = new CombatReportDialog();
        dialog.loadCombatReport(this.star, combatReportKey);
        dialog.show(getFragmentManager(), "");
      }
    }
  }

  private void refresh() {
    TextView storedGoodsTextView = view.findViewById(R.id.stored_goods);
    TextView deltaGoodsTextView = view.findViewById(R.id.delta_goods);
    View storedGoodsIcon = view.findViewById(R.id.stored_goods_icon);
    TextView storedMineralsTextView = view.findViewById(R.id.stored_minerals);
    TextView deltaMineralsTextView = view.findViewById(R.id.delta_minerals);
    View storedMineralsIcon = view.findViewById(R.id.stored_minerals_icon);
    FleetListSimple fleetList = view.findViewById(R.id.fleet_list);

    fleetList.setStar(star);

    BaseEmpirePresence ep = star.getEmpire(EmpireManager.i.getEmpire().getKey());
    if (ep == null) {
      storedGoodsTextView.setVisibility(View.GONE);
      deltaGoodsTextView.setVisibility(View.GONE);
      storedGoodsIcon.setVisibility(View.GONE);
      storedMineralsTextView.setVisibility(View.GONE);
      deltaMineralsTextView.setVisibility(View.GONE);
      storedMineralsIcon.setVisibility(View.GONE);
    } else {
      storedGoodsTextView.setVisibility(View.VISIBLE);
      deltaGoodsTextView.setVisibility(View.VISIBLE);
      storedGoodsIcon.setVisibility(View.VISIBLE);
      storedMineralsTextView.setVisibility(View.VISIBLE);
      deltaMineralsTextView.setVisibility(View.VISIBLE);
      storedMineralsIcon.setVisibility(View.VISIBLE);

      String goods = String.format(Locale.ENGLISH, "%d / %d", (int) ep.getTotalGoods(),
          (int) ep.getMaxGoods());
      storedGoodsTextView.setText(goods);

      String minerals = String.format(Locale.ENGLISH, "%d / %d", (int) ep.getTotalMinerals(),
          (int) ep.getMaxMinerals());
      storedMineralsTextView.setText(minerals);

      if (ep.getDeltaGoodsPerHour() >= 0) {
        deltaGoodsTextView.setTextColor(Color.GREEN);
        deltaGoodsTextView.setText(
            String.format(Locale.ENGLISH, "+%d/hr", (int) ep.getDeltaGoodsPerHour()));
      } else {
        deltaGoodsTextView.setTextColor(Color.RED);
        deltaGoodsTextView.setText(
            String.format(Locale.ENGLISH, "%d/hr", (int) ep.getDeltaGoodsPerHour()));
      }
      if (ep.getDeltaMineralsPerHour() >= 0) {
        deltaMineralsTextView.setTextColor(Color.GREEN);
        deltaMineralsTextView.setText(
            String.format(Locale.ENGLISH, "+%d/hr", (int) ep.getDeltaMineralsPerHour()));
      } else {
        deltaMineralsTextView.setTextColor(Color.RED);
        deltaMineralsTextView.setText(
            String.format(Locale.ENGLISH, "%d/hr", (int) ep.getDeltaMineralsPerHour()));
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

    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    Intent intent;
    if (colony != null) {
      if (colony.getEmpireID() == myEmpire.getID()) {
        intent = new Intent(getActivity(), OwnedPlanetActivity.class);
      } else {
        intent = new Intent(getActivity(), EnemyPlanetActivity.class);
      }
    } else {
      intent = new Intent(getActivity(), EmptyPlanetActivity.class);
    }
    intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
    intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planet.getIndex());
    startActivity(intent);
  }

  private void refreshSelectedPlanet() {
    TextView planetNameTextView = view.findViewById(R.id.planet_name);

    if (star == null || planet == null) {
      return;
    }

    colony = null;
    for (BaseColony colony : star.getColonies()) {
      if (colony.getPlanetIndex() == planet.getIndex()) {
        this.colony = (Colony) colony;
        break;
      }
    }

    Vector2 planetCentre = solarSystemSurfaceView.getPlanetCentre(planet);

    String planetName = star.getName() + " " + RomanNumeralFormatter.format(planet.getIndex());
    planetNameTextView.setText(planetName);

    View congenialityContainer = view.findViewById(R.id.congeniality_container);
    if (planetCentre == null) {
      // this is probably because the SolarSystemView probably hasn't rendered yet. We'll just
      // ignore this then cause it'll fire an onPlanetSelected when it finishes drawing.
      congenialityContainer.setVisibility(View.GONE);
    } else {
      float pixelScale = solarSystemSurfaceView.getPixelScale();
      double x = planetCentre.x * pixelScale;
      double y = planetCentre.y * pixelScale;

      // hard-coded size of the congeniality container: 85x34 dp
      float offsetX = (85 + 20) * pixelScale;
      float offsetY = (34 + 20) * pixelScale;

      if (x - offsetX < 0) {
        offsetX = -(20 * pixelScale);
      }
      if (y - offsetY < 20) {
        offsetY = -(20 * pixelScale);
      }

      RelativeLayout.LayoutParams params =
          (RelativeLayout.LayoutParams) congenialityContainer.getLayoutParams();
      params.leftMargin = (int) (x - offsetX);
      params.topMargin = (int) (y - offsetY);
      if (params.topMargin < (40 * pixelScale)) {
        params.topMargin = (int) (40 * pixelScale);
      }

      congenialityContainer.setLayoutParams(params);
      congenialityContainer.setVisibility(View.VISIBLE);
    }

    ProgressBar populationCongenialityProgressBar = view.findViewById(
        R.id.solarsystem_population_congeniality);
    TextView populationCongenialityTextView = view.findViewById(
        R.id.solarsystem_population_congeniality_value);
    populationCongenialityTextView.setText(
        String.format(Locale.ENGLISH, "%d", planet.getPopulationCongeniality()));
    populationCongenialityProgressBar.setProgress(
        (int) (populationCongenialityProgressBar.getMax()
            * (planet.getPopulationCongeniality() / 1000.0)));

    ProgressBar farmingCongenialityProgressBar = view.findViewById(
        R.id.solarsystem_farming_congeniality);
    TextView farmingCongenialityTextView = view.findViewById(
        R.id.solarsystem_farming_congeniality_value);
    farmingCongenialityTextView.setText(
        String.format(Locale.ENGLISH, "%d", planet.getFarmingCongeniality()));
    farmingCongenialityProgressBar.setProgress(
        (int) (farmingCongenialityProgressBar.getMax()
            * (planet.getFarmingCongeniality() / 100.0)));

    ProgressBar miningCongenialityProgressBar = view.findViewById(
        R.id.solarsystem_mining_congeniality);
    TextView miningCongenialityTextView = view.findViewById(
        R.id.solarsystem_mining_congeniality_value);
    miningCongenialityTextView.setText(
        String.format(Locale.ENGLISH, "%d", planet.getMiningCongeniality()));
    miningCongenialityProgressBar.setProgress(
        (int) (miningCongenialityProgressBar.getMax() * (planet.getMiningCongeniality() / 100.0)));

    Button emptyViewButton = view.findViewById(R.id.empty_view_btn);
    final View colonyDetailsContainer = view.findViewById(R.id.solarsystem_colony_details);
    final View enemyColonyDetailsContainer = view.findViewById(R.id.enemy_colony_details);
    if (colony == null) {
      emptyViewButton.setVisibility(View.VISIBLE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      refreshUncolonizedDetails();
    } else {
      emptyViewButton.setVisibility(View.GONE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      Empire colonyEmpire = EmpireManager.i.getEmpire(colony.getEmpireID());
      if (colonyEmpire != null) {
        Empire thisEmpire = EmpireManager.i.getEmpire();
        if (thisEmpire.getKey().equals(colonyEmpire.getKey())) {
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
    final TextView populationCountTextView = view.findViewById(
        R.id.population_count);
    populationCountTextView.setText("Uncolonized");
  }

  private void refreshColonyDetails() {
    final TextView populationCountTextView = view.findViewById(
        R.id.population_count);
    populationCountTextView.setText(String.format(Locale.ENGLISH, "Pop: %d / %d",
        (int) colony.getPopulation(), (int) colony.getMaxPopulation()));

    final ColonyFocusView colonyFocusView = view.findViewById(
        R.id.colony_focus_view);
    colonyFocusView.refresh(star, colony);
  }

  private void refreshEnemyColonyDetails(Empire empire) {
    final TextView populationCountTextView = view.findViewById(
        R.id.population_count);
    populationCountTextView.setText(String.format(Locale.ENGLISH, "Population: %d",
        (int) colony.getPopulation()));

    ImageView enemyIcon = view.findViewById(R.id.enemy_empire_icon);
    TextView enemyName = view.findViewById(R.id.enemy_empire_name);
    TextView enemyDefence = view.findViewById(R.id.enemy_empire_defence);

    int defence = (int) (0.25 * colony.getPopulation() * colony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }
    enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
    enemyName.setText(empire.getDisplayName());
    enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
  }
}
