package au.com.codeka.warworlds.game.solarsystem;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.ctrl.FleetListSimple;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.game.CombatReportDialog;
import au.com.codeka.warworlds.game.ScoutReportDialog;
import au.com.codeka.warworlds.game.SitrepActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends BaseActivity implements StarManager.StarFetchedHandler {
    private static Logger log = LoggerFactory.getLogger(SolarSystemActivity.class);
    private SolarSystemSurfaceView mSolarSystemSurfaceView;
    private Context mContext = this;
    private Star mStar;
    private Planet mPlanet;
    private Colony mColony;
    private boolean mIsFirstRefresh;

    private static final int BUILD_REQUEST = 3000;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.solarsystem);

        mSolarSystemSurfaceView = (SolarSystemSurfaceView) findViewById(R.id.solarsystem_view);
        final Button buildButton = (Button) findViewById(R.id.solarsystem_colony_build);
        final Button focusButton = (Button) findViewById(R.id.solarsystem_colony_focus);
        final Button sitrepButton = (Button) findViewById(R.id.sitrep_btn);
        final Button planetViewButton = (Button) findViewById(R.id.enemy_empire_view);
        final Button emptyViewButton = (Button) findViewById(R.id.empty_view_btn);
        final FleetListSimple fleetList = (FleetListSimple) findViewById(R.id.fleet_list);
        final SelectionView selectionView = (SelectionView) findViewById(R.id.selection);
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

                Intent intent = new Intent(SolarSystemActivity.this, BuildActivity.class);
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
                FocusDialog dialog = new FocusDialog();
                dialog.setColony(mStar, mColony);
                dialog.show(getSupportFragmentManager(), "");
            }
        });

        sitrepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStar == null) {
                    return; // can happen before the star loads
                }

                Intent intent = new Intent(mContext, SitrepActivity.class);
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
                Intent intent = new Intent(SolarSystemActivity.this, FleetActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
                intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("au.com.codeka.warworlds.IsFirstRefresh", false);
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

                StarManager.getInstance().requestStar(SolarSystemActivity.this, starKey, true,
                                                      SolarSystemActivity.this);
                StarManager.getInstance().addStarUpdatedListener(starKey, SolarSystemActivity.this);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    @Override
    public void onStarFetched(Star star) {
        log.debug("Star refreshed...");

        // if we don't have a star yet, we'll need to figure out which planet to select
        // initially from the intent that started us. Otherwise, we'll want to select
        // whatever planet we have currently
        int selectedPlanetIndex;
        if (mStar == null) {
            Bundle extras = getIntent().getExtras();
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

        TextView storedGoodsTextView = (TextView) findViewById(R.id.stored_goods);
        TextView deltaGoodsTextView = (TextView) findViewById(R.id.delta_goods);
        View storedGoodsIcon = findViewById(R.id.stored_goods_icon);
        TextView storedMineralsTextView = (TextView) findViewById(R.id.stored_minerals);
        TextView deltaMineralsTextView = (TextView) findViewById(R.id.delta_minerals);
        View storedMineralsIcon = findViewById(R.id.stored_minerals_icon);
        FleetListSimple fleetList = (FleetListSimple) findViewById(R.id.fleet_list);

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

        mStar = star;
        mPlanet = (Planet) planet;

        if (mIsFirstRefresh) {
            mIsFirstRefresh = false;
            Bundle extras = getIntent().getExtras();
            boolean showScoutReport = extras.getBoolean("au.com.codeka.warworlds.ShowScoutReport");
            if (showScoutReport) {
                ScoutReportDialog dialog = new ScoutReportDialog();
                dialog.setStar(mStar);
                dialog.show(getSupportFragmentManager(), "");
            }

            String combatReportKey = extras.getString("au.com.codeka.warworlds.CombatReportKey");
            if (!showScoutReport && combatReportKey != null) {
                CombatReportDialog dialog = new CombatReportDialog();
                dialog.loadCombatReport(mStar, combatReportKey);
                dialog.show(getSupportFragmentManager(), "");
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if (mStar != null) {
            intent.putExtra("au.com.codeka.warworlds.SectorX", mStar.getSectorX());
            intent.putExtra("au.com.codeka.warworlds.SectorY", mStar.getSectorY());
            intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
        }
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    private void onViewColony() {
        if (mPlanet == null) {
            return;
        }

        // TODO: determine if enemy colony or not...
        Intent intent;
        if (mColony != null) {
            intent = new Intent(this, EnemyPlanetActivity.class);
        } else {
            intent = new Intent(this, EmptyPlanetActivity.class);
        }
        intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
        intent.putExtra("au.com.codeka.warworlds.PlanetIndex", mPlanet.getIndex());
        startActivity(intent);
    }

    private void refreshSelectedPlanet() {
        log.debug("refreshing selected planet...");

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
        TextView planetNameTextView = (TextView) findViewById(R.id.planet_name);
        planetNameTextView.setText(planetName);

        View congenialityContainer = findViewById(R.id.congeniality_container);
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

        ProgressBar populationCongenialityProgressBar = (ProgressBar) findViewById(
                R.id.solarsystem_population_congeniality);
        TextView populationCongenialityTextView = (TextView) findViewById(
                R.id.solarsystem_population_congeniality_value);
        populationCongenialityTextView.setText(Integer.toString(
                mPlanet.getPopulationCongeniality()));
        populationCongenialityProgressBar.setProgress(
                (int) (populationCongenialityProgressBar.getMax() * (mPlanet.getPopulationCongeniality() / 1000.0)));

        ProgressBar farmingCongenialityProgressBar = (ProgressBar) findViewById(
                R.id.solarsystem_farming_congeniality);
        TextView farmingCongenialityTextView = (TextView) findViewById(
                R.id.solarsystem_farming_congeniality_value);
        farmingCongenialityTextView.setText(Integer.toString(
                mPlanet.getFarmingCongeniality()));
        farmingCongenialityProgressBar.setProgress(
                (int)(farmingCongenialityProgressBar.getMax() * (mPlanet.getFarmingCongeniality() / 100.0)));

        ProgressBar miningCongenialityProgressBar = (ProgressBar) findViewById(
                R.id.solarsystem_mining_congeniality);
        TextView miningCongenialityTextView = (TextView) findViewById(
                R.id.solarsystem_mining_congeniality_value);
        miningCongenialityTextView.setText(Integer.toString(
                mPlanet.getMiningCongeniality()));
        miningCongenialityProgressBar.setProgress(
                (int)(miningCongenialityProgressBar.getMax() * (mPlanet.getMiningCongeniality() / 100.0)));

        Button emptyViewButton = (Button) findViewById(R.id.empty_view_btn);
        final View colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details);
        final View enemyColonyDetailsContainer = findViewById(R.id.enemy_colony_details);
        if (mColony == null) {
            emptyViewButton.setVisibility(View.VISIBLE);
            colonyDetailsContainer.setVisibility(View.GONE);
            enemyColonyDetailsContainer.setVisibility(View.GONE);

            refreshUncolonizedDetails();
        } else {
            emptyViewButton.setVisibility(View.GONE);
            colonyDetailsContainer.setVisibility(View.GONE);
            enemyColonyDetailsContainer.setVisibility(View.GONE);

            EmpireManager.i.fetchEmpire(mContext, mColony.getEmpireKey(),
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
        final TextView populationCountTextView = (TextView) findViewById(
                R.id.population_count);
        populationCountTextView.setText("Uncolonized");
    }

    private void refreshColonyDetails() {
        final TextView populationCountTextView = (TextView) findViewById(
                R.id.population_count);
        populationCountTextView.setText(String.format("Pop: %d / %d",
                (int) mColony.getPopulation(), (int) mColony.getMaxPopulation()));

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

    private void refreshEnemyColonyDetails(Empire empire) {
        final TextView populationCountTextView = (TextView) findViewById(
                R.id.population_count);
        populationCountTextView.setText(String.format("Population: %d",
                (int) mColony.getPopulation()));

        ImageView enemyIcon = (ImageView) findViewById(R.id.enemy_empire_icon);
        TextView enemyName = (TextView) findViewById(R.id.enemy_empire_name);
        TextView enemyDefence = (TextView) findViewById(R.id.enemy_empire_defence);

        int defence = (int) (0.25 * mColony.getPopulation() * mColony.getDefenceBoost());
        if (defence < 1) {
            defence = 1;
        }
        enemyIcon.setImageBitmap(empire.getShield(this));
        enemyName.setText(empire.getDisplayName());
        enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
    }
}
