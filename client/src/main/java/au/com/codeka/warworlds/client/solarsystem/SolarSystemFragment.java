package au.com.codeka.warworlds.client.solarsystem;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.ctrl.FleetListSimple;
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

/**
 * This is a fragment which displays details about a single solar system.
 */
public class SolarSystemFragment extends BaseFragment {
  private static final Log log = new Log("SolarSystemFragment");

  private static final int BUILD_REQUEST = 3000;
  private static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private Planet planet;
  private boolean isFirstRefresh;
  private long starID;

  TextView planetName;
  TextView storedGoods;
  TextView deltaGoods;
  View storedGoodsIcon;
  TextView storedMinerals;
  TextView deltaMinerals;
  View storedMineralsIcon;
  FleetListSimple fleetList;

  // needs to be Object so we can do a version check before instantiating the class
  Object solarSystemSurfaceViewOnLayoutChangedListener;

  public static Bundle createArguments(long starID) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starID);
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

//    mSolarSystemSurfaceView = (SolarSystemSurfaceView) mView.findViewById(R.id.solarsystem_view);
//    mProgressBar = (ProgressBar) mView.findViewById(R.id.progress_bar);
    final Button buildButton = (Button) view.findViewById(R.id.solarsystem_colony_build);
    final Button focusButton = (Button) view.findViewById(R.id.solarsystem_colony_focus);
    final Button sitrepButton = (Button) view.findViewById(R.id.sitrep_btn);
    final Button planetViewButton = (Button) view.findViewById(R.id.enemy_empire_view);
    final Button emptyViewButton = (Button) view.findViewById(R.id.empty_view_btn);
    planetName = (TextView) view.findViewById(R.id.planet_name);
    storedGoods = (TextView) view.findViewById(R.id.stored_goods);
    deltaGoods = (TextView) view.findViewById(R.id.delta_goods);
    storedGoodsIcon = view.findViewById(R.id.stored_goods_icon);
    storedMinerals = (TextView) view.findViewById(R.id.stored_minerals);
    deltaMinerals = (TextView) view.findViewById(R.id.delta_minerals);
    storedMineralsIcon = view.findViewById(R.id.stored_minerals_icon);
    fleetList = (FleetListSimple) view.findViewById(R.id.fleet_list);
    //final SelectionView selectionView = (SelectionView) mView.findViewById(R.id.selection);
    //mSolarSystemSurfaceView.setSelectionView(selectionView);

    isFirstRefresh = true;
    if (savedInstanceState != null) {
      isFirstRefresh = savedInstanceState.getBoolean("au.com.codeka.warworlds.IsFirstRefresh");
    }

    //mSolarSystemSurfaceView.addPlanetSelectedListener(
    //    new SolarSystemSurfaceView.OnPlanetSelectedListener() {
    //      @Override
    //      public void onPlanetSelected(Planet planet) {
    //        SolarSystemFragment.this.planet = planet;
    //        refreshSelectedPlanet();
    //      }
    //    });

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

//        FocusDialog dialog = new FocusDialog();
//        dialog.setColony(star, mColony);
//        dialog.show(getActivity().getSupportFragmentManager(), "");
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

