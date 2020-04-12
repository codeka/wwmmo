package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Layout for {@link PlanetDetailsScreen}.
 */
public class PlanetDetailsLayout extends RelativeLayout {
  interface Callbacks {
    void onSaveFocusClick(
        float farmingFocus, float miningFocus, float energyFocus, float constructionFocus);
    void onAttackClick();
    void onColonizeClick();
  }

  // The index into the focus arrays for each type of focus (farming, mining, etc).
  private static final int FARMING_INDEX = 0;
  private static final int MINING_INDEX = 1;
  private static final int ENERGY_INDEX = 2;
  private static final int CONSTRUCTION_INDEX = 3;

  private boolean[] focusLocks = new boolean[] { false, false, false, false };
  private float[] focusValues = new float[] { 0.25f, 0.25f, 0.25f, 0.25f };

  private Star star;
  private Planet planet;

  private final CongenialityView congeniality;
  private final ImageView planetIcon;
  private final ImageView empireIcon;
  private final View focusContainer;
  private final SeekBar[] focusSeekBars;
  private final TextView[] focusTextViews;
  private final Button[] focusMinusButtons;
  private final Button[] focusPlusButtons;
  private final ImageButton[] focusLockButtons;
  private final TextView empireName;
  private final TextView empireDefence;
  private final TextView note;
  private final FleetListSimple fleetList;
  private final Button attackBtn;
  private final Button colonizeBtn;

  public PlanetDetailsLayout(Context context, Star star, Planet planet, Callbacks callbacks) {
    super(context);
    inflate(context, R.layout.planet_details, this);
    ViewBackgroundGenerator.setBackground(findViewById(R.id.planet_background), null, star.id);
    this.star = star;
    this.planet = planet;

    congeniality = findViewById(R.id.congeniality);
    planetIcon = findViewById(R.id.planet_icon);
    empireIcon = findViewById(R.id.empire_icon);
    empireDefence = findViewById(R.id.empire_defence);
    focusContainer = findViewById(R.id.focus_container);
    note = findViewById(R.id.note);
    fleetList = findViewById(R.id.fleet_list);
    focusSeekBars = new SeekBar[] {
        findViewById(R.id.focus_farming),
        findViewById(R.id.focus_mining),
        findViewById(R.id.focus_energy),
        findViewById(R.id.focus_construction)};
    focusTextViews = new TextView[] {
        findViewById(R.id.focus_farming_value),
        findViewById(R.id.focus_mining_value),
        findViewById(R.id.focus_energy_value),
        findViewById(R.id.focus_construction_value)};
    focusMinusButtons = new Button[] {
        findViewById(R.id.focus_farming_minus_btn),
        findViewById(R.id.focus_mining_minus_btn),
        findViewById(R.id.focus_energy_minus_btn),
        findViewById(R.id.focus_construction_minus_btn)};
    focusPlusButtons = new Button[] {
        findViewById(R.id.focus_farming_plus_btn),
        findViewById(R.id.focus_mining_plus_btn),
        findViewById(R.id.focus_energy_plus_btn),
        findViewById(R.id.focus_construction_plus_btn)};
    focusLockButtons = new ImageButton[] {
        findViewById(R.id.focus_farming_lock),
        findViewById(R.id.focus_mining_lock),
        findViewById(R.id.focus_energy_lock),
        findViewById(R.id.focus_construction_lock)};
    empireName = findViewById(R.id.empire_name);

    attackBtn = findViewById(R.id.attack_btn);
    attackBtn.setOnClickListener(v -> callbacks.onAttackClick());

    colonizeBtn = findViewById(R.id.colonize_btn);
    colonizeBtn.setOnClickListener(v -> callbacks.onColonizeClick());

    for (int i = 0; i < 4; i++) {
      focusLockButtons[i].setOnClickListener(this::onFocusLockClick);
      focusPlusButtons[i].setOnClickListener(this::onFocusPlusClick);
      focusMinusButtons[i].setOnClickListener(this::onFocusMinusClick);

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
    findViewById(R.id.focus_save_btn).setOnClickListener(v ->
        callbacks.onSaveFocusClick(
            focusValues[FARMING_INDEX],
            focusValues[MINING_INDEX],
            focusValues[ENERGY_INDEX],
            focusValues[CONSTRUCTION_INDEX]));
    refresh();
  }

  private void refresh() {
    Empire empire = null;
    if (planet.colony != null && planet.colony.empire_id != null) {
      empire = EmpireManager.i.getEmpire(planet.colony.empire_id);
    }

    ImageHelper.bindPlanetIcon(planetIcon, star, planet);
    ImageHelper.bindEmpireShield(empireIcon, empire);
    if (empire != null) {
      empireName.setText(empire.display_name);
    } else if (planet.colony != null) {
      empireName.setText(R.string.native_colony);
    } else {
      empireName.setText(R.string.uncolonized);
    }
    congeniality.setPlanet(planet);

    if (planet.colony != null) {
      empireDefence.setText(
          String.format(
              Locale.ENGLISH,
              "Defence: %.0f",
              planet.colony.defence_bonus * planet.colony.population));
    }

    if (EmpireManager.i.isMyEmpire(empire)) {
      // It's our colony.
      focusContainer.setVisibility(View.VISIBLE);
      attackBtn.setVisibility(View.GONE);
      colonizeBtn.setVisibility(View.GONE);
      note.setText(R.string.focus_hint);

      focusValues[FARMING_INDEX] = planet.colony.focus.farming;
      focusValues[MINING_INDEX] = planet.colony.focus.mining;
      focusValues[ENERGY_INDEX] = planet.colony.focus.energy;
      focusValues[CONSTRUCTION_INDEX] = planet.colony.focus.construction;
      refreshFocus();
    } else if (planet.colony != null) {
      // It's an enemy colony (could be native or another player).
      focusContainer.setVisibility(View.GONE);
      attackBtn.setVisibility(View.VISIBLE);
      colonizeBtn.setVisibility(View.GONE);

      fleetList.setStar(star, (fleet) -> fleet.design_type == Design.DesignType.TROOP_CARRIER);
      note.setText(
          fleetList.getNumFleets() > 0 ? R.string.attack_hint : R.string.attack_hint_no_ships);
      attackBtn.setEnabled(fleetList.getNumFleets() > 0);
    } else {
      // It's uncolonized.
      focusContainer.setVisibility(View.GONE);
      attackBtn.setVisibility(View.GONE);
      colonizeBtn.setVisibility(View.VISIBLE);

      fleetList.setStar(star, (fleet) -> fleet.design_type == Design.DesignType.COLONY_SHIP);
      note.setText(
          fleetList.getNumFleets() > 0 ? R.string.colonize_hint : R.string.colonize_hint_no_ships);
      colonizeBtn.setEnabled(fleetList.getNumFleets() > 0);
    }
  }

  private void refreshFocus() {
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

  public void onFocusLockClick(View view) {
    for (int i = 0; i < 4; i++) {
      if (focusLockButtons[i] == view) {
        focusLocks[i] = !focusLocks[i];
      }
    }
    refreshFocus();
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
    refreshFocus();
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
    refreshFocus();
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
    refreshFocus();
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
