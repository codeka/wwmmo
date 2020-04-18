package au.com.codeka.warworlds.client.game.solarsystem;

import android.text.SpannableStringBuilder;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.ColonyFocus;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;

import static com.google.common.base.Preconditions.checkNotNull;

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

  @Override
  public CharSequence getTitle() {
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    ssb.append("○ ");
    ssb.append(star.name);
    ssb.append(" ");
    ssb.append(RomanNumeralFormatter.format(planet.index + 1));
    ImageHelper.bindStarIcon(
        ssb, 0, 1, context.getActivity(), star, 24, /* TODO: redraw callback */ null);
    return ssb;
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
      if (planet.colony == null) {
        return;
      }

      Empire myEmpire = checkNotNull(EmpireManager.i.getMyEmpire());
      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.ATTACK_COLONY)
          .empire_id(myEmpire.id)
          .colony_id(planet.colony.id));
      context.popScreen();
    }

    @Override
    public void onColonizeClick() {
      Empire myEmpire = checkNotNull(EmpireManager.i.getMyEmpire());
      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.COLONIZE)
          .empire_id(myEmpire.id)
          .planet_index(planet.index));

      context.popScreen();
    }
  };
}
