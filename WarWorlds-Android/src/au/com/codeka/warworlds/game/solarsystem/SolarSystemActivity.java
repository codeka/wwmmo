package au.com.codeka.warworlds.game.solarsystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.Point2D;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends Activity implements StarManager.StarFetchedHandler {
    private static Logger log = LoggerFactory.getLogger(SolarSystemActivity.class);
    private SolarSystemSurfaceView mSolarSystemSurfaceView;
    private boolean mIsSectorUpdated;
    private Star mStar;
    private Planet mPlanet;
    private Colony mColony;

    private static final int BUILD_REQUEST = 3000;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.solarsystem);

        final TextView username = (TextView) findViewById(R.id.username);
        mSolarSystemSurfaceView = (SolarSystemSurfaceView) findViewById(R.id.solarsystem_view);
        final Button colonizeButton = (Button) findViewById(R.id.solarsystem_colonize);
        final Button buildButton = (Button) findViewById(R.id.solarsystem_colony_build);
        final Button focusButton = (Button) findViewById(R.id.solarsystem_colony_focus);
        final Button fleetButton = (Button) findViewById(R.id.fleet_btn);
        final View congenialityContainer = findViewById(R.id.congeniality_container);

        EmpireManager empireManager = EmpireManager.getInstance();
        username.setText(empireManager.getEmpire().getDisplayName());
        congenialityContainer.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");

        StarManager.getInstance().requestStar(starKey, true, this);
        StarManager.getInstance().addStarUpdatedListener(starKey, this);

        mSolarSystemSurfaceView.addPlanetSelectedListener(
                new SolarSystemSurfaceView.OnPlanetSelectedListener() {
            @Override
            public void onPlanetSelected(Planet planet) {
                mPlanet = planet;
                refreshSelectedPlanet();
            }
        });

        colonizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onColonizeClick();
            }
        });

        buildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SolarSystemActivity.this, BuildActivity.class);
                intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
                intent.putExtra("au.com.codeka.warworlds.Colony", mColony);

                startActivityForResult(intent, BUILD_REQUEST);
            }
        });

        focusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("au.com.codeka.warworlds.StarKey", mStar.getKey());
                args.putParcelable("au.com.codeka.warworlds.Colony", mColony);

                DialogManager.getInstance().show(SolarSystemActivity.this, FocusDialog.class, args);
            }
        });

        fleetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("au.com.codeka.warworlds.StarKey", mStar.getKey());
                DialogManager.getInstance().show(SolarSystemActivity.this, FleetDialog.class, args);
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

        Planet planet = null;
        if (selectedPlanetIndex >= 0) {
            for (Planet p : star.getPlanets()) {
                if (p.getIndex() == selectedPlanetIndex) {
                    planet = p;
                    break;
                }
            }
        }

        TextView storedGoodsTextView = (TextView) findViewById(R.id.stored_goods);
        View storedGoodsIcon = findViewById(R.id.stored_goods_icon);
        TextView storedMineralsTextView = (TextView) findViewById(R.id.stored_minerals);
        View storedMineralsIcon = findViewById(R.id.stored_minerals_icon);

        EmpirePresence ep = star.getEmpire(EmpireManager.getInstance().getEmpire().getKey());
        if (ep == null) {
            storedGoodsTextView.setVisibility(View.GONE);
            storedGoodsIcon.setVisibility(View.GONE);
            storedMineralsTextView.setVisibility(View.GONE);
            storedMineralsIcon.setVisibility(View.GONE);
        } else {
            storedGoodsTextView.setVisibility(View.VISIBLE);
            storedGoodsIcon.setVisibility(View.VISIBLE);
            storedMineralsTextView.setVisibility(View.VISIBLE);
            storedMineralsIcon.setVisibility(View.VISIBLE);

            storedGoodsTextView.setText(Integer.toString((int) ep.getTotalGoods()));
            storedMineralsTextView.setText(Integer.toString((int) ep.getTotalMinerals()));
        }

        mStar = star;
        mPlanet = planet;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("au.com.codeka.warworlds.SectorUpdated", mIsSectorUpdated);
        intent.putExtra("au.com.codeka.warworlds.SectorX", mStar.getSectorX());
        intent.putExtra("au.com.codeka.warworlds.SectorY", mStar.getSectorY());
        intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d = DialogManager.getInstance().onCreateDialog(this, id);
        if (d == null)
            d = super.onCreateDialog(id);
        return d;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d, Bundle args) {
        DialogManager.getInstance().onPrepareDialog(this, id, d, args);
        super.onPrepareDialog(id, d, args);
    }

    private void refreshSelectedPlanet() {
        log.debug("refreshing selected planet...");

        if (mStar == null || mPlanet == null) {
            return;
        }

        mColony = null;
        for (Colony colony : mStar.getColonies()) {
            if (colony.getPlanetIndex() == mPlanet.getIndex()) {
                mColony = colony;
                log.debug("Planet has colony "+mColony.getKey()+" on it.");
                break;
            }
        }

        Point2D planetCentre = mSolarSystemSurfaceView.getPlanetCentre(mPlanet);

        String planetName = mStar.getName()+" "+RomanNumeralFormatter.format(mPlanet.getIndex());
        TextView planetNameTextView = (TextView) findViewById(R.id.planet_name);
        planetNameTextView.setText(planetName);

        View congenialityContainer = findViewById(R.id.congeniality_container);
        if (planetCentre == null) {
            // this is probably because the SolarSystemView probably hasn't rendered yet. We'll
            // just ignore this then cause it'll fire an onPlanetSelected when it finishes
            // drawing.
        } else {
            double x = planetCentre.getX() * mSolarSystemSurfaceView.getPixelScale();
            double y = planetCentre.getY() * mSolarSystemSurfaceView.getPixelScale();

            // hard-coded size of the congeniality container: 85x34 dp
            float offsetX = (85 + 20) * mSolarSystemSurfaceView.getPixelScale();
            float offsetY = (34 + 20) * mSolarSystemSurfaceView.getPixelScale();

            if (x - offsetX < 0) {
                offsetX  = -(20 * mSolarSystemSurfaceView.getPixelScale());
            }
            if (y - offsetY < 0) {
                offsetY = -(20 * mSolarSystemSurfaceView.getPixelScale());
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) congenialityContainer.getLayoutParams();
            params.leftMargin = (int) (x - offsetX);
            params.topMargin = (int) (y - offsetY);

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

        Button colonizeButton = (Button) findViewById(R.id.solarsystem_colonize);
        final View colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details);
        if (mColony == null) {
            colonizeButton.setVisibility(View.VISIBLE);
            colonyDetailsContainer.setVisibility(View.GONE);
        } else {
            colonizeButton.setVisibility(View.GONE);
            colonyDetailsContainer.setVisibility(View.GONE);

            EmpireManager.getInstance().fetchEmpire(mColony.getEmpireKey(),
                new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        Empire thisEmpire = EmpireManager.getInstance().getEmpire();
                        if (thisEmpire.getKey().equals(empire.getKey())) {
                            log.debug("refreshing colony details...");

                            colonyDetailsContainer.setVisibility(View.VISIBLE);
                            refreshColonyDetails();
                        } else {
                            // it's not our colony...
                        }
                    }
                });
        }
    }

    private void refreshColonyDetails() {
        final TextView populationCountTextView = (TextView) findViewById(
                R.id.population_count);
        populationCountTextView.setText(String.format("Population: %d",
                                                      (int) mColony.getPopulation()));

        ProgressBar populationFocus = (ProgressBar) findViewById(
                R.id.solarsystem_colony_population_focus);
        populationFocus.setProgress((int)(100.0f * mColony.getPopulationFocus()));
        TextView populationValue = (TextView) findViewById(
                R.id.solarsystem_colony_population_value);
        populationValue.setText(String.format("%s%d / hr",
                (mColony.getPopulationDelta() > 0 ? "+" : "-"),
                Math.abs((int) mColony.getPopulationDelta())));

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
        constructionValue.setText("todo");
    }

    private void onColonizeClick() {
        Planet planet = mSolarSystemSurfaceView.getSelectedPlanet();
        if (planet == null) {
            return;
        }

        EmpireManager.getInstance().getEmpire().colonize(planet, new MyEmpire.ColonizeCompleteHandler() {
            @Override
            public void onColonizeComplete(Colony colony) {
                // remember that the sector we're in has now been updated so we can pass that
                // back to the StarfieldActivity
                mIsSectorUpdated = true;
            }
        });
    }
}
