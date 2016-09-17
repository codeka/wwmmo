package au.com.codeka.warworlds.client.game.solarsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;

/**
 * Activity for interacting with enemy planets (note it's not necessarily an enemy, per se, it
 * could also be an ally or faction member).
 */
public class PlanetDetailsFragment extends BaseFragment {
  private static final Log log = new Log("PlanetDetailsFragment");
  private static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private int planetIndex;
  private Long colonyId;

  // The index into the focus arrays for each type of focus (farming, mining, etc).
  private static final int FARMING_INDEX = 0;
  private static final int MINING_INDEX = 1;
  private static final int ENERGY_INDEX = 2;
  private static final int CONSTRUCTION_INDEX = 3;

  private boolean[] focusLocks = new boolean[] { false, false, false, false };
  private float[] focusValues = new float[] { 0.25f, 0.25f, 0.25f, 0.25f };

  private ImageView planetIcon;
  private ImageView empireIcon;
  private View focusContainer;
  private SeekBar[] focusSeekBars;
  private TextView[] focusTextViews;
  private Button[] focusMinusButtons;
  private Button[] focusPlusButtons;
  private ImageButton[] focusLockButtons;
  private TextView populationCongenialityValue;
  private ProgressBar populationCongeniality;
  private TextView farmingCongenialityValue;
  private ProgressBar farmingCongeniality;
  private TextView miningCongenialityValue;
  private ProgressBar miningCongeniality;
  private TextView energyCongenialityValue;
  private ProgressBar energyCongeniality;
  private TextView empireName;
  private Button attackBtn;
  private Button colonizeBtn;

