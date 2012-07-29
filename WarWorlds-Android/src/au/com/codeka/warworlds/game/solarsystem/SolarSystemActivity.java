package au.com.codeka.warworlds.game.solarsystem;

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
import au.com.codeka.warworlds.game.UniverseElementActivity;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.Design.DesignKind;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarManager.StarFetchedHandler;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends UniverseElementActivity {
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
        mSolarSystemSurfaceView = (SolarSystemSurfaceView) findViewById(R.id.solarsystem_view);
        final Button colonizeButton = (Button) findViewById(R.id.solarsystem_colonize);
        final Button buildButton = (Button) findViewById(R.id.solarsystem_colony_build);
        final Button focusButton = (Button) findViewById(R.id.solarsystem_colony_focus);
        final Button fleetButton = (Button) findViewById(R.id.fleet_btn);
        final View congenialityContainer = findViewById(R.id.congeniality_container);

        EmpireManager empireManager = EmpireManager.getInstance();
        username.setText(empireManager.getEmpire().getDisplayName());
        congenialityContainer.setVisibility(View.GONE);

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
                showDialog(BuildDialog.ID);
            }
        });

        focusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(FocusDialog.ID);
            }
        });

        fleetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putParcelable("au.com.codeka.warworlds.Star", mStar);
                DialogManager.getInstance().show(SolarSystemActivity.this, FleetDialog.class, args);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mStarKey = null;
        mIsSectorUpdated = false;;
        mStar = null;
        mPlanet = null;
        mColony = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSectorX = extras.getLong("au.com.codeka.warworlds.SectorX");
            mSectorY = extras.getLong("au.com.codeka.warworlds.SectorY");
            mStarKey = extras.getString("au.com.codeka.warworlds.StarKey");
            int selectedPlanetIndex = extras.getInt("au.com.codeka.warworlds.PlanetIndex");

            refreshStar(selectedPlanetIndex);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case BuildDialog.ID:
            return new BuildDialog(this);
        case BuildConfirmDialog.ID:
            return new BuildConfirmDialog(this);
        case FocusDialog.ID:
            return new FocusDialog(this);
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d, Bundle args) {
        switch(id) {
        case BuildConfirmDialog.ID: {
            BuildConfirmDialog dialog = (BuildConfirmDialog) d;
            String designID = args.getString("au.com.codeka.warworlds.DesignID");
            if (designID == null)
                designID = "";
            DesignKind dk = DesignKind.fromInt(args.getInt("au.com.codeka.warworlds.DesignKind",
                                               DesignKind.BUILDING.getValue()));

            // TODO: this could be encapsulated in the DesignManager base class....
            Design design;
            if (dk == DesignKind.BUILDING) {
                design = BuildingDesignManager.getInstance().getDesign(designID);
            } else {
                design = ShipDesignManager.getInstance().getDesign(designID);
            }

            dialog.setDesign(design);
            dialog.setColony(mColony);
            break;
        }
        case BuildDialog.ID: {
            BuildDialog dialog = (BuildDialog) d;
            dialog.setColony(mStar, mColony);
            break;
        }
        case FocusDialog.ID: {
            FocusDialog dialog = (FocusDialog) d;
            dialog.setColony(mColony);
            break;
        }
        }

        super.onPrepareDialog(id, d, args);
    }

    @Override
    public void refresh() {
        int selectedPlanetIndex = -1;
        Planet selectedPlanet = mSolarSystemSurfaceView.getSelectedPlanet();
        if (selectedPlanet != null) {
            selectedPlanetIndex = selectedPlanet.getIndex();
        }

        refreshStar(selectedPlanetIndex);
    }

    private void refreshStar(final int selectedPlanetIndex) {
        StarManager.requestStar(mSectorX, mSectorY, mStarKey, new StarFetchedHandler() {
            @Override
            public void onStarFetched(Star star) {
                mSolarSystemSurfaceView.setStar(star);
                if (selectedPlanetIndex >= 0) {
                    mSolarSystemSurfaceView.selectPlanet(selectedPlanetIndex);
                } else {
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

                fireStarUpdated(mStar, mPlanet, mColony);
            }
        });
    }

    private void refreshSelectedPlanet() {
        if (mStar == null || mPlanet == null) {
            return;
        }

        mColony = null;
        for (Colony colony : mStar.getColonies()) {
            if (colony.getPlanetIndex() == mPlanet.getIndex()) {
                mColony = colony;
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

            float offsetX = congenialityContainer.getWidth() + (20 * mSolarSystemSurfaceView.getPixelScale());
            float offsetY = congenialityContainer.getHeight() + (20 * mSolarSystemSurfaceView.getPixelScale());

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
                // refresh this page
                refresh();

                // remember that the sector we're in has now been updated so we can pass that
                // back to the StarfieldActivity
                mIsSectorUpdated = true;
            }
        });
    }
}
