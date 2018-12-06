package au.com.codeka.warworlds.client.game.solarsystem;

import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
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
public class PlanetDetailsScreen extends Screen {
  private static final Log log = new Log("PlanetDetailsScreen");

  private ScreenContext context;
  private Star star;
  private Planet planet;
  private PlanetDetailsLayout layout;

  public PlanetDetailsScreen(Star star, Planet planet) {
    this.star = star;
    this.planet = planet;
  }

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    this.context = context;
    layout = new PlanetDetailsLayout(context.getActivity(), star, planet, layoutCallbacks);
  }

  @Override
  public ShowInfo onShow() {
    return ShowInfo.builder().view(layout).build();
  }

  private final PlanetDetailsLayout.Callbacks layoutCallbacks = new PlanetDetailsLayout.Callbacks() {
    @Override
    public void onSaveFocusClick(
        float farmingFocus, float miningFocus, float energyFocus, float constructionFocus) {
      Preconditions.checkState(planet.colony != null && planet.colony.id != null);

      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.ADJUST_FOCUS)
          .colony_id(planet.colony.id)
          .focus(new ColonyFocus.Builder()
              .farming(farmingFocus)
              .mining(miningFocus)
              .energy(energyFocus)
              .construction(constructionFocus)
              .build()));

      context.popScreen();
    }

    @Override
    public void onAttackClick() {
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
  */
    }

    @Override
    public void onColonizeClick() {
      Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.COLONIZE)
          .empire_id(myEmpire.id)
          .planet_index(planet.index));

      context.popScreen();
    }
  };
}
