package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bottom pane of the fleets view that contains the "merge" function.
 */
public class MergeBottomPane extends RelativeLayout {
  public interface Callback {
    void onCancel();
  }

  private final Callback callback;

  /** The fleet we're splitting, may be null if {@link #setFleet(Star, long)} hasn't been called. */
  @Nullable
  Fleet fleet;

  /** The star of the fleet we're splitting. */
  @Nullable Star star;

  public MergeBottomPane(Context context, @Nonnull Callback callback) {
    super(context, null);
    this.callback = checkNotNull(callback);

    inflate(context, R.layout.ctrl_fleet_merge_bottom_pane, this);
    findViewById(R.id.merge_btn).setOnClickListener(this::onMergeClick);
    findViewById(R.id.cancel_btn).setOnClickListener(this::onCancelClick);

  }

  /** Set the fleet we're merging to the one with the given ID on the given star. */
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

//    update(leftCount, rightCount);
  }

  private void onMergeClick(View view) {
    callback.onCancel();
  }

  private void onCancelClick(View view) {
    callback.onCancel();
  }
}
