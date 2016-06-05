package au.com.codeka.warworlds.client.solarsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Activity for interacting with enemy planets (note it's not necessarily an enemy, per se, it
 * could also be an ally or faction member).
 */
public class PlanetDetailsFragment extends BaseFragment {
  private static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private int planetIndex;
  private Planet planet;
  @Nullable private Empire empire;

  private Button attackBtn;
  private ImageView planetIcon;
  private View congenialityContainer;
  private ProgressBar populationCongenialityProgressBar;
  private TextView populationCongenialityTextView;
  private ProgressBar farmingCongenialityProgressBar;
  private TextView farmingCongenialityTextView;
  private ProgressBar miningCongenialityProgressBar;
  private TextView miningCongenialityTextView;
  private ProgressBar energyCongenialityProgressBar;
  private TextView energyCongenialityTextView;

  public static Bundle createArguments(long starID, int planetIndex) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starID);
    args.putInt(PLANET_INDEX_KEY, planetIndex);
    return args;
  }

  @Override
  public View onCreateView(
      LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
    return layoutInflater.inflate(R.layout.frag_planet_details, parent, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    ViewBackgroundGenerator.setBackground(view.findViewById(R.id.planet_background));

    attackBtn = (Button) view.findViewById(R.id.attack_btn);
    planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
    congenialityContainer = view.findViewById(R.id.congeniality_container);
    populationCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.population_congeniality);
    populationCongenialityTextView = (TextView) view.findViewById(
        R.id.population_congeniality_value);
    farmingCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.farming_congeniality);
    farmingCongenialityTextView = (TextView) view.findViewById(
        R.id.farming_congeniality_value);
    miningCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.mining_congeniality);
    miningCongenialityTextView = (TextView) view.findViewById(
        R.id.mining_congeniality_value);
    energyCongenialityProgressBar = (ProgressBar) view.findViewById(
        R.id.energy_congeniality);
    energyCongenialityTextView = (TextView) view.findViewById(
        R.id.energy_congeniality_value);

    attackBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onAttackClick();
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
   // App.i.getEventBus().register(eventHandler);

    star = StarManager.i.getStar(getArguments().getLong(STAR_ID_KEY));
    if (star == null) {
      return;
    }
    planetIndex = getArguments().getInt(PLANET_INDEX_KEY);
    planet = star.planets.get(planetIndex);
    refreshStarDetails();
    if (planet.colony != null && planet.colony.empire_id != null) {
      empire = EmpireManager.i.getEmpire(planet.colony.empire_id);
      refreshEmpireDetails();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
   // App.i.getEventBus().unregister(eventHandler);
  }

  private Object eventHandler = new Object() {/*
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
    }*/
  };

  private void refreshStarDetails() {
    if (empire != null) {
      attackBtn.setEnabled(false);
      refreshEmpireDetails();
    } else {
      attackBtn.setVisibility(View.GONE);
    }

    if (star == null || planet == null) {
      planetIcon.setVisibility(View.GONE);
      congenialityContainer.setVisibility(View.GONE);
    } else {
      planetIcon.setVisibility(View.VISIBLE);

      Picasso.with(getContext())
          .load(ImageHelper.getPlanetImageUrl(getContext(), star, planetIndex, 150, 150))
          .into(planetIcon);

      congenialityContainer.setVisibility(View.VISIBLE);

      populationCongenialityTextView.setText(
          NumberFormatter.format(planet.population_congeniality));
      populationCongenialityProgressBar.setProgress(
          (int) (populationCongenialityProgressBar.getMax()
              * (planet.population_congeniality / 1000.0)));

      farmingCongenialityTextView.setText(NumberFormatter.format(planet.farming_congeniality));
      farmingCongenialityProgressBar.setProgress(
          (int)(farmingCongenialityProgressBar.getMax() * (planet.farming_congeniality / 100.0)));

      miningCongenialityTextView.setText(NumberFormatter.format(planet.mining_congeniality));
      miningCongenialityProgressBar.setProgress(
          (int)(miningCongenialityProgressBar.getMax() * (planet.mining_congeniality / 100.0)));

      energyCongenialityTextView.setText(NumberFormatter.format(planet.energy_congeniality));
      energyCongenialityProgressBar.setProgress(
          (int)(miningCongenialityProgressBar.getMax() * (planet.energy_congeniality / 100.0)));
    }

  }

  private void refreshEmpireDetails() {/*
    ImageView enemyIcon = (ImageView) findViewById(R.id.enemy_empire_icon);
    TextView enemyName = (TextView) findViewById(R.id.enemy_empire_name);
    TextView enemyDefence = (TextView) findViewById(R.id.enemy_empire_defence);

    int defence = (int) (0.25 * colony.getPopulation() * colony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }
    enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(this, colonyEmpire));
    enemyName.setText(colonyEmpire.getDisplayName());
    enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
  */}

  private void onAttackClick() {/*
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
  */}
}
