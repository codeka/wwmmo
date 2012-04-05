package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
    }

    @Override
    public void onResume() {
        super.onResume();

        setContentView(R.layout.solarsystem);

        final TextView username = (TextView) findViewById(R.id.username);
        final TextView money = (TextView) findViewById(R.id.money);
        mSolarSystemSurfaceView = (SolarSystemSurfaceView) findViewById(R.id.solarsystem_view);
        final Button colonizeButton = (Button) findViewById(R.id.solarsystem_colonize);

        EmpireManager empireManager = EmpireManager.getInstance();
        username.setText(empireManager.getEmpire().getDisplayName());
        money.setText("$ 12,345"); // TODO: empire.getCash()
        colonizeButton.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSectorX = extras.getLong("au.com.codeka.warworlds.SectorX");
            mSectorY = extras.getLong("au.com.codeka.warworlds.SectorY");
            mStarKey = extras.getString("au.com.codeka.warworlds.StarKey");
            String selectedPlanetKey = extras.getString("au.com.codeka.warworlds.PlanetKey");

            refreshStar(selectedPlanetKey);
        }

        mSolarSystemSurfaceView.addPlanetSelectedListener(
                new SolarSystemSurfaceView.OnPlanetSelectedListener() {
            @Override
            public void onPlanetSelected(Planet planet) {
                colonizeButton.setVisibility(View.VISIBLE);
            }
        });

        colonizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onColonizeClick();
            }
        });
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
            public void onStarFetched(Star s) {
                mSolarSystemSurfaceView.setStar(s);
                if (selectedPlanetKey != null) {
                    mSolarSystemSurfaceView.selectPlanet(selectedPlanetKey);
                } else {
                    mSolarSystemSurfaceView.redraw();
                }
            }
        });
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
}
