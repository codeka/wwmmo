package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.sim.StarHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bottom pane of the fleets view that contains the "move" function.
 */
public class MoveBottomPane extends RelativeLayout {
  private static final Log log = new Log("MoveBottomPane");

  /** Implement this when you want to get notified about when we want to close this pane. */
  public interface Callback {
    void onClose();
  }

  private final Callback callback;
  private final StarfieldManager starfieldManager;

  @Nullable private Star star;
  @Nullable private Fleet fleet;

  @Nullable private Star destStar;

  @Nullable private SceneObject fleetMoveIndicator;
  private float fleetMoveIndicatorFraction;

  public MoveBottomPane(
      Context context,
      @Nonnull StarfieldManager starfieldManager,
      @Nonnull Callback callback) {
    super(context, null);
    this.callback = checkNotNull(callback);
    this.starfieldManager = checkNotNull(starfieldManager);

    inflate(context, R.layout.ctrl_fleet_move_bottom_pane, this);

    findViewById(R.id.move_btn).setOnClickListener(this::onMoveClick);
    findViewById(R.id.cancel_btn).setOnClickListener(this::onCancelClick);
  }

  public void setFleet(Star star, long fleetId) {
    for (Fleet fleet : star.fleets) {
      if (fleet.id.equals(fleetId)) {
        setFleet(star, fleet);
      }
    }
  }

  private void setFleet(Star star, Fleet fleet) {
    this.star = star;
    this.fleet = fleet;
    if (isAttachedToWindow()) {
      refreshStarfield();
    }
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (star != null && fleet != null) {
      refreshStarfield();
    }

    starfieldManager.addTapListener(tapListener);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    starfieldManager.removeTapListener(tapListener);
    destroyFleetMoveIndicator();
  }

  /** Called when we've got a fleet and need to setup the starfield. */
  private void refreshStarfield() {
    starfieldManager.warpTo(star);
    starfieldManager.setSelectedStar(null);
    refreshMoveIndicator();
  }

  /**
   * Refreshes the "move indicator", which is the little ship icon on the starfield that shows
   * where you're going to move to.
   */
  private void refreshMoveIndicator() {
    destroyFleetMoveIndicator();

    if (star == null || fleet == null) {
      return;
    }

    if (destStar != null) {
      double distanceInParsecs = StarHelper.distanceBetween(star, destStar);
      String leftDetails = String
          .format(Locale.ENGLISH, "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
              destStar.name, distanceInParsecs);
      ((TextView) findViewById(R.id.star_details_left)).setText(Html.fromHtml(leftDetails));

      Design design = DesignHelper.getDesign(fleet.design_type);
      double timeInHours = distanceInParsecs / design.speed_px_per_hour;
      int hrs = (int) Math.floor(timeInHours);
      int mins = (int) Math.floor((timeInHours - hrs) * 60.0f);

      double estimatedFuel = design.fuel_cost_per_px * distanceInParsecs * fleet.num_ships;
      double actualFuel = fleet.fuel_amount == null ? 0 : fleet.fuel_amount;
      String fuel = String.format(Locale.US, "%.1f / %.1f", estimatedFuel, actualFuel);

      String fontOpen = "";
      String fontClose = "";
      EmpireStorage storage = StarHelper.getStorage(star, EmpireManager.i.getMyEmpire().id);
      if (storage == null || estimatedFuel > actualFuel) {
        fontOpen = "<font color=\"#ff0000\">";
        fontClose = "</font>";
      }

      String rightDetails = String.format(
          Locale.ENGLISH,
          "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Energy:</b> %s%s",
          hrs, mins, fontOpen, fuel, fontClose);
      ((TextView) findViewById(R.id.star_details_right)).setText(Html.fromHtml(rightDetails));
    }

    SceneObject starSceneObject = starfieldManager.getStarSceneObject(star.id);
    if (starSceneObject == null) {
      return;
    }

    fleetMoveIndicator = starfieldManager.createFleetSprite(fleet);
    fleetMoveIndicator.setDrawRunnable(() -> {
      Preconditions.checkNotNull(fleetMoveIndicator);

      if (destStar != null) {
        fleetMoveIndicatorFraction += 0.032f; // TODO: pass in dt here?
        while (fleetMoveIndicatorFraction > 1.0f) {
          fleetMoveIndicatorFraction -= 1.0f;
        }

        Vector2 dir = StarHelper.directionBetween(star, destStar);

        Vector2 dirUnit = new Vector2(dir.x, dir.y);
        dirUnit.normalize();
        float angle = Vector2.angleBetween(dirUnit, new Vector2(0, -1));
        fleetMoveIndicator.setRotation(angle, 0, 0, 1);

        dir.scale(fleetMoveIndicatorFraction);
        fleetMoveIndicator.setTranslation(0.0f, (float) dir.length());
      }
    });
    starSceneObject.addChild(fleetMoveIndicator);
  }

  private void destroyFleetMoveIndicator() {
    if (fleetMoveIndicator != null) {
      if (fleetMoveIndicator.getParent() != null) {
        fleetMoveIndicator.getParent().removeChild(fleetMoveIndicator);
      }
      fleetMoveIndicator = null;
    }
  }

  private void onMoveClick(View view) {
    Preconditions.checkNotNull(star);
    Preconditions.checkNotNull(fleet);
    if (destStar == null) {
      return;
    }

    StarManager.i.updateStar(star, new StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.MOVE_FLEET)
        .fleet_id(fleet.id)
        .star_id(destStar.id));

    callback.onClose();
  }

  private void onCancelClick(View view) {
    callback.onClose();
  }

  private final StarfieldManager.TapListener tapListener = new StarfieldManager.TapListener() {
    @Override
    public void onStarTapped(@Nullable Star star) {
      if (star == null) {
        destStar = null;
        refreshMoveIndicator();
        return;
      }

      if (MoveBottomPane.this.star != null && star.id.equals(MoveBottomPane.this.star.id)) {
        destStar = null;
        refreshMoveIndicator();
        return;
      }

      destStar = star;
      refreshMoveIndicator();
    }

    @Override
    public void onFleetTapped(@Nullable Star star, @Nullable Fleet fleet) {
      // TODO: indicate that we don't want to select this fleet.
    }
  };
}
