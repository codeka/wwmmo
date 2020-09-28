package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.build.BuildActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class OwnedPlanetActivity extends BaseActivity {
  private Star star;
  private Planet planet;
  private Colony colony;

  private FocusView focusView;
  private PlanetDetailsView planetDetails;
  private TextView buildQueueLength;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.planet_owned);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    final Button buildButton = findViewById(R.id.build_btn);
    buildButton.setOnClickListener(v -> {
      if (star == null) {
        return; // can happen before the star loads
      }
      if (colony == null) {
        return; // shouldn't happen, the button should be hidden.
      }

      Intent intent = new Intent(this, BuildActivity.class);
      intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());

      Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
      colony.toProtocolBuffer(colony_pb);
      intent.putExtra("au.com.codeka.warworlds.Colony", colony_pb.build().toByteArray());
      startActivity(intent);
    });

    planetDetails = findViewById(R.id.planet_details);
    focusView = findViewById(R.id.focus);
    findViewById(R.id.update_focus_btn).setOnClickListener(v -> focusView.save());
    buildQueueLength = findViewById(R.id.build_queue_length);
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
    for (BaseColony colony : star.getColonies()) {
      if (colony.getPlanetIndex() == planetIndex) {
        this.colony = (Colony) colony;
      }
    }

    planetDetails.setup(star, planet, null);
    focusView.setColony(star, colony);

    int totalBuildRequests = 0;
    for (BaseBuildRequest buildRequest : star.getBuildRequests()) {
      if (buildRequest.getPlanetIndex() == planetIndex) {
        totalBuildRequests ++;
      }
    }
    buildQueueLength.setText(String.format(Locale.US, "Build queue: %d", totalBuildRequests));
  }
}
