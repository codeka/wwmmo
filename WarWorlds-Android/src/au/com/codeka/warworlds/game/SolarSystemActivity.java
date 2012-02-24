package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.ModelManager;
import au.com.codeka.warworlds.model.ModelManager.StarFetchedHandler;
import au.com.codeka.warworlds.model.Star;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 * @author dean@codeka.com.au
 *
 */
public class SolarSystemActivity extends Activity {

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
        final SolarSystemSurfaceView solarSystemSurfaceView =
                (SolarSystemSurfaceView) findViewById(R.id.solarsystem_view);

        EmpireManager empire = EmpireManager.getInstance();
        username.setText(empire.getDisplayName());
        money.setText("$ 12,345"); // TODO: empire.getCash()

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            long sectorX = extras.getLong("au.com.codeka.warworlds.SectorX");
            long sectorY = extras.getLong("au.com.codeka.warworlds.SectorY");
            int starID = extras.getInt("au.com.codeka.warworlds.StarID");

            ModelManager.requestStar(sectorX, sectorY, starID, new StarFetchedHandler() {
                @Override
                public void onStarFetched(Star s) {
                    solarSystemSurfaceView.setStar(s);
                    solarSystemSurfaceView.redraw();
                }
            });
        }

    }
}
