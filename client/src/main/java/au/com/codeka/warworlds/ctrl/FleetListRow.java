package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetUpgrade.BoostFleetUpgrade;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

import com.google.common.base.CaseFormat;

public class FleetListRow extends RelativeLayout {
    private Context context;
    private Fleet fleet;

    public FleetListRow(Context context) {
        this(context, null);
    }

    public FleetListRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        inflate(context, R.layout.fleet_list_row, this);
    }

    public void setFleet(Fleet fleet) {
        this.fleet = fleet;
        refresh();
    }

    public void refresh() {
        ImageView icon = (ImageView) findViewById(R.id.ship_icon);
        final LinearLayout row1 = (LinearLayout) findViewById(R.id.ship_row1);
        final LinearLayout row2 = (LinearLayout) findViewById(R.id.ship_row2);
        final LinearLayout row3 = (LinearLayout) findViewById(R.id.ship_row3);
        final TextView notes = (TextView) findViewById(R.id.notes);

        row1.removeAllViews();
        row2.removeAllViews();
        row3.removeAllViews();

        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(
                DesignKind.SHIP, fleet.getDesignID());
        icon.setImageDrawable(new SpriteDrawable(
                SpriteManager.i.getSprite(design.getSpriteName())));

        populateFleetNameRow(context, row1, fleet, design);
        populateFleetStanceRow(context, row2, fleet);

        if (notes != null && fleet.getNotes() != null) {
            notes.setText(fleet.getNotes());
            notes.setVisibility(View.VISIBLE);
        } else if (notes != null) {
            notes.setText("");
            notes.setVisibility(View.GONE);
        }

        if (fleet.getState() == Fleet.State.MOVING) {
            row3.setVisibility(View.GONE);
            populateFleetDestinationRow(context, row3, fleet, true);
        } else {
            row3.setVisibility(View.GONE);

            final MyEmpire myEmpire = EmpireManager.i.getEmpire();
            Empire empire = EmpireManager.i.getEmpire(fleet.getEmpireID());
            if (empire != null) {
                row3.setVisibility(View.VISIBLE);

                Bitmap shieldBmp = EmpireShieldManager.i.getShield(context, empire);
                if (shieldBmp != null) {
                    addImageToRow(context, row3, shieldBmp, 0);
                }

                if (myEmpire.getKey().equals(empire.getKey())) {
                    addTextToRow(context, row3, empire.getDisplayName(), 0);
                } else if (myEmpire.getAlliance() != null && empire.getAlliance() != null &&
                        myEmpire.getAlliance().getKey().equals(empire.getAlliance().getKey())) {
                    addTextToRow(context, row3, empire.getDisplayName(), 0);
                } else {
                    String html = "<font color=\"red\">" + empire.getDisplayName() + "</font>";
                    addTextToRow(context, row3, Html.fromHtml(html), 0);
                }
            }
        }
    }

    public static void populateFleetNameRow(Context context, LinearLayout row, Fleet fleet, ShipDesign design) {
        populateFleetNameRow(context, row, fleet, design, 0);
    }

    public static void populateFleetNameRow(Context context, LinearLayout row, Fleet fleet, ShipDesign design, float textSize) {
        if (fleet == null) {
            String text = String.format(Locale.ENGLISH, "%s", design.getDisplayName(false));
            addTextToRow(context, row, text, textSize);
        } else if (fleet.getUpgrades().size() == 0) {
            String text = String.format(Locale.ENGLISH, "%d × %s",
                    (int) Math.ceil(fleet.getNumShips()), design.getDisplayName(fleet.getNumShips() > 1));
            addTextToRow(context, row, text, textSize);
        } else {
            String text = String.format(Locale.ENGLISH, "%d ×", (int) Math.ceil(fleet.getNumShips()));
            addTextToRow(context, row, text, textSize);
            for (BaseFleetUpgrade upgrade : fleet.getUpgrades()) {
                Sprite sprite = SpriteManager.i.getSprite(design.getUpgrade(upgrade.getUpgradeID()).getSpriteName());
                addImageToRow(context, row, sprite, textSize);
            }
            text = String.format(Locale.ENGLISH, "%s", design.getDisplayName(fleet.getNumShips() > 1));
            addTextToRow(context, row, text, textSize);
        }
    }

    public static void populateFleetDestinationRow(final Context context, final LinearLayout row,
            final Fleet fleet, final boolean includeEta) {
        ArrayList<Integer> starIDs = new ArrayList<Integer>();
        starIDs.add(Integer.parseInt(fleet.getStarKey()));
        starIDs.add(Integer.parseInt(fleet.getDestinationStarKey()));
        SparseArray<Star> starSummaries = StarManager.i.getStars(starIDs);
        Star srcStar = starSummaries.get(Integer.parseInt(fleet.getStarKey()));
        Star destStar = starSummaries.get(Integer.parseInt(fleet.getDestinationStarKey()));
        if (srcStar != null && destStar != null) {
            populateFleetDestinationRow(context, row, fleet, srcStar, destStar, includeEta);
        }
    }

    private static void populateFleetDestinationRow(Context context, LinearLayout row,
            Fleet fleet, Star src, Star dest, boolean includeEta) {
        float timeRemainingInHours = fleet.getTimeToDestination();
        Sprite sprite = StarImageManager.getInstance().getSprite(dest, -1, true);
        String eta = TimeFormatter.create().format(timeRemainingInHours);

        float marginHorz = 0;
        float marginVert = 0;
        if (dest.getStarType().getImageScale() > 2.5) {
            marginHorz = -(float) (sprite.getWidth() / dest.getStarType().getImageScale());
            marginVert = -(float) (sprite.getHeight() / dest.getStarType().getImageScale());
        }

        BoostFleetUpgrade boostUpgrade = (BoostFleetUpgrade) fleet.getUpgrade("boost");
        if (boostUpgrade != null && boostUpgrade.isBoosting()) {
            addTextToRow(context, row, "→", 0);
        }
        addTextToRow(context, row, "→", 0);
        addImageToRow(context, row, sprite, marginHorz, marginVert, 0);
        String name = dest.getName();
        if (dest.getStarType().getInternalName().equals("marker")) {
            name = "<i>Empty Space</i>";
        }
        if (includeEta) {
            String text = String.format("%s <b>ETA:</b> %s",
                                        name, eta);
            addTextToRow(context, row, Html.fromHtml(text), 0);
        } else {
            addTextToRow(context, row, Html.fromHtml(name), 0);
        }

        row.setVisibility(View.VISIBLE);
    }

    public static void populateFleetStanceRow(Context context, LinearLayout row, Fleet fleet) {
        String text = String.format("%s (stance: %s)",
                CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.getState().toString()),
                CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fleet.getStance().toString()));
        addTextToRow(context, row, text, 0);
    }

    private static void addTextToRow(Context context, LinearLayout row, CharSequence text, float size) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setSingleLine(true);
        tv.setEllipsize(TruncateAt.END);
        if (size != 0) {
            tv.setTextSize(size);
        }
        row.addView(tv);
    }

    private static void addImageToRow(Context context, LinearLayout row, Sprite sprite, float size) {
        addImageToRow(context, row, sprite, 0, 0, size);
    }
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

}
