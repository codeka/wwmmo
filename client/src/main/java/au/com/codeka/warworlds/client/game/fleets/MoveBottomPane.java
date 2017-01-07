package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bottom pane of the fleets view that contains the "move" function.
 */
public class MoveBottomPane extends RelativeLayout {
  /** Implement this when you want to get notified about when we want to close this pane. */
  public interface Callback {
    void onClose();
  }

  private final Callback callback;
  private final StarfieldManager starfieldManager;

  @Nullable private Star star;
  @Nullable private Fleet fleet;

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
  }

  /** Called when we've got a fleet and need to setup the starfield. */
  private void refreshStarfield() {
    starfieldManager.warpTo(star);
    starfieldManager.setSelectedStar(null);
  }

  private void onMoveClick(View view) {
    // TODO: move

    callback.onClose();
  }

  private void onCancelClick(View view) {
    callback.onClose();
  }
}
