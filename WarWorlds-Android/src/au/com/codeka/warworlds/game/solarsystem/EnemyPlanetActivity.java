package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * Activity for interacting with enemy planets (note it's not nessecarily an enemy, per se, it
 * could also be an ally or faction member).
 */
public class EnemyPlanetActivity extends BaseActivity
                                 implements StarManager.StarFetchedHandler {
    private Star mStar;
    private Planet mPlanet;
    private Colony mColony;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.planet_enemy);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(EnemyPlanetActivity.this, WarWorldsActivity.class));
                } else {
                    String starKey = getIntent().getExtras().getString("au.com.codeka.warworlds.StarKey");
                    StarManager.getInstance().requestStar(EnemyPlanetActivity.this, starKey, false, EnemyPlanetActivity.this);
                    StarManager.getInstance().addStarUpdatedListener(starKey, EnemyPlanetActivity.this);
                }
            }
        });
    }

    @Override
    public void onStarFetched(Star s) {
        int planetIndex = getIntent().getExtras().getInt("au.com.codeka.warworlds.PlanetIndex");

        mStar = s;
        mPlanet = s.getPlanets()[planetIndex];
        for (Colony colony : s.getColonies()) {
            if (colony.getPlanetIndex() == planetIndex) {
                mColony = colony;
            }
        }

        PlanetDetailsView planetDetails = (PlanetDetailsView) findViewById(R.id.planet_details);
        planetDetails.setup(mStar, mPlanet, mColony);
    }
}
