package au.com.codeka.warworlds.client.ctrl;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.common.base.CaseFormat;
import com.squareup.picasso.Picasso;

import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.DesignHelper;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

public class FleetListHelper {
  public static void populateFleetNameRow(
      Context context, LinearLayout row, Fleet fleet, Design design) {
    populateFleetNameRow(context, row, fleet, design, 0);
  }

  public static void populateFleetNameRow(
      Context context, LinearLayout row, Fleet fleet, Design design, float textSize) {
    if (fleet == null) {
      String text = String.format(Locale.ENGLISH, "%s",
          DesignHelper.getDesignName(design, false /* plural */));
      addTextToRow(context, row, text, textSize);
    } else /*if (fleet.upgrades.size() == 0) */ {
      String text = String.format(Locale.ENGLISH, "%d × %s",
          (int) Math.ceil(fleet.num_ships),
          DesignHelper.getDesignName(design, fleet.num_ships > 1 /* plural */));
      addTextToRow(context, row, text, textSize);
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
  }

  public static void populateFleetDestinationRow(
      final Context context, final LinearLayout row, final Star srcStar, final Fleet fleet,
      final boolean includeEta) {
    Star destStar = StarManager.i.getStar(fleet.destination_star_id);
    if (srcStar != null && destStar != null) {
      populateFleetDestinationRow(context, row, fleet, srcStar, destStar, includeEta);
    }
  }

  private static void populateFleetDestinationRow(Context context, LinearLayout row,
      Fleet fleet, Star src, Star dest, boolean includeEta) {
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
    addTextToRow(context, row, "→", 0);
    //addImageToRow(context, row, sprite, marginHorz, marginVert, 0);
    String name = dest.name;
    if (dest.classification == Star.CLASSIFICATION.MARKER) {
      name = "<i>Empty Space</i>";
    }
    if (includeEta) {
      String text = String.format("%s <b>ETA:</b> %s", name, "eta"/*eta*/);
      addTextToRow(context, row, Html.fromHtml(text), 0);
    } else {
      addTextToRow(context, row, Html.fromHtml(name), 0);
    }

    row.setVisibility(View.VISIBLE);
  }

  public static void populateFleetStanceRow(Context context, LinearLayout row, Fleet fleet) {
    String text = String.format("%s (stance: %s)",
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.state.toString()),
        CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.stance.toString()));
    addTextToRow(context, row, text, 0);
  }

  private static void addTextToRow(
      Context context, LinearLayout row, CharSequence text, float size) {
    TextView tv = new TextView(context);
    tv.setText(text);
    tv.setSingleLine(true);
    tv.setEllipsize(TruncateAt.END);
    if (size != 0) {
      tv.setTextSize(size);
    }
    row.addView(tv);
  }
/*
  private static void addImageToRow(Context context, LinearLayout row, Sprite sprite, float size) {
    addImageToRow(context, row, sprite, 0, 0, size);
  }
*/
  private static void addImageToRow(Context context, LinearLayout row, Bitmap bmp, float size) {
    ImageView iv = new ImageView(context);
    iv.setImageBitmap(bmp);

    if (size == 0) {
      size = 16.0f;
    }
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) size * 2, (int) size * 2);
    lp.setMargins(5, -5, 5, -5);
    iv.setLayoutParams(lp);

    row.addView(iv);
  }
/*
  private static void addImageToRow(Context context, LinearLayout row, Sprite sprite, float marginHorz, float marginVert, float size) {
    ImageView iv = new ImageView(context);
    iv.setImageDrawable(new SpriteDrawable(sprite));

    if (size == 0) {
      size = 16.0f;
    }
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) size * 2, (int) size * 2);
    lp.setMargins((int) (marginHorz + 5), (int) (marginVert - 5), (int) (marginHorz + 5), (int) (marginVert - 5));
    iv.setLayoutParams(lp);

    row.addView(iv);
  }
*/
}
