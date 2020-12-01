package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
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

  private int numNormalTroopCarriers = 0;
  private int numMissionaryTroopCarriers = 0;
  private int numEmissaryTroopCarriers = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.planet_enemy);

    View rootView = findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    Button attackBtn = findViewById(R.id.attack_btn);
    attackBtn.setOnClickListener(v -> onAttackClick());

    Button sendMissionariesBtn = findViewById(R.id.missionary_btn);
    sendMissionariesBtn.setOnClickListener(v -> onSendMissionariesClick());

    SeekBar emissarySeekBar = findViewById(R.id.emissary_seekbar);
    emissarySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        refreshPropagandizeTimeEstimate();
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    Button sendEmissaryBtn = findViewById(R.id.emissary_btn);
    sendEmissaryBtn.setOnClickListener(v -> onSendEmissariesClick());
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();
    ShieldManager.eventBus.register(eventHandler);
    StarManager.eventBus.register(eventHandler);

    ServerGreeter.waitForHello(this, (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(EnemyPlanetActivity.this, WelcomeFragment.class));
      } else {
        starKey = getIntent().getExtras().getString("au.com.codeka.warworlds.StarKey");
        star = StarManager.i.getStar(Integer.parseInt(starKey));
        if (star != null) {
          refreshStarDetails();
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

  private final Object eventHandler = new Object() {
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
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();

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
        // We shouldn't get here, should be on EmptyPlanetActivity
        return;
      } else {
        refreshEmpireDetails();
      }
    } else {
      attackBtn.setVisibility(View.GONE);
    }

    PlanetDetailsView planetDetails = findViewById(R.id.planet_details);
    planetDetails.setup(star, planet, colony);

    for (BaseFleet fleet : star.getFleets()) {
      if (fleet.getEmpireKey() == null || !fleet.getEmpireKey().equals(myEmpire.getKey())) {
        // It's not our fleet, ignore.
        continue;
      }

      if (!fleet.getDesignID().equals("troopcarrier")) {
        // It's not a troop carrier, ignore.
        continue;
      }

      ArrayList<BaseFleetUpgrade> upgrades = fleet.getUpgrades();
      if (upgrades == null || upgrades.size() == 0) {
        numNormalTroopCarriers += fleet.getNumShips();
      } else {
        for (BaseFleetUpgrade upgrade : upgrades) {
          if (upgrade.getUpgradeID().equals("missionary")) {
            numMissionaryTroopCarriers += fleet.getNumShips();
          } else if (upgrade.getUpgradeID().equals("emissary")) {
            numEmissaryTroopCarriers += fleet.getNumShips();
          }
        }
      }
    }

    TextView tv = findViewById(R.id.attack_label);
    tv.setText(
        String.format(Locale.ENGLISH, "Available Troop Carriers: %d", numNormalTroopCarriers));

    tv = findViewById(R.id.missionary_label);
    tv.setText(
        String.format(Locale.ENGLISH, "Available Troop Carriers: %d", numMissionaryTroopCarriers));

    if (colony != null) {
      int defence = (int) (0.25 * colony.getPopulation() * colony.getDefenceBoost());
      if (defence < 1) {
        defence = 1;
      }
      tv = findViewById(R.id.emissary_min_ships);
      tv.setText(String.format(Locale.ENGLISH, "%d", Math.min(numEmissaryTroopCarriers, defence)));

      tv = findViewById(R.id.emissary_max_ships);
      tv.setText(String.format(Locale.ENGLISH, "%d", numEmissaryTroopCarriers));

      refreshPropagandizeTimeEstimate();
    }
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
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    myEmpire.attackColony(star, colony, MyEmpire.AttackKind.NORMAL, 0.0f, this::finish);
  }

  private void refreshPropagandizeTimeEstimate() {
    int defence = (int) (0.25 * colony.getPopulation() * colony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }

    SeekBar emissarySeekBar = findViewById(R.id.emissary_seekbar);
    float fraction = (float) emissarySeekBar.getProgress() / emissarySeekBar.getMax();
    // fraction will be a value between 0 and 1, but we want it to be logarithmic.

    // This is the number of emissary troop carriers we'll actually use.
    int actualEmissaryTroopCarriers =
        defence + (int)(fraction * (numEmissaryTroopCarriers - defence));

    float propagandaTime = colony.getPropagandaTime(actualEmissaryTroopCarriers);

    TextView tv = findViewById(R.id.emissary_label);
    tv.setText(
        String.format(
            Locale.ENGLISH,
            "Troop Carriers: %d / %d",
            actualEmissaryTroopCarriers,
            numEmissaryTroopCarriers));

    tv = findViewById(R.id.emissary_est_time);
    tv.setText(
       String.format(
           Locale.US, "Estimated time: %s", TimeFormatter.create().format(propagandaTime)));
  }

  private void onSendMissionariesClick() {
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    myEmpire.attackColony(star, colony, MyEmpire.AttackKind.SEND_MISSIONARIES, 0.0f, this::finish);
  }

  private void onSendEmissariesClick() {
    final SeekBar emissarySeekBar = findViewById(R.id.emissary_seekbar);
    final float fraction = (float) emissarySeekBar.getProgress() / emissarySeekBar.getMax();
    final MyEmpire myEmpire = EmpireManager.i.getEmpire();
    myEmpire.attackColony(
        star, colony, MyEmpire.AttackKind.SEND_EMISSARIES, fraction, this::finish);
  }

  private void setAttackVisible(boolean visible) {
    findViewById(R.id.horz_sep_1).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.attack_label).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.attack_btn).setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  private void setMissionaryVisible(boolean visible) {
    findViewById(R.id.horz_sep_2).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.missionary_label).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.missionary_btn).setVisibility(visible ? View.VISIBLE : View.GONE);
  }
  private void setEmissaryVisible(boolean visible) {
    findViewById(R.id.horz_sep_3).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_btn).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_est_time).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_help).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_label).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_max_ships).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_min_ships).setVisibility(visible ? View.VISIBLE : View.GONE);
    findViewById(R.id.emissary_seekbar).setVisibility(visible ? View.VISIBLE : View.GONE);
  }
}