/*    // this will, on HONEYCOMB+ re-centre the progress back over the solarsystem. It looks better...
    int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      solarSystemSurfaceViewOnLayoutChangedListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
            int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
          int containerWidth = right - left;
          int containerHeight = bottom - top;
          int progressWidth = (int)(50 * mSolarSystemSurfaceView.getPixelScale());
          int progressHeight = progressWidth;

          RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mProgressBar.getLayoutParams();
          lp.leftMargin = (containerWidth / 2) - (progressWidth / 2);
          lp.topMargin = (containerHeight / 2) - (progressHeight / 2);
          mProgressBar.setLayoutParams(lp);
        }
      };

      mSolarSystemSurfaceView.addOnLayoutChangeListener(
          (View.OnLayoutChangeListener) solarSystemSurfaceViewOnLayoutChangedListener);
    }*/
  }

  @SuppressLint("NewApi")
  @Override
  public void onPause() {
    super.onPause();
    App.i.getEventBus().unregister(eventHandler);

   /* int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      mSolarSystemSurfaceView.removeOnLayoutChangeListener(
          (View.OnLayoutChangeListener) solarSystemSurfaceViewOnLayoutChangedListener);
    }*/
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
    //if (StarSimulationQueue.needsSimulation(star)) {
     // StarSimulationQueue.i.simulate(star, true);
    //}

    // if we don't have a star yet, we'll need to figure out which planet to select
    // initially from the intent that started us. Otherwise, we'll want to select
    // whatever planet we have currently
    int selectedPlanetIndex;
    if (isFirstRefresh) {
      Bundle extras = getArguments();
      selectedPlanetIndex = extras.getInt(PLANET_INDEX_KEY, -1);
    } else {
      selectedPlanetIndex = -1;
    }

    //mSolarSystemSurfaceView.setStar(star);
    if (selectedPlanetIndex >= 0) {
      log.debug("Selecting planet #%d", selectedPlanetIndex);
      //mSolarSystemSurfaceView.selectPlanet(selectedPlanetIndex);
    } else {
      log.debug("No planet selected");
      //mSolarSystemSurfaceView.redraw();
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
      if (s.empire_id.equals(myEmpire.id)) {
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

      if (storage.goods_delta_per_hour > 0) {
        deltaGoods.setTextColor(Color.GREEN);
        deltaGoods.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(storage.goods_delta_per_hour)));
      } else {
        deltaGoods.setTextColor(Color.RED);
        deltaGoods.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(storage.goods_delta_per_hour)));
      }
      if (storage.minerals_delta_per_hour >= 0) {
        deltaMinerals.setTextColor(Color.GREEN);
        deltaMinerals.setText(String.format(Locale.ENGLISH, "+%d/hr",
            Math.round(storage.minerals_delta_per_hour)));
      } else {
        deltaMinerals.setTextColor(Color.RED);
        deltaMinerals.setText(String.format(Locale.ENGLISH, "%d/hr",
            Math.round(storage.minerals_delta_per_hour)));
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

    // TODO: determine if enemy colony or not...
    //Intent intent;
    //if (mColony != null) {
    //  intent = new Intent(getActivity(), EnemyPlanetActivity.class);
    //} else {
    //  intent = new Intent(getActivity(), EmptyPlanetActivity.class);
    //}
    //intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
    //intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planet.getIndex());
    //startActivity(intent);
  }

  private void refreshSelectedPlanet() {
    if (star == null || planet == null) {
      return;
    }

    //Vector2 planetCentre = mSolarSystemSurfaceView.getPlanetCentre(planet);

    String name = star.name+" "/*+RomanNumeralFormatter.format(planet.getIndex())*/;
    planetName.setText(name);
/*
    View congenialityContainer = mView.findViewById(R.id.congeniality_container);
    if (planetCentre == null) {
      // this is probably because the SolarSystemView probably hasn't rendered yet. We'll
      // just ignore this then cause it'll fire an onPlanetSelected when it finishes
      // drawing.
      congenialityContainer.setVisibility(View.GONE);
    } else {
      float pixelScale = mSolarSystemSurfaceView.getPixelScale();
      double x = planetCentre.x * pixelScale;
      double y = planetCentre.y * pixelScale;

      // hard-coded size of the congeniality container: 85x34 dp
      float offsetX = (85 + 20) * pixelScale;
      float offsetY = (34 + 20) * pixelScale;

      if (x - offsetX < 0) {
        offsetX  = -(20 * pixelScale);
      }
      if (y - offsetY < 20) {
        offsetY = -(20 * pixelScale);
      }

      RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) congenialityContainer.getLayoutParams();
      params.leftMargin = (int) (x - offsetX);
      params.topMargin = (int) (y - offsetY);
      if (params.topMargin < (40 * pixelScale)) {
        params.topMargin = (int)(40 * pixelScale);
      }

      congenialityContainer.setLayoutParams(params);
      congenialityContainer.setVisibility(View.VISIBLE);
    }*/
/*
    ProgressBar populationCongenialityProgressBar = (ProgressBar) mView.findViewById(
        R.id.solarsystem_population_congeniality);
    TextView populationCongenialityTextView = (TextView) mView.findViewById(
        R.id.solarsystem_population_congeniality_value);
    populationCongenialityTextView.setText(Integer.toString(
        planet.getPopulationCongeniality()));
    populationCongenialityProgressBar.setProgress(
        (int) (populationCongenialityProgressBar.getMax() * (planet.getPopulationCongeniality() / 1000.0)));

    ProgressBar farmingCongenialityProgressBar = (ProgressBar) mView.findViewById(
        R.id.solarsystem_farming_congeniality);
    TextView farmingCongenialityTextView = (TextView) mView.findViewById(
        R.id.solarsystem_farming_congeniality_value);
    farmingCongenialityTextView.setText(Integer.toString(
        planet.getFarmingCongeniality()));
    farmingCongenialityProgressBar.setProgress(
        (int)(farmingCongenialityProgressBar.getMax() * (planet.getFarmingCongeniality() / 100.0)));

    ProgressBar miningCongenialityProgressBar = (ProgressBar) mView.findViewById(
        R.id.solarsystem_mining_congeniality);
    TextView miningCongenialityTextView = (TextView) mView.findViewById(
        R.id.solarsystem_mining_congeniality_value);
    miningCongenialityTextView.setText(Integer.toString(
        planet.getMiningCongeniality()));
    miningCongenialityProgressBar.setProgress(
        (int)(miningCongenialityProgressBar.getMax() * (planet.getMiningCongeniality() / 100.0)));

    Button emptyViewButton = (Button) mView.findViewById(R.id.empty_view_btn);
    final View colonyDetailsContainer = mView.findViewById(R.id.solarsystem_colony_details);
    final View enemyColonyDetailsContainer = mView.findViewById(R.id.enemy_colony_details);
    if (mColony == null) {
      emptyViewButton.setVisibility(View.VISIBLE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      refreshUncolonizedDetails();
    } else {
      emptyViewButton.setVisibility(View.GONE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      Empire colonyEmpire = EmpireManager.i.getEmpire(mColony.getEmpireID());
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
    }*/
  }

  private void refreshUncolonizedDetails() {
    //final TextView populationCountTextView = (TextView) mView.findViewById(
   //     R.id.population_count);
   // populationCountTextView.setText("Uncolonized");
  }

  private void refreshColonyDetails() {/*
    final TextView populationCountTextView = (TextView) mView.findViewById(
        R.id.population_count);
    populationCountTextView.setText(String.format("Pop: %d / %d",
        (int) mColony.getPopulation(), (int) mColony.getMaxPopulation()));

    final ColonyFocusView colonyFocusView = (ColonyFocusView) mView.findViewById(
        R.id.colony_focus_view);
    colonyFocusView.refresh(star, mColony);
  */}

  private void refreshEnemyColonyDetails(Empire empire) {/*
    final TextView populationCountTextView = (TextView) mView.findViewById(
        R.id.population_count);
    populationCountTextView.setText(String.format("Population: %d",
        (int) mColony.getPopulation()));

    ImageView enemyIcon = (ImageView) mView.findViewById(R.id.enemy_empire_icon);
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
