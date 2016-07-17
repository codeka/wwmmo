package au.com.codeka.warworlds.client.fleets;

import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils.TruncateAt;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.common.base.CaseFormat;
import com.squareup.picasso.Picasso;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.build.BuildHelper;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

public class FleetListHelper {
  public static void populateFleetRow(ViewGroup row, Star star, Fleet fleet, Design design) {
    BuildHelper.setDesignIcon(design, (ImageView) row.findViewById(R.id.fleet_icon));
    ((TextView) row.findViewById(R.id.fleet_row1)).setText(getFleetName(fleet, design));
    ((TextView) row.findViewById(R.id.fleet_row2)).setText(getFleetStance(fleet));
    ((TextView) row.findViewById(R.id.fleet_row3)).setText(getFleetDestination(star, fleet, true));
  }

  /**
   * Gets the name of a fleet.
   *
   * @param fleet The fleet whose name we want to get. Can be null if there is no fleet (actually
   *              this is just used to get the count of ships).
   * @param design The ship's {@link Design}.
   */
  public static CharSequence getFleetName(@Nullable Fleet fleet, Design design) {
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    if (fleet == null) {
      String text = String.format(Locale.ENGLISH, "%s",
          DesignHelper.getDesignName(design, false /* plural */));
      ssb.append(text);
    } else /*if (fleet.upgrades.size() == 0) */ {
      String text = String.format(Locale.ENGLISH, "%d × %s",
          (int) Math.ceil(fleet.num_ships),
          DesignHelper.getDesignName(design, fleet.num_ships > 1 /* plural */));
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
  public static CharSequence getFleetDestination(Star srcStar, Fleet fleet, boolean includeEta) {
    if (fleet.destination_star_id == null) {
      return null;
    }

    Star destStar = StarManager.i.getStar(fleet.destination_star_id);
    if (srcStar != null && destStar != null) {
      return getFleetDestination(fleet, srcStar, destStar, includeEta);
    }

    return null;
  }

  private static CharSequence getFleetDestination(
      Fleet fleet, Star src, Star dest, boolean includeEta) {
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    /*float timeRemainingInHours = fleet.getTimeToDestination();
    Sprite sprite = StarImageManager.getInstance().getSprite(dest, -1, true);
    String eta = TimeFormatter.create().format(timeRemainingInHours);*/

    float marginHorz = 0;
    float marginVert = 0;
    //if (dest.getStarType().getImageScale() > 2.5) {
    //  marginHorz = -(float) (sprite.getWidth() / dest.getStarType().getImageScale());
    //  marginVert = -(float) (sprite.getHeight() / dest.getStarType().getImageScale());
    //}

    //BoostFleetUpgrade boostUpgrade = (BoostFleetUpgrade) fleet.getUpgrade("boost");
    //if (boostUpgrade != null && boostUpgrade.isBoosting()) {
    //  addTextToRow(context, row, "→", 0);
    //}
    ssb.append("→");
    //addImageToRow(context, row, sprite, marginHorz, marginVert, 0);
    String name = dest.name;
    if (dest.classification == Star.CLASSIFICATION.MARKER) {
      name = "<i>Empty Space</i>";
    }
    if (includeEta) {
      String text = String.format("%s <b>ETA:</b> %s", name, "eta"/*eta*/);
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
