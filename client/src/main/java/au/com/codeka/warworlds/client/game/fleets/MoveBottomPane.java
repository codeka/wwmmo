package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.starfield.StarfieldHelper;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.opengl.SceneObject;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

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

    SceneObject starSceneObject = starfieldManager.getStarSceneObject(star.id);
    if (starSceneObject == null) {
      return;
    }

    fleetMoveIndicator = starfieldManager.createFleetSprite(fleet);
    fleetMoveIndicator.setDrawRunnable(() -> {
      Preconditions.checkNotNull(fleetMoveIndicator);

      if (destStar != null) {
        fleetMoveIndicatorFraction += 0.016f; // TODO: pass in dt here?
        while (fleetMoveIndicatorFraction > 1.0f) {
          fleetMoveIndicatorFraction -= 1.0f;
        }

        Vector2 dir = StarfieldHelper.directionBetween(star, destStar);

        Vector2 dirUnit = new Vector2(dir.x, dir.y);
        dirUnit.normalize();
        float angle = Vector2.angleBetween(dirUnit, new Vector2(0, -1));
        log.info("angle: %.2f", angle);
        fleetMoveIndicator.setRotation(angle, 0, 0, 1);

        dir.scale(1.0, -1.0);
        dir.scale(fleetMoveIndicatorFraction);
     //   fleetMoveIndicator.setTranslation((float) dir.x, (float) dir.y);

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
    // TODO: move

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

      refreshMoveIndicator();
      destStar = star;
    }
  };
}
