package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.ColonyFocusView;
import au.com.codeka.warworlds.client.game.fleets.FleetListSimple;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.ColonyHelper;
import com.squareup.wire.Wire;
import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * The layout for the {@link SolarSystemScreen}.
 */
// TODO: it probably makes sense to split these into a bunch of sub-views.
public class SolarSystemLayout extends DrawerLayout {
  public interface Callbacks {
    void onBuildClick(int planetIndex);
    void onFocusClick(int planetIndex);
    void onSitrepClick();
    void onViewColonyClick(int planetIndex);
    void onFleetClick(long fleetId);
  }

  private static final Log log = new Log("SolarSystemLayout");

  private final Callbacks callbacks;

  private final SunAndPlanetsView sunAndPlanets;
  private final CongenialityView congeniality;
  private final StoreView store;
  private final TextView planetName;
  private final FleetListSimple fleetList;
  private final ViewGroup bottomLeftPane;
  private final Button emptyViewButton;
  private final View colonyDetailsContainer;
  private final View enemyColonyDetailsContainer;
  private final TextView populationCountTextView;
  private final ColonyFocusView colonyFocusView;

  @Nonnull private Star star;
  private int planetIndex;

  /**
   * Constructs a new {@link SolarSystemLayout}.
   *
   * @param context The {@link Context}.
   * @param star The {@link Star} to display initially.
   * @param planetIndex The index of the planet to have initially select (or -1 for no planet).
   */
  public SolarSystemLayout(Context context, Callbacks callbacks, @Nonnull Star star, int planetIndex) {
    super(context);
    inflate(context, R.layout.solarsystem, this);

    this.callbacks = callbacks;
    this.star = star;
    this.planetIndex = planetIndex;

    sunAndPlanets = findViewById(R.id.solarsystem_view);
    congeniality = findViewById(R.id.congeniality);
    store = findViewById(R.id.store);
    final Button buildButton = findViewById(R.id.solarsystem_colony_build);
    final Button focusButton = findViewById(R.id.solarsystem_colony_focus);
    final Button sitrepButton = findViewById(R.id.sitrep_btn);
    final Button planetViewButton = findViewById(R.id.enemy_empire_view);
    planetName = findViewById(R.id.planet_name);
    fleetList = findViewById(R.id.fleet_list);
    bottomLeftPane = findViewById(R.id.bottom_left_pane);
    emptyViewButton = findViewById(R.id.empty_view_btn);
    colonyDetailsContainer = findViewById(R.id.solarsystem_colony_details);
    enemyColonyDetailsContainer = findViewById(R.id.enemy_colony_details);
    populationCountTextView = findViewById(R.id.population_count);
    colonyFocusView = findViewById(R.id.colony_focus_view);

    sunAndPlanets.setPlanetSelectedHandler(planet -> {
      if (planet == null) {
        SolarSystemLayout.this.planetIndex = -1;
      } else {
        SolarSystemLayout.this.planetIndex = planet.index;
      }
      refreshSelectedPlanet();
    });

    buildButton.setOnClickListener(v -> callbacks.onBuildClick(planetIndex));
    focusButton.setOnClickListener(v -> callbacks.onFocusClick(planetIndex));
    sitrepButton.setOnClickListener(v -> callbacks.onSitrepClick());
    planetViewButton.setOnClickListener(v -> callbacks.onViewColonyClick(planetIndex));
    emptyViewButton.setOnClickListener(v -> callbacks.onViewColonyClick(planetIndex));
    fleetList.setFleetSelectedHandler(fleet -> callbacks.onFleetClick(fleet.id));

    refreshStar(star);
  }

  public void refreshStar(Star star) {
    fleetList.setStar(star);
    sunAndPlanets.setStar(star);
    store.setStar(star);

    if (planetIndex >= 0) {
      planetName.setText(this.star.name);
    }

    if (planetIndex >= 0) {
      log.debug("Selecting planet #%d", planetIndex);
      sunAndPlanets.selectPlanet(planetIndex);
    } else {
      log.debug("No planet selected");
    }
  }

  private void refreshSelectedPlanet() {
    if (planetIndex < 0) {
      return;
    }
    Planet planet = star.planets.get(planetIndex);

    Vector2 planetCentre = sunAndPlanets.getPlanetCentre(planet);

    String name = star.name + " " + RomanNumeralFormatter.format(star.planets.indexOf(planet) + 1);
    planetName.setText(name);

    if (planetCentre == null) {
      // this is probably because the SolarSystemView probably hasn't rendered yet. We'll
      // just ignore this then cause it'll fire an onPlanetSelected when it finishes
      // drawing.
      congeniality.setVisibility(View.GONE);
    } else {
      float pixelScale = getContext().getResources().getDisplayMetrics().density;
      double x = planetCentre.x;
      double y = planetCentre.y;

      // hard-coded size of the congeniality container: 85x64 dp
      float offsetX = (85 + 20) * pixelScale;
      float offsetY = (64 + 20) * pixelScale;

      if (x - offsetX < 0) {
        offsetX  = -(20 * pixelScale);
      }
      if (y - offsetY < 20) {
        offsetY = -(20 * pixelScale);
      }

      RelativeLayout.LayoutParams params =
          (RelativeLayout.LayoutParams) congeniality.getLayoutParams();
      params.leftMargin = (int) (x - offsetX);
      params.topMargin = (int) (y - offsetY);
      if (params.topMargin < (40 * pixelScale)) {
        params.topMargin = (int)(40 * pixelScale);
      }

      congeniality.setLayoutParams(params);
      congeniality.setVisibility(View.VISIBLE);
    }
    congeniality.setPlanet(planet);

    TransitionManager.beginDelayedTransition(bottomLeftPane);
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
            refreshColonyDetails(planet);
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

  private void refreshColonyDetails(Planet planet) {
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
