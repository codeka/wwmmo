package au.com.codeka.warworlds.game.solarsystem;

import java.util.Locale;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * Activity for interacting with enemy planets (note it's not necessarily an enemy, per se, it
 * could also be an ally or faction member).
 */
public class EnemyPlanetActivity extends BaseActivity {
  private String starKey;
  private Star star;
  private Planet planet;
  private Colony colony;
  private Empire colonyEmpire;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.planet_enemy);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    Button attackBtn = findViewById(R.id.attack_btn);
    attackBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onAttackClick();
      }
    });
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();
    ShieldManager.eventBus.register(eventHandler);
    StarManager.eventBus.register(eventHandler);

    ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
      @Override
      public void onHelloComplete(boolean success, ServerGreeting greeting) {
        if (!success) {
          startActivity(new Intent(EnemyPlanetActivity.this, WarWorldsActivity.class));
        } else {
          starKey = getIntent().getExtras().getString("au.com.codeka.warworlds.StarKey");
          star = StarManager.i.getStar(Integer.parseInt(starKey));
          if (star != null) {
            refreshStarDetails();
          }
        }
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    StarManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star s) {
      if (starKey == null || !starKey.equals(s.getKey())) {
        return;
      }

      star = s;
      refreshStarDetails();
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      if (colonyEmpire != null && event.kind.equals(ShieldManager.EmpireShield)
          && Integer.parseInt(colonyEmpire.getKey()) == event.id) {
        refreshEmpireDetails();
      }
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      if (colony != null && colony.getEmpireID() == empire.getID()) {
        colonyEmpire = empire;
        findViewById(R.id.attack_btn).setEnabled(true);
        refreshEmpireDetails();
      }
    }
  };

  private void refreshStarDetails() {
    int planetIndex = getIntent().getExtras().getInt("au.com.codeka.warworlds.PlanetIndex");
    planet = (Planet) star.getPlanets()[planetIndex - 1];
    for (BaseColony baseColony : star.getColonies()) {
      if (baseColony.getPlanetIndex() == planetIndex) {
        colony = (Colony) baseColony;
      }
    }

    final Button attackBtn = findViewById(R.id.attack_btn);
    if (colony != null) {
      colonyEmpire = EmpireManager.i.getEmpire(colony.getEmpireID());
      if (colonyEmpire == null) {
        attackBtn.setEnabled(false);
      } else {
        refreshEmpireDetails();
      }
    } else {
      attackBtn.setVisibility(View.GONE);
    }

    PlanetDetailsView planetDetails = findViewById(R.id.planet_details);
    planetDetails.setup(star, planet, colony);
  }

  private void refreshEmpireDetails() {
    ImageView enemyIcon = findViewById(R.id.enemy_empire_icon);
    TextView enemyName = findViewById(R.id.enemy_empire_name);
    TextView enemyDefence = findViewById(R.id.enemy_empire_defence);

    int defence = (int) (0.25 * colony.getPopulation() * colony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }
    enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(this, colonyEmpire));
    enemyName.setText(colonyEmpire.getDisplayName());
    enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
  }

  private void onAttackClick() {
    int defence = (int) (0.25 * colony.getPopulation() * colony.getDefenceBoost());

    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    int attack = 0;
    for (BaseFleet fleet : star.getFleets()) {
      if (fleet.getEmpireKey() == null) {
        continue;
      }
      if (fleet.getEmpireKey().equals(myEmpire.getKey())) {
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP,
            fleet.getDesignID());
        if (design.hasEffect("troopcarrier")) {
          attack += Math.ceil(fleet.getNumShips());
        }
      }
    }

    StyledDialog.Builder b = new StyledDialog.Builder(this);
    b.setMessage(Html.fromHtml(String.format(Locale.ENGLISH,
        "<p>Do you want to attack this %s colony?</p>"
      + "<p><b>Colony defence:</b> %d<br />"
      + "   <b>Your attack capability:</b> %d</p>", colonyEmpire.getDisplayName(), defence,
        attack)));
    b.setPositiveButton("Attack!", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(final DialogInterface dialog, int which) {
        myEmpire.attackColony(star, colony, new MyEmpire.AttackColonyCompleteHandler() {
          @Override
          public void onComplete() {
            dialog.dismiss();
            finish();
          }
        });
      }
    });
    b.setNegativeButton("Cancel", null);
    b.create().show();
  }
}