  public static Bundle createArguments(long starID, int planetIndex) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starID);
    args.putInt(PLANET_INDEX_KEY, planetIndex);
    return args;
  }

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_planet_details;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    long seed = getArguments().getLong(STAR_ID_KEY);
    ViewBackgroundGenerator.setBackground(view.findViewById(R.id.planet_background), null, seed);

    planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
    empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
    focusContainer = view.findViewById(R.id.focus_container);
    focusSeekBars = new SeekBar[] {
        (SeekBar) view.findViewById(R.id.focus_farming),
        (SeekBar) view.findViewById(R.id.focus_mining),
        (SeekBar) view.findViewById(R.id.focus_energy),
        (SeekBar) view.findViewById(R.id.focus_construction)};
    focusTextViews = new TextView[] {
        (TextView) view.findViewById(R.id.focus_farming_value),
        (TextView) view.findViewById(R.id.focus_mining_value),
        (TextView) view.findViewById(R.id.focus_energy_value),
        (TextView) view.findViewById(R.id.focus_construction_value)};
    focusMinusButtons = new Button[] {
        (Button) view.findViewById(R.id.focus_farming_minus_btn),
        (Button) view.findViewById(R.id.focus_mining_minus_btn),
        (Button) view.findViewById(R.id.focus_energy_minus_btn),
        (Button) view.findViewById(R.id.focus_construction_minus_btn)};
    focusPlusButtons = new Button[] {
        (Button) view.findViewById(R.id.focus_farming_plus_btn),
        (Button) view.findViewById(R.id.focus_mining_plus_btn),
        (Button) view.findViewById(R.id.focus_energy_plus_btn),
        (Button) view.findViewById(R.id.focus_construction_plus_btn)};
    focusLockButtons = new ImageButton[] {
        (ImageButton) view.findViewById(R.id.focus_farming_lock),
        (ImageButton) view.findViewById(R.id.focus_mining_lock),
        (ImageButton) view.findViewById(R.id.focus_energy_lock),
        (ImageButton) view.findViewById(R.id.focus_construction_lock)};
    populationCongenialityValue = (TextView) view.findViewById(R.id.population_congeniality_value);
    populationCongeniality = (ProgressBar) view.findViewById(R.id.population_congeniality);
    farmingCongenialityValue = (TextView) view.findViewById(R.id.farming_congeniality_value);
    farmingCongeniality = (ProgressBar) view.findViewById(R.id.farming_congeniality);
    miningCongenialityValue = (TextView) view.findViewById(R.id.mining_congeniality_value);
    miningCongeniality = (ProgressBar) view.findViewById(R.id.mining_congeniality);
    energyCongenialityValue = (TextView) view.findViewById(R.id.energy_congeniality_value);
    energyCongeniality = (ProgressBar) view.findViewById(R.id.energy_congeniality);
    empireName = (TextView) view.findViewById(R.id.empire_name);

    attackBtn = (Button) view.findViewById(R.id.attack_btn);
    attackBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onAttackClick(view);
      }
    });

    colonizeBtn = (Button) view.findViewById(R.id.colonize_btn);
    colonizeBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onColonizeClick(view);
      }
    });

    for (int i = 0; i < 4; i++) {
      focusLockButtons[i].setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          onFocusLockClick(view);
        }
      });

      focusPlusButtons[i].setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          onFocusPlusClick(view);
        }
      });

      focusMinusButtons[i].setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          onFocusMinusClick(view);
        }
      });

      focusSeekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
          onFocusProgressChanged(seekBar, progressValue, fromUser);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
      });
    }
    view.findViewById(R.id.focus_save_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onFocusSaveClick(view);
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

    rebind();
  }

  private void rebind() {
    Planet planet = star.planets.get(planetIndex);
    Empire empire = null;
    if (planet.colony != null && planet.colony.empire_id != null) {
      colonyId = planet.colony.id;
      empire = EmpireManager.i.getEmpire(planet.colony.empire_id);
    }

    ImageHelper.bindPlanetIcon(planetIcon, star, planet);
    ImageHelper.bindEmpireShield(empireIcon, empire);
    if (empire != null) {
      empireName.setText(empire.display_name);
    } else {
      empireName.setText("");
    }
    populationCongeniality.setProgress(planet.population_congeniality);
    populationCongenialityValue.setText(NumberFormatter.format(planet.population_congeniality));
    farmingCongeniality.setProgress(planet.farming_congeniality);
    farmingCongenialityValue.setText(NumberFormatter.format(planet.farming_congeniality));
    miningCongeniality.setProgress(planet.mining_congeniality);
    miningCongenialityValue.setText(NumberFormatter.format(planet.mining_congeniality));
    energyCongeniality.setProgress(planet.energy_congeniality);
    energyCongenialityValue.setText(NumberFormatter.format(planet.energy_congeniality));

    focusContainer.setVisibility(empire == null ? View.GONE : View.VISIBLE);

    attackBtn.setVisibility(
        empire != null && EmpireManager.i.isEnemy(empire) ? View.VISIBLE : View.GONE);
    colonizeBtn.setVisibility(empire == null ? View.VISIBLE : View.GONE);

    if (planet.colony != null) {
      focusValues[FARMING_INDEX] = planet.colony.focus.farming;
      focusValues[MINING_INDEX] = planet.colony.focus.mining;
      focusValues[ENERGY_INDEX] = planet.colony.focus.energy;
      focusValues[CONSTRUCTION_INDEX] = planet.colony.focus.construction;
    }
    bindFocus();
  }

  private void bindFocus() {
    Planet planet = star.planets.get(planetIndex);
    if (planet.colony == null) {
      return;
    }
    ColonyFocus focus = planet.colony.focus;

    for (int i = 0; i < 4; i++) {
      focusLockButtons[i].setImageResource(
          focusLocks[i] ? R.drawable.lock_closed : R.drawable.lock_opened);
      focusSeekBars[i].setProgress((int)(focusValues[i] * 1000.0f));
      focusTextViews[i].setText(NumberFormatter.format(Math.round(focusValues[i] * 100.0f)));
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

  /** Called when the user clicks 'save', we want to update the focus to the new values. */
  private void saveFocus(float farming, float mining, float energy, float construction) {
    Preconditions.checkState(colonyId != null);

    StarManager.i.updateStar(star, new StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.ADJUST_FOCUS)
        .colony_id(colonyId)
        .focus(new ColonyFocus.Builder()
            .farming(farming)
            .mining(mining)
            .energy(energy)
            .construction(construction)
            .build())
        .build());

    getFragmentManager().popBackStack();
  }

  private void onAttackClick(View view) {
    log.info("Attack!");
    /*
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

  private void onColonizeClick(View view) {
    Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
    StarManager.i.updateStar(star, new StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.COLONIZE)
        .empire_id(myEmpire.id)
        .planet_index(planetIndex)
        .build());

    // TODO: have a nicer API for this.
    getFragmentActivity().getSupportFragmentManager().popBackStack();
  }

  public void onFocusLockClick(View view) {
    for (int i = 0; i < 4; i++) {
      if (focusLockButtons[i] == view) {
        focusLocks[i] = !focusLocks[i];
      }
    }
    bindFocus();
  }

  public void onFocusPlusClick(View view) {
    for (int i = 0; i < 4; i++) {
      if (view == focusPlusButtons[i]) {
        float newValue = Math.max(0.0f, focusValues[i] + 0.01f);
        focusSeekBars[i].setProgress(Math.round(newValue * focusSeekBars[i].getMax()));
        redistribute(i, newValue);
        break;
      }
    }
    bindFocus();
  }

  public void onFocusMinusClick(View view) {
    for (int i = 0; i < 4; i++) {
      if (view == focusMinusButtons[i]) {
        float newValue = Math.max(0.0f, focusValues[i] - 0.01f);
        focusSeekBars[i].setProgress(Math.round(newValue * focusSeekBars[i].getMax()));
        redistribute(i, newValue);
        break;
      }
    }
    bindFocus();
  }

  public void onFocusProgressChanged(
      SeekBar changedSeekBar, int progressValue, boolean fromUser) {
    if (!fromUser) {
      return;
    }

    for (int i = 0; i < 4; i++) {
      if (focusSeekBars[i] == changedSeekBar) {
        redistribute(i, (float) progressValue / changedSeekBar.getMax());
      }
    }
    bindFocus();
  }

  public void onFocusSaveClick(View view) {
    saveFocus(
        focusValues[FARMING_INDEX],
        focusValues[MINING_INDEX],
        focusValues[ENERGY_INDEX],
        focusValues[CONSTRUCTION_INDEX]);
  }

  private void redistribute(int changedIndex, float newValue) {
    float otherValuesTotal = 0.0f;
    for (int i = 0; i < 4; i++) {
      if (i == changedIndex) {
        focusValues[i] = newValue;
        continue;
      }
      otherValuesTotal += focusValues[i];
    }

    float desiredOtherValuesTotal = 1.0f - newValue;
    if (desiredOtherValuesTotal <= 0.0f) {
      for (int i = 0; i < 4; i++) {
        if (i == changedIndex || focusLocks[i]) {
          continue;
        };
        focusValues[i] = 0.0f;
      }
      return;
    }

    float ratio = otherValuesTotal / desiredOtherValuesTotal;
    for (int i = 0; i < 4; i++) {
      if (i == changedIndex || focusLocks[i]) {
        continue;
      }
      float focus = focusValues[i];
      if (focus <= 0.001f) {
        focus = 0.001f;
      }
      focusValues[i] = focus / ratio;
    }
  }
}
