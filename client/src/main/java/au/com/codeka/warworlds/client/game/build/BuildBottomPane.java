package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.BuildHelper;
import au.com.codeka.warworlds.common.sim.StarModifier;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;

import static com.google.common.base.Preconditions.checkState;

/**
 * Bottom pane of the build fragment for when we're building something new.
 */
public class BuildBottomPane extends RelativeLayout implements BottomPaneContentView {
  public interface Callback {
    void onBuild(Design.DesignType designType, int count);
  }

  private final static Log log = new Log("BuildBottomPane");

  private final Star star;
  private final Colony colony;
  private final Design design;
  private final Callback callback;

  private final ImageView buildIcon;
  private final TextView buildName;
  private final TextView buildDescription;
  private final TextView buildTime;
  private final TextView buildMinerals;
  private final ViewGroup buildCountContainer;

  private final SeekBar buildCountSeek;
  private final EditText buildCount;

  public BuildBottomPane(Context context) {
    super(context);
    checkState(isInEditMode());

    star = null;
    colony = null;
    design = null;
    callback = null;
    buildIcon = null;
    buildName = null;
    buildDescription = null;
    buildTime = null;
    buildMinerals = null;
    buildCountContainer = null;
    buildCountSeek = null;
    buildCount = null;
  }

  public BuildBottomPane(
      Context context,
      Star star,
      Colony colony,
      Design design,
      Callback callback) {
    super(context);
    this.star = star;
    this.colony = colony;
    this.design = design;
    this.callback = callback;

    inflate(context, R.layout.build_build_bottom_pane, this);

    buildIcon = findViewById(R.id.build_icon);
    buildName = findViewById(R.id.build_name);
    buildDescription = findViewById(R.id.build_description);
    buildCountContainer = findViewById(R.id.build_count_container);
    buildTime = findViewById(R.id.build_timetobuild);
    buildMinerals = findViewById(R.id.build_mineralstobuild);

    buildCountSeek = findViewById(R.id.build_count_seek);
    buildCount = findViewById(R.id.build_count_edit);
    buildCountSeek.setMax(1000);
    buildCountSeek.setOnSeekBarChangeListener(buildCountSeekBarChangeListener);

    findViewById(R.id.build_button).setOnClickListener(v -> build());

    buildCount.setText("1");
    buildCountSeek.setProgress(1);

    BuildViewHelper.setDesignIcon(design, buildIcon);
    buildName.setText(design.display_name);
    buildDescription.setText(Html.fromHtml(design.description));

    if (design.design_kind == Design.DesignKind.SHIP) {
      // You can only build more than ship at a time (not buildings).
      buildCountContainer.setVisibility(View.VISIBLE);
    } else {
      buildCountContainer.setVisibility(View.GONE);
    }
    updateBuildTime();
  }

  @Override
  public void refresh(Star star) {
    // TODO
  }

  private void updateBuildTime() {
    App.i.getTaskRunner().runTask(() -> {
      // Add the build request to a temporary copy of the star, simulate it and figure out the
      // build time.
      Star.Builder starBuilder = star.newBuilder();

      int count = 1;
      if (design.design_kind == Design.DesignKind.SHIP) {
        count = Integer.parseInt(buildCount.getText().toString());
      }

      Empire myEmpire = EmpireManager.i.getMyEmpire();
      try {
        new StarModifier(() -> 0).modifyStar(starBuilder,
            new StarModification.Builder()
                .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
                .colony_id(colony.id)
                .count(count)
                .design_type(design.type)
                // TODO: upgrades?
                .build());
      } catch (SuspiciousModificationException e) {
        log.error("Suspicious modification?", e);
        return;
      }
      // find the build request with ID 0, that's our guy

      Star updatedStar = starBuilder.build();
      for (BuildRequest buildRequest : BuildHelper.getBuildRequests(updatedStar)) {
        if (buildRequest.id == 0) {
          App.i.getTaskRunner().runTask(() -> {
            buildTime.setText(BuildHelper.formatTimeRemaining(buildRequest));
            EmpireStorage newEmpireStorage = BuildHelper.getEmpireStorage(updatedStar, myEmpire.id);
            EmpireStorage oldEmpireStorage = BuildHelper.getEmpireStorage(star, myEmpire.id);
            if (newEmpireStorage != null && oldEmpireStorage != null) {
              float mineralsDelta = newEmpireStorage.minerals_delta_per_hour
                  - oldEmpireStorage.minerals_delta_per_hour;
              buildMinerals.setText(String.format(Locale.US, "%s%.1f/hr",
                  mineralsDelta < 0 ? "-" : "+", Math.abs(mineralsDelta)));
              buildMinerals.setTextColor(mineralsDelta < 0 ? Color.RED : Color.GREEN);
            } else {
              buildMinerals.setText("");
            }
          }, Threads.UI);
        }
      }
    }, Threads.BACKGROUND);
  }

  /** Start building the thing we currently have showing. */
  public void build() {
    String str = buildCount.getText().toString();
    int count;
    try {
      count = Integer.parseInt(str);
    } catch (NumberFormatException e) {
      count = 1;
    }

    if (count <= 0) {
      return;
    }

    callback.onBuild(design.type, count);
  }

  private final SeekBar.OnSeekBarChangeListener buildCountSeekBarChangeListener =
      new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean userInitiated) {
          if (!userInitiated) {
            return;
          }
          buildCount.setText(String.format(Locale.US, "%d", progress));
          updateBuildTime();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
      };
}
