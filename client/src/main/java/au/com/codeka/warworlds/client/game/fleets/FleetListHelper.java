package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.CaseFormat;

import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.build.BuildViewHelper;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.util.Callback;
import au.com.codeka.warworlds.common.TimeFormatter;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;

public class FleetListHelper {
  public static void populateFleetRow(ViewGroup row, Fleet fleet, Design design) {
    TextView destinationView = row.findViewById(R.id.fleet_row3);
    CharSequence destination =
        getFleetDestination(row.getContext(), fleet, true, (ssb) -> {
          // We have to set the text to something different, then set it back again for it to
          // actually notice that it's changed.
          destinationView.setText("");
          destinationView.setText(ssb);
        });
    destinationView.setText(destination);

    BuildViewHelper.setDesignIcon(design, row.findViewById(R.id.fleet_icon));
    ((TextView) row.findViewById(R.id.fleet_row1)).setText(getFleetName(fleet, design));
    ((TextView) row.findViewById(R.id.fleet_row2)).setText(getFleetStance(fleet));

    ImageView fuelLevelIcon = row.findViewById(R.id.fuel_level_icon);
    TextView fuelLevelText = row.findViewById(R.id.fuel_level_text);
    ProgressBar fuelLevel = row.findViewById(R.id.fuel_level);
    if (fuelLevel != null && fuelLevelText != null && fuelLevelIcon != null) {
      if (fleet.fuel_amount >= design.fuel_size * fleet.num_ships) {
        fuelLevel.setVisibility(View.GONE);
        fuelLevelText.setVisibility(View.GONE);
        fuelLevelIcon.setVisibility(View.GONE);
      } else {
        fuelLevel.setVisibility(View.VISIBLE);
        fuelLevelText.setVisibility(View.VISIBLE);
        fuelLevelIcon.setVisibility(View.VISIBLE);

        float fuelPercent = 100 * fleet.fuel_amount / (design.fuel_size * fleet.num_ships);
        fuelLevel.setProgress(Math.round(fuelPercent));
        fuelLevelText.setText(
            String.format(
                Locale.ENGLISH, "%.0f / %.0f", fleet.fuel_amount,
                design.fuel_size * fleet.num_ships));
      }
    }
  }

  /**
   * Gets the name of a fleet.
   *
   * @param fleet The fleet whose name we want to get. Can be null if there is no fleet (actually
   *              this is just used to get the count of ships).
   * @param design The ship's {@link Design}.
   */
  public static CharSequence getFleetName(@Nullable Fleet fleet, Design design) {
    return getFleetName(fleet == null ? 1 : (int) Math.ceil(fleet.num_ships), design);
  }

  /**
   * Gets the name of a fleet.
   *
   * @param buildRequest The {@link BuildRequest} that we're building, used to get the count.
   * @param design The ship's {@link Design}.
   */
  public static CharSequence getFleetName(@Nullable BuildRequest buildRequest, Design design) {
    return getFleetName(buildRequest == null ? 1 : buildRequest.count, design);
  }

  /**
   * Gets the name of a fleet.
   *
   * @param count The number of ships in this fleet.
   * @param design The ship's {@link Design}.
   */
  private static CharSequence getFleetName(int count, Design design) {
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    if (count <= 1) {
      String text = String.format(Locale.ENGLISH, "%s",
          DesignHelper.getDesignName(design, false /* plural */));
      ssb.append(text);
    } else /*if (upgrades.size() == 0) */ {
      String text = String.format(Locale.ENGLISH, "%d × %s",
          count, DesignHelper.getDesignName(design, true /* plural */));
      ssb.append(text);
    } /*else {
      String text = String.format(Locale.ENGLISH, "%d ×", (int) Math.ceil(fleet.getNumShips()));
      addTextToRow(context, row, text, textSize);
      for (BaseFleetUpgrade upgrade : fleet.getUpgrades()) {
        Sprite sprite = SpriteManager.i.getSprite(design.getUpgrade(upgrade.getUpgradeID()).getSpriteName());
        addImageToRow(context, row, sprite, textSize);
      }
      text = String.format(Locale.ENGLISH, "%s", design.getDisplayName(fleet.getNumShips() > 1));
      addTextToRow(context, row, text, textSize);
    }*/
    return ssb;
  }


  /** Gets the destination text for the given fleet, or null if the fleet is not moving. */
  @Nullable
  public static CharSequence getFleetDestination(
      Context context, Fleet fleet, boolean includeEta,
      @Nullable Callback<SpannableStringBuilder> needRedrawCallback) {
    if (fleet.destination_star_id == null) {
      return null;
    }

    Star destStar = StarManager.i.getStar(fleet.destination_star_id);
    if (destStar != null) {
      return getFleetDestination(context, fleet, destStar, includeEta, needRedrawCallback);
    }

    return null;
  }

  private static CharSequence getFleetDestination(
      Context context, Fleet fleet, Star dest, boolean includeEta,
      @Nullable Callback<SpannableStringBuilder> needRedrawCallback) {
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    String eta = TimeFormatter.create().format(fleet.eta - System.currentTimeMillis());

//    float marginHorz = 0;
//    float marginVert = 0;
//    if (dest.classification.getImageScale() > 2.5) {
//      marginHorz = -(float) (sprite.getWidth() / dest.getStarType().getImageScale());
//      marginVert = -(float) (sprite.getHeight() / dest.getStarType().getImageScale());
//    }

//    BoostFleetUpgrade boostUpgrade = (BoostFleetUpgrade) fleet.getUpgrade("boost");
//    if (boostUpgrade != null && boostUpgrade.isBoosting()) {
//      addTextToRow(context, row, "→", 0);
//    }

    ssb.append("→ ○ ");
    ImageHelper.bindStarIcon(
        ssb, ssb.length() - 2, ssb.length() - 1, context, dest, needRedrawCallback);
    String name = dest.name;
    if (dest.classification == Star.CLASSIFICATION.MARKER) {
      name = "<i>Empty Space</i>";
    }
    if (includeEta) {
      String text = String.format("%s <b>ETA:</b> %s", name, eta);
      ssb.append(Html.fromHtml(text));
    } else {
      ssb.append(Html.fromHtml(name));
    }

    return ssb;
  }

  public static CharSequence getFleetStance(Fleet fleet) {
    return String.format("%s (stance: %s)",
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.state.toString()),
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.stance.toString()));
  }
}
