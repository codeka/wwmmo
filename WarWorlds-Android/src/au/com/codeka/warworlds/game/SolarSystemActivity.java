package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.ModelManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.ModelManager.StarFetchedHandler;
import au.com.codeka.warworlds.model.Star;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 * @author dean@codeka.com.au
 *
 */
public class SolarSystemActivity extends Activity {
    private SolarSystemSurfaceView mSolarSystemSurfaceView;
    private long mSectorX;
    private long mSectorY;
    private String mStarKey;
    private boolean mIsSectorUpdated;

    private Star mStar;
    private Planet mPlanet;
    private Colony mColony;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.solarsystem);

        final TextView username = (TextView) findViewById(R.id.username);
        final TextView money = (TextView) findViewById(R.id.money);
        mSolarSystemSurfaceView = (SolarSystemSurfaceView) findViewById(R.id.solarsystem_view);
        final Button colonizeButton = (Button) findViewById(R.id.solarsystem_colonize);
        final Button buildButton = (Button) findViewById(R.id.solarsystem_colony_build);

        EmpireManager empireManager = EmpireManager.getInstance();
        username.setText(empireManager.getEmpire().getDisplayName());
        money.setText("$ 12,345"); // TODO: empire.getCash()

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
                onBuildClick();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSectorX = extras.getLong("au.com.codeka.warworlds.SectorX");
            mSectorY = extras.getLong("au.com.codeka.warworlds.SectorY");
            mStarKey = extras.getString("au.com.codeka.warworlds.StarKey");
            String selectedPlanetKey = extras.getString("au.com.codeka.warworlds.PlanetKey");

            refreshStar(selectedPlanetKey);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("au.com.codeka.warworlds.SectorUpdated", mIsSectorUpdated);
        intent.putExtra("au.com.codeka.warworlds.SectorX", mSectorX);
        intent.putExtra("au.com.codeka.warworlds.SectorY", mSectorY);
        intent.putExtra("au.com.codeka.warworlds.StarKey", mStarKey);
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    private void refreshStar() {
        String selectedPlanetKey = null;
        Planet selectedPlanet = mSolarSystemSurfaceView.getSelectedPlanet();
        if (selectedPlanet != null) {
            selectedPlanetKey = selectedPlanet.getKey();
        }

        refreshStar(selectedPlanetKey);
    }

    private void refreshStar(final String selectedPlanetKey) {
        ModelManager.requestStar(mSectorX, mSectorY, mStarKey, new StarFetchedHandler() {
            @Override
            public void onStarFetched(Star star) {
                mSolarSystemSurfaceView.setStar(star);
                if (selectedPlanetKey != null) {
                    mSolarSystemSurfaceView.selectPlanet(selectedPlanetKey);
                } else {
                    mSolarSystemSurfaceView.redraw();
                }

                Planet planet = null;
                if (selectedPlanetKey != null) {
                    for (Planet p : star.getPlanets()) {
                        if (p.getKey().equals(selectedPlanetKey)) {
                            planet = p;
                            break;
                        }
                    }
                }

                mStar = star;
                mPlanet = planet;
                refreshSelectedPlanet();
            }
        });
    }

    private void refreshSelectedPlanet() {
        View containerView = findViewById(R.id.solarsystem_planet_properties);
        if (mStar == null || mPlanet == null) {
            containerView.setVisibility(View.GONE);
            return;
        }

        mColony = null;
        for (Colony colony : mStar.getColonies()) {
            if (colony.getPlanetKey().equals(mPlanet.getKey())) {
                mColony = colony;
                break;
            }
        }

        containerView.setVisibility(View.VISIBLE);

        TextView planetNameTextView = (TextView) findViewById(R.id.solarsystem_planetname);
        planetNameTextView.setText(mStar.getName()+" "+numberToRomanNumeral(mPlanet.getIndex()));

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
        View colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details);
        if (mColony == null) {
            colonizeButton.setVisibility(View.VISIBLE);
            colonyDetailsContainer.setVisibility(View.GONE);
        } else {
            colonizeButton.setVisibility(View.GONE);
            colonyDetailsContainer.setVisibility(View.VISIBLE);

            final TextView empireNameTextView = (TextView) findViewById(
                    R.id.solarsystem_colony_empirename);
            empireNameTextView.setText("");
            EmpireManager.getInstance().fetchEmpire(mColony.getEmpireKey(),
                    new EmpireManager.EmpireFetchedHandler() {
                        @Override
                        public void onEmpireFetched(Empire empire) {
                            empireNameTextView.setText(empire.getDisplayName());
                        }
                    });

            TextView populationTextView = (TextView) findViewById(
                    R.id.solarsystem_colony_population_value);
            populationTextView.setText(String.format("%d", mColony.getPopulation()));

            TextView populationRateTextView = (TextView) findViewById(
                    R.id.solarsystem_colony_population_rate);
            populationRateTextView.setText(String.format("%.2f", mColony.getPopulationRate()));

            TextView farmingTextView = (TextView) findViewById(
                    R.id.solarsystem_colony_farming_value);
            farmingTextView.setText(String.format("%.1f", mColony.getFarmingRate() * 10.0));
        }
    }

    private static String numberToRomanNumeral(int n) {
        // TODO: this is dumb..
        switch (n) {
        case 0: return "";
        case 1: return "I";
        case 2: return "II";
        case 3: return "III";
        case 4: return "IV";
        case 5: return "V";
        case 6: return "VI";
        case 7: return "VII";
        case 8: return "VIII";
        case 9: return "IX";
        case 10: return "X";
        default: return "+++";
        }
    }

    private void onColonizeClick() {
        Planet planet = mSolarSystemSurfaceView.getSelectedPlanet();
        if (planet == null) {
            return;
        }

        EmpireManager.getInstance().getEmpire().colonize(planet, new Empire.ColonizeCompleteHandler() {
            @Override
            public void onColonizeComplete(Colony colony) {
                // refresh this page
                refreshStar();

                // remember that the sector we're in has now been updated so we can pass that
                // back to the StarfieldActivity
                mIsSectorUpdated = true;
            }
        });
    }

    /**
     * When you click build, we need to pop up the build window.
     */
    private void onBuildClick() {
        
    }
}
