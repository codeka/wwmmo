package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class OwnedPlanetActivity extends BaseActivity {
  private Star star;
  private Planet planet;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.planet_owned);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    ServerGreeter.waitForHello(this, (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(this, WarWorldsActivity.class));
      } else {
        String starKey = getIntent().getExtras().getString("au.com.codeka.warworlds.StarKey");
        StarManager.eventBus.register(eventHandler);
        Star star = StarManager.i.getStar(Integer.parseInt(starKey));
        if (star != null) {
          refresh(star);
        }
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    StarManager.eventBus.unregister(eventHandler);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star s) {
      if (star != null && !star.getKey().equals(s.getKey())) {
        return;
      }

      refresh(s);
    }
  };

  private void refresh(Star s) {
    int planetIndex = getIntent().getExtras().getInt("au.com.codeka.warworlds.PlanetIndex");

    star = s;
    planet = (Planet) star.getPlanets()[planetIndex - 1];

    PlanetDetailsView planetDetails = findViewById(R.id.planet_details);
    planetDetails.setup(star, planet, null);
  }
}
