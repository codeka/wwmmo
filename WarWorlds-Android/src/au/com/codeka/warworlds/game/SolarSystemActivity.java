package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
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
            long sectorX = extras.getLong("au.com.codeka.warworlds.SectorX");
            long sectorY = extras.getLong("au.com.codeka.warworlds.SectorY");
            int starID = extras.getInt("au.com.codeka.warworlds.StarID");

            ModelManager.requestStar(sectorX, sectorY, starID, new StarFetchedHandler() {
                @Override
                public void onStarFetched(Star s) {
                    mSolarSystemSurfaceView.setStar(s);
                    mSolarSystemSurfaceView.redraw();
                }
            });
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

    private void onColonizeClick() {
        Planet planet = mSolarSystemSurfaceView.getSelectedPlanet();
        if (planet == null) {
            return;
        }

        EmpireManager.getInstance().getEmpire().colonize(planet, new Empire.ColonizeCompleteHandler() {
            @Override
            public void onColonizeComplete(boolean success) {
                // TODO: ??
            }
        });
    }
}
