package au.com.codeka.warworlds.game.solarsystem;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.game.CombatReportDialog;
import au.com.codeka.warworlds.game.ScoutReportDialog;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.game.build.BuildActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * This is a fragment which displays details about a single solar system.
 */
public class SolarSystemFragment extends Fragment
                                 implements StarManager.StarFetchedHandler,
                                            EmpireShieldManager.EmpireShieldUpdatedHandler {
    private static Logger log = LoggerFactory.getLogger(SolarSystemFragment.class);
    private SolarSystemSurfaceView mSolarSystemSurfaceView;
    private ProgressBar mProgressBar;
    private StarSummary mStarSummary;
    private Star mStar;
    private Planet mPlanet;
    private Colony mColony;
    private boolean mIsFirstRefresh;
    private View mView;

    // needs to be Object so we can do a version check before instantiating the class
    Object mSolarSystemSurfaceViewOnLayoutChangedListener;

    private static final int BUILD_REQUEST = 3000;

    public Star getStar() {
        return mStar;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.solarsystem, container, false);

        mSolarSystemSurfaceView = (SolarSystemSurfaceView) mView.findViewById(R.id.solarsystem_view);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progress_bar);
        final Button buildButton = (Button) mView.findViewById(R.id.solarsystem_colony_build);
        final Button focusButton = (Button) mView.findViewById(R.id.solarsystem_colony_focus);
        final Button sitrepButton = (Button) mView.findViewById(R.id.sitrep_btn);
        final Button planetViewButton = (Button) mView.findViewById(R.id.enemy_empire_view);
        final Button emptyViewButton = (Button) mView.findViewById(R.id.empty_view_btn);
        final FleetListSimple fleetList = (FleetListSimple) mView.findViewById(R.id.fleet_list);
        final SelectionView selectionView = (SelectionView) mView.findViewById(R.id.selection);
        mSolarSystemSurfaceView.setSelectionView(selectionView);

        mIsFirstRefresh = true;
        if (savedInstanceState != null) {
            mIsFirstRefresh = savedInstanceState.getBoolean("au.com.codeka.warworlds.IsFirstRefresh");
        }

        mSolarSystemSurfaceView.addPlanetSelectedListener(
                new SolarSystemSurfaceView.OnPlanetSelectedListener() {
            @Override
            public void onPlanetSelected(Planet planet) {
                mPlanet = planet;
                refreshSelectedPlanet();
            }
        });

        buildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStar == null) {
                    return; // can happen before the star loads
                }

                Intent intent = new Intent(getActivity(), BuildActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());

                Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
                mColony.toProtocolBuffer(colony_pb);
                intent.putExtra("au.com.codeka.warworlds.Colony", colony_pb.build().toByteArray());

                startActivityForResult(intent, BUILD_REQUEST);
            }
        });

        focusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStar == null || mStar.getPlanets() == null) {
                    return;
                }

                FocusDialog dialog = new FocusDialog();
                dialog.setColony(mStar, mColony);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }
        });

        sitrepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStar == null) {
                    return; // can happen before the star loads
                }

                Intent intent = new Intent(getActivity(), SitrepActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
                startActivity(intent);
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
                Intent intent = new Intent(getActivity(), FleetActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
                intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
                startActivity(intent);
            }
        });

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        long starID = args.getLong("au.com.codeka.warworlds.StarID");
        String starKey = Long.toString(starID);
        StarManager.getInstance().addStarUpdatedListener(starKey, this);
        EmpireShieldManager.i.addEmpireShieldUpdatedHandler(this);

        // get as much details about the star as we can, until it gets refreshes anyway.
        mStarSummary = StarManager.getInstance().getStarSummaryNoFetch(starKey, Float.MAX_VALUE);
        StarManager.getInstance().requestStar(starKey, false, this);

        refreshSelectedPlanet();

        // this will, on HONEYCOMB+ re-centre the progress back over the solarsystem. It looks
        // better...
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            mSolarSystemSurfaceViewOnLayoutChangedListener = new View.OnLayoutChangeListener() {
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
                    (View.OnLayoutChangeListener) mSolarSystemSurfaceViewOnLayoutChangedListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        StarManager.getInstance().removeStarUpdatedListener(this);
        EmpireShieldManager.i.removeEmpireShieldUpdatedHandler(this);

        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            mSolarSystemSurfaceView.removeOnLayoutChangeListener(
                    (View.OnLayoutChangeListener) mSolarSystemSurfaceViewOnLayoutChangedListener);
        }
    }

    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onEmpireShieldUpdated(int empireID) {
        refreshSelectedPlanet();
    }

    @Override
    public void onStarFetched(Star star) {
        log.debug("Star refreshed...");

        // if we don't have a star yet, we'll need to figure out which planet to select
        // initially from the intent that started us. Otherwise, we'll want to select
        // whatever planet we have currently
        int selectedPlanetIndex;
        if (mIsFirstRefresh) {
            Bundle extras = getArguments();
            selectedPlanetIndex = extras.getInt("au.com.codeka.warworlds.PlanetIndex");
        } else if (mPlanet != null) {
            selectedPlanetIndex = mPlanet.getIndex();
        } else {
            selectedPlanetIndex = -1;
        }

        mSolarSystemSurfaceView.setStar(star);
        if (selectedPlanetIndex >= 0) {
            log.debug("Selecting planet #"+selectedPlanetIndex);
            mSolarSystemSurfaceView.selectPlanet(selectedPlanetIndex);
        } else {
            log.debug("No planet selected");
            mSolarSystemSurfaceView.redraw();
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

        mStarSummary = mStar = star;
        mPlanet = (Planet) planet;
        mProgressBar.setVisibility(View.GONE);

        refresh();

        if (mPlanet == null) {
            TextView planetNameTextView = (TextView) mView.findViewById(R.id.planet_name);
            planetNameTextView.setText(mStar.getName());
        }

        if (mIsFirstRefresh) {
            mIsFirstRefresh = false;
            Bundle extras = getArguments();
            boolean showScoutReport = extras.getBoolean("au.com.codeka.warworlds.ShowScoutReport");
            if (showScoutReport) {
                ScoutReportDialog dialog = new ScoutReportDialog();
                dialog.setStar(mStar);
                dialog.show(getFragmentManager(), "");
            }

            String combatReportKey = extras.getString("au.com.codeka.warworlds.CombatReportKey");
            if (!showScoutReport && combatReportKey != null) {
                CombatReportDialog dialog = new CombatReportDialog();
                dialog.loadCombatReport(mStar, combatReportKey);
                dialog.show(getFragmentManager(), "");
            }
        }
    }

    private void refresh() {
        TextView storedGoodsTextView = (TextView) mView.findViewById(R.id.stored_goods);
        TextView deltaGoodsTextView = (TextView) mView.findViewById(R.id.delta_goods);
        View storedGoodsIcon = mView.findViewById(R.id.stored_goods_icon);
        TextView storedMineralsTextView = (TextView) mView.findViewById(R.id.stored_minerals);
        TextView deltaMineralsTextView = (TextView) mView.findViewById(R.id.delta_minerals);
        View storedMineralsIcon = mView.findViewById(R.id.stored_minerals_icon);
        FleetListSimple fleetList = (FleetListSimple) mView.findViewById(R.id.fleet_list);

        fleetList.setStar(mStar);

        BaseEmpirePresence ep = mStar.getEmpire(EmpireManager.i.getEmpire().getKey());
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

            String minerals = String.format(Locale.ENGLISH, "%d / %d", (int)ep.getTotalMinerals(),
                    (int) ep.getMaxMinerals());
            storedMineralsTextView.setText(minerals);

            if (ep.getDeltaGoodsPerHour() >= 0) {
                deltaGoodsTextView.setTextColor(Color.GREEN);
                deltaGoodsTextView.setText(String.format("+%d/hr", (int) ep.getDeltaGoodsPerHour()));
            } else {
                deltaGoodsTextView.setTextColor(Color.RED);
                deltaGoodsTextView.setText(String.format("%d/hr", (int) ep.getDeltaGoodsPerHour()));
            }
            if (ep.getDeltaMineralsPerHour() >= 0) {
                deltaMineralsTextView.setTextColor(Color.GREEN);
                deltaMineralsTextView.setText(String.format("+%d/hr", (int) ep.getDeltaMineralsPerHour()));
            } else {
                deltaMineralsTextView.setTextColor(Color.RED);
                deltaMineralsTextView.setText(String.format("%d/hr", (int) ep.getDeltaMineralsPerHour()));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("au.com.codeka.warworlds.IsFirstRefresh", false);
    }

    private void onViewColony() {
        if (mPlanet == null) {
            return;
        }

        // TODO: determine if enemy colony or not...
        Intent intent;
        if (mColony != null) {
            intent = new Intent(getActivity(), EnemyPlanetActivity.class);
        } else {
            intent = new Intent(getActivity(), EmptyPlanetActivity.class);
        }
        intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
        intent.putExtra("au.com.codeka.warworlds.PlanetIndex", mPlanet.getIndex());
        startActivity(intent);
    }

    private void refreshSelectedPlanet() {
        log.debug("refreshing selected planet...");

        TextView planetNameTextView = (TextView) mView.findViewById(R.id.planet_name);

        if (mStarSummary != null && mPlanet == null) {
            planetNameTextView.setText(mStarSummary.getName());
            return;
        }
        if (mStar == null || mPlanet == null) {
            return;
        }

        mColony = null;
        for (BaseColony colony : mStar.getColonies()) {
            if (colony.getPlanetIndex() == mPlanet.getIndex()) {
                mColony = (Colony) colony;
                break;
            }
        }

        Vector2 planetCentre = mSolarSystemSurfaceView.getPlanetCentre(mPlanet);

        String planetName = mStar.getName()+" "+RomanNumeralFormatter.format(mPlanet.getIndex());
        planetNameTextView.setText(planetName);

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
        }

        ProgressBar populationCongenialityProgressBar = (ProgressBar) mView.findViewById(
                R.id.solarsystem_population_congeniality);
        TextView populationCongenialityTextView = (TextView) mView.findViewById(
                R.id.solarsystem_population_congeniality_value);
        populationCongenialityTextView.setText(Integer.toString(
                mPlanet.getPopulationCongeniality()));
        populationCongenialityProgressBar.setProgress(
                (int) (populationCongenialityProgressBar.getMax() * (mPlanet.getPopulationCongeniality() / 1000.0)));

        ProgressBar farmingCongenialityProgressBar = (ProgressBar) mView.findViewById(
                R.id.solarsystem_farming_congeniality);
        TextView farmingCongenialityTextView = (TextView) mView.findViewById(
                R.id.solarsystem_farming_congeniality_value);
        farmingCongenialityTextView.setText(Integer.toString(
                mPlanet.getFarmingCongeniality()));
        farmingCongenialityProgressBar.setProgress(
                (int)(farmingCongenialityProgressBar.getMax() * (mPlanet.getFarmingCongeniality() / 100.0)));

        ProgressBar miningCongenialityProgressBar = (ProgressBar) mView.findViewById(
                R.id.solarsystem_mining_congeniality);
        TextView miningCongenialityTextView = (TextView) mView.findViewById(
                R.id.solarsystem_mining_congeniality_value);
        miningCongenialityTextView.setText(Integer.toString(
                mPlanet.getMiningCongeniality()));
        miningCongenialityProgressBar.setProgress(
                (int)(miningCongenialityProgressBar.getMax() * (mPlanet.getMiningCongeniality() / 100.0)));

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

            EmpireManager.i.fetchEmpire(mColony.getEmpireKey(),
                new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        Empire thisEmpire = EmpireManager.i.getEmpire();
                        if (thisEmpire.getKey().equals(empire.getKey())) {
                            colonyDetailsContainer.setVisibility(View.VISIBLE);
                            refreshColonyDetails();
                        } else {
                            enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
                            refreshEnemyColonyDetails(empire);
                        }
                    }
                });
        }
    }

    private void refreshUncolonizedDetails() {
        final TextView populationCountTextView = (TextView) mView.findViewById(
                R.id.population_count);
        populationCountTextView.setText("Uncolonized");
    }

    private void refreshColonyDetails() {
        final TextView populationCountTextView = (TextView) mView.findViewById(
                R.id.population_count);
        populationCountTextView.setText(String.format("Pop: %d / %d",
                (int) mColony.getPopulation(), (int) mColony.getMaxPopulation()));

        final ColonyFocusView colonyFocusView = (ColonyFocusView) mView.findViewById(
                R.id.colony_focus_view);
        colonyFocusView.refresh(mStar, mColony);
    }

    private void refreshEnemyColonyDetails(Empire empire) {
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
    }
}
