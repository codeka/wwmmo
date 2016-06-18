package au.com.codeka.warworlds.client.solarsystem;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableFloat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.databinding.FragPlanetDetailsBinding;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.Log;
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
  private FragPlanetDetailsBinding binding;
  private Handlers handlers = new Handlers();

  public static Bundle createArguments(long starID, int planetIndex) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starID);
    args.putInt(PLANET_INDEX_KEY, planetIndex);
    return args;
  }

  @Override
  public View onCreateView(
      LayoutInflater layoutInflater, ViewGroup parent, Bundle savedInstanceState) {
    binding = DataBindingUtil.inflate(layoutInflater, R.layout.frag_planet_details, parent, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    long seed = getArguments().getLong(STAR_ID_KEY);
    ViewBackgroundGenerator.setBackground(view.findViewById(R.id.planet_background), null, seed);
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
    Planet planet = star.planets.get(planetIndex);

    Empire empire = null;
    FocusModel focusModel = null;
    if (planet.colony != null && planet.colony.empire_id != null) {
      colonyId = planet.colony.id;
      empire = EmpireManager.i.getEmpire(planet.colony.empire_id);
      focusModel = new FocusModel(planet.colony.focus);
    }

    binding.setHandlers(handlers);
    binding.setStar(star);
    binding.setPlanet(planet);
    binding.setEmpire(empire);
    binding.setFocus(focusModel);
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

  @SuppressWarnings("unused") // used through data binding
  public class Handlers {
    public void onAttackClick(View view) {
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

    public void onColonizeClick(View view) {
      Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.COLONIZE)
          .empire_id(myEmpire.id)
          .planet_index(planetIndex)
          .build());

      // TODO: have a nicer API for this.
      getFragmentActivity().getSupportFragmentManager().popBackStack();
    }
  }

  @SuppressWarnings("unused") // used by bindings
  public class FocusModel {
    public ObservableFloat farmingFocus;
    public ObservableFloat miningFocus;
    public ObservableFloat energyFocus;
    public ObservableFloat constructionFocus;
    public ObservableBoolean farmingLocked;
    public ObservableBoolean miningLocked;
    public ObservableBoolean energyLocked;
    public ObservableBoolean constructionLocked;

    public FocusModel(ColonyFocus focus) {
      farmingFocus = new ObservableFloat(focus.farming);
      miningFocus = new ObservableFloat(focus.mining);
      energyFocus = new ObservableFloat(focus.energy);
      constructionFocus = new ObservableFloat(focus.construction);
      farmingLocked = new ObservableBoolean(false);
      miningLocked = new ObservableBoolean(false);
      energyLocked = new ObservableBoolean(false);
      constructionLocked = new ObservableBoolean(false);
    }

    public void onFarmingLockClick(View view) {
      farmingLocked.set(!farmingLocked.get());
    }

    public void onMiningLockClick(View view) {
      miningLocked.set(!miningLocked.get());
    }

    public void onEnergyLockClick(View view) {
      energyLocked.set(!energyLocked.get());
    }

    public void onConstructionLockClick(View view) {
      constructionLocked.set(!constructionLocked.get());
    }

    public void onPlusClick(View view) {
      SeekBar[] seekBars = {
          binding.focusFarming, binding.focusMining, binding.focusEnergy, binding.focusConstruction
      };
      View buttons[] = {
          binding.focusFarmingPlusBtn, binding.focusMiningPlusBtn, binding.focusEnergyPlusBtn,
          binding.focusConstructionPlusBtn
      };
      ObservableFloat[] focuses = {
          farmingFocus, miningFocus, energyFocus, constructionFocus
      };

      for (int i = 0; i < 4; i++) {
        if (view == buttons[i]) {
          float newValue = Math.max(0.0f, focuses[i].get() + 0.01f);
          seekBars[i].setProgress(Math.round(newValue * seekBars[i].getMax()));
          redistribute(i, newValue);
          break;
        }
      }
    }

    public void onMinusClick(View view) {
      SeekBar[] seekBars = {
          binding.focusFarming, binding.focusMining, binding.focusEnergy, binding.focusConstruction
      };
      View buttons[] = {
          binding.focusFarmingMinusBtn, binding.focusMiningMinusBtn, binding.focusEnergyMinusBtn,
          binding.focusConstructionMinusBtn
      };
      ObservableFloat[] focuses = {
          farmingFocus, miningFocus, energyFocus, constructionFocus
      };

      for (int i = 0; i < 4; i++) {
        if (view == buttons[i]) {
          float newValue = Math.max(0.0f, focuses[i].get() - 0.01f);
          seekBars[i].setProgress(Math.round(newValue * seekBars[i].getMax()));
          redistribute(i, newValue);
          break;
        }
      }
    }

    public void onFocusProgressChanged(
        SeekBar changedSeekBar, int progressValue, boolean fromUser) {
      if (!fromUser) {
        return;
      }

      SeekBar[] seekBars = {
          binding.focusFarming, binding.focusMining, binding.focusEnergy, binding.focusConstruction
      };
      for (int i = 0; i < 4; i++) {
        if (seekBars[i] == changedSeekBar) {
          redistribute(i, (float) changedSeekBar.getProgress() / changedSeekBar.getMax());
        }
      }
    }

    public void onSaveClick(View view) {
      saveFocus(farmingFocus.get(), miningFocus.get(), energyFocus.get(), constructionFocus.get());
    }

    private void redistribute(int changedIndex, float newValue) {
      ObservableBoolean[] locks = {
          farmingLocked, miningLocked, energyLocked, constructionLocked
      };
      ObservableFloat[] focuses = {
          farmingFocus, miningFocus, energyFocus, constructionFocus
      };

      float otherValuesTotal = 0.0f;
      for (int i = 0; i < 4; i++) {
        if (i == changedIndex) {
          focuses[i].set(newValue);
          continue;
        }
        otherValuesTotal += focuses[i].get();
      }

      float desiredOtherValuesTotal = 1.0f - newValue;
      if (desiredOtherValuesTotal <= 0.0f) {
        for (int i = 0; i < 4; i++) {
          if (i == changedIndex || locks[i].get()) {
            continue;
          };
          focuses[i].set(0.0f);
        }
        return;
      }

      float ratio = otherValuesTotal / desiredOtherValuesTotal;
      for (int i = 0; i < 4; i++) {
        if (i == changedIndex || locks[i].get()) {
          continue;
        }
        float focus = focuses[i].get();
        if (focus <= 0.001f) {
          focus = 0.001f;
        }
        focuses[i].set(focus / ratio);
      }
    }
  }
}
