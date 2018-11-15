package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.text.Html;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.wire.Wire;

import java.util.Locale;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.ColonyHelper;

/**
 * The summary view of a planet that shows up in the bottom-left of the solarsystem screen.
 */
public class PlanetSummaryView extends FrameLayout {
  interface Callbacks {
    void onViewClick();
  }

  @Nullable private Callbacks callbacks;

  private final Button emptyViewButton;
  private final View colonyDetailsContainer;
  private final View enemyColonyDetailsContainer;
  private final TextView populationCountTextView;
  private final ColonyFocusView colonyFocusView;

  public PlanetSummaryView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    inflate(context, R.layout.solarsystem_planet_summary, this);

    emptyViewButton = findViewById(R.id.empty_view_btn);
    colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details);
    enemyColonyDetailsContainer = findViewById(R.id.enemy_colony_details);
    populationCountTextView = findViewById(R.id.population_count);
    colonyFocusView = findViewById(R.id.colony_focus_view);

    emptyViewButton.setOnClickListener(view -> {
      if (callbacks != null) {
        callbacks.onViewClick();
      }
    });
  }

  public void setCallbacks(@Nonnull Callbacks callbacks) {
    this.callbacks = callbacks;
  }

  public void setPlanet(Star star, Planet planet) {
    TransitionManager.beginDelayedTransition(this);
    if (planet.colony == null) {
      emptyViewButton.setVisibility(View.VISIBLE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      refreshUncolonizedDetails();
    } else {
      emptyViewButton.setVisibility(View.GONE);
      colonyDetailsContainer.setVisibility(View.GONE);
      enemyColonyDetailsContainer.setVisibility(View.GONE);

      if (planet.colony.empire_id == null) {
        enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
        refreshNativeColonyDetails(planet);
      } else {
        Empire colonyEmpire = EmpireManager.i.getEmpire(planet.colony.empire_id);
        if (colonyEmpire != null) {
          Empire myEmpire = EmpireManager.i.getMyEmpire();
          if (myEmpire.id.equals(colonyEmpire.id)) {
            colonyDetailsContainer.setVisibility(View.VISIBLE);
            refreshColonyDetails(star, planet);
          } else {
            enemyColonyDetailsContainer.setVisibility(View.VISIBLE);
            refreshEnemyColonyDetails(colonyEmpire, planet);
          }
        } else {
          // TODO: wait for the empire to come in.
        }
      }
    }
  }

  private void refreshUncolonizedDetails() {
    populationCountTextView.setText(getContext().getString(R.string.uncolonized));
  }

  private void refreshColonyDetails(Star star, Planet planet) {
    String pop = "Pop: "
        + Math.round(planet.colony.population)
        + " <small>"
        + String.format(Locale.US, "(%s%d / hr)",
        Wire.get(planet.colony.delta_population, 0.0f) < 0 ? "-" : "+",
        Math.abs(Math.round(Wire.get(planet.colony.delta_population, 0.0f))))
        + "</small> / "
        + ColonyHelper.getMaxPopulation(planet);
    populationCountTextView.setText(Html.fromHtml(pop));

    colonyFocusView.setVisibility(View.VISIBLE);
    colonyFocusView.refresh(star, planet.colony);
  }

  private void refreshEnemyColonyDetails(Empire empire, Planet planet) {
    populationCountTextView.setText(String.format(Locale.US, "Population: %d",
        Math.round(planet.colony.population)));

/*    ImageView enemyIcon = (ImageView) mView.findViewById(R.id.enemy_empire_icon);
    TextView enemyName = (TextView) mView.findViewById(R.id.enemy_empire_name);
    TextView enemyDefence = (TextView) mView.findViewById(R.id.enemy_empire_defence);

    int defence = (int) (0.25 * mColony.getPopulation() * mColony.getDefenceBoost());
    if (defence < 1) {
      defence = 1;
    }
    enemyIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
    enemyName.setText(empire.getDisplayName());
    enemyDefence.setText(String.format(Locale.ENGLISH, "Defence: %d", defence));
  */}

  private void refreshNativeColonyDetails(Planet planet) {
    String pop = "Pop: "
        + Math.round(planet.colony.population);
    populationCountTextView.setText(Html.fromHtml(pop));
    colonyFocusView.setVisibility(View.GONE);
  }
}
