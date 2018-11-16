package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.text.Html;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Star;

import static com.google.common.base.Preconditions.checkState;

public class UpgradeBottomPane extends RelativeLayout implements BottomPaneContentView {
  public interface Callback {
    void onUpgrade(Building building);
  }

  private final Star star;
  private final Colony colony;
  private final Design design;
  private final Building building;
  private final Callback callback;

  private final ImageView buildIcon;
  private final TextView buildName;
  private final TextView buildDescription;
  private final TextView buildTime;
  private final TextView buildMinerals;
  private final TextView currentLevel;

  public UpgradeBottomPane(Context context) {
    super(context);
    checkState(isInEditMode());

    star = null;
    colony = null;
    design = null;
    building = null;
    callback = null;
    buildIcon = null;
    buildName = null;
    buildDescription = null;
    buildTime = null;
    buildMinerals = null;
    currentLevel = null;
  }

  public UpgradeBottomPane(
      Context context,
      Star star,
      Colony colony,
      Design design,
      Building building,
      Callback callback) {
    super(context);
    this.star = star;
    this.colony = colony;
    this.design = design;
    this.building = building;
    this.callback = callback;

    inflate(context, R.layout.build_upgrade_bottom_pane, this);

    buildIcon = findViewById(R.id.build_icon);
    buildName = findViewById(R.id.build_name);
    buildDescription = findViewById(R.id.build_description);
    buildTime = findViewById(R.id.build_timetobuild);
    buildMinerals = findViewById(R.id.build_mineralstobuild);
    currentLevel = findViewById(R.id.upgrade_current_level);

    findViewById(R.id.build_button).setOnClickListener(v -> upgrade());

    currentLevel.setText(String.format(Locale.US, "%d", building.level));

    BuildViewHelper.setDesignIcon(design, buildIcon);
    buildName.setText(design.display_name);
    buildDescription.setText(Html.fromHtml(design.description));
  }

  @Override
  public void refresh(Star star) {

  }

  private void upgrade() {
    callback.onUpgrade(building);
  }
}
