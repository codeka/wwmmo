package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.DesignHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bottom pane of the fleets view that contains the "split" function.
 */
public class SplitBottomPane extends RelativeLayout {
  public interface Callback {
    void onCancel();
  }

  private final Callback callback;
  private final ViewGroup fleetDetails;
  private final EditText splitLeft;
  private final EditText splitRight;
  private final SeekBar splitRatio;

  /** If true, we should ignore callbacks because we're currently editing it in code. */
  private boolean ignoreEdits;

  /** The fleet we're splitting, may be null if {@link #setFleet(Star, long)} hasn't been called. */
  @Nullable Fleet fleet;

  /** The star of the fleet we're splitting. */
  @Nullable Star star;

  public SplitBottomPane(Context context, @Nonnull Callback callback) {
    super(context, null);
    this.callback = checkNotNull(callback);

    inflate(context, R.layout.ctrl_fleet_split_bottom_pane, this);

    fleetDetails = findViewById(R.id.fleet);
    splitLeft = findViewById(R.id.split_left);
    splitRight = findViewById(R.id.split_right);
    splitRatio = findViewById(R.id.split_ratio);

    findViewById(R.id.split_btn).setOnClickListener(this::onSplitClick);
    findViewById(R.id.cancel_btn).setOnClickListener(this::onCancelClick);
    splitLeft.addTextChangedListener(new SplitTextWatcher(true));
    splitRight.addTextChangedListener(new SplitTextWatcher(false));
    splitRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !ignoreEdits && fleet != null) {
          update(progress, (int) Math.floor(fleet.num_ships) - progress);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });
  }

  /** Set the fleet we're displaying to the one with the given ID on the given star. */
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

    FleetListHelper.populateFleetRow(
        fleetDetails,
        star,
        fleet,
        DesignHelper.getDesign(fleet.design_type));

    int leftCount = (int) Math.floor(fleet.num_ships) / 2;
    int rightCount = (int) Math.floor(fleet.num_ships) - leftCount;
    update(leftCount, rightCount);
  }

  private void update(int leftCount, int rightCount) {
    ignoreEdits = true;
    splitLeft.setText(Integer.toString(leftCount));
    splitRight.setText(Integer.toString(rightCount));
    splitRatio.setMax(leftCount + rightCount);
    splitRatio.setProgress(leftCount);
    ignoreEdits = false;
  }

  private void onSplitClick(View view) {
    if (star == null || fleet == null) {
      return;
    }
    StarManager.i.updateStar(star, new StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.SPLIT_FLEET)
        .fleet_id(fleet.id)
        .count(splitRatio.getMax() - splitRatio.getProgress()));

    callback.onCancel();
  }

  private void onCancelClick(View view) {
    callback.onCancel();
  }

  private class SplitTextWatcher implements TextWatcher {
    private final boolean isLeft;

    SplitTextWatcher(boolean isLeft) {
      this.isLeft = isLeft;
    }

    @Override
    public void afterTextChanged(Editable editable) {
      if (fleet == null || ignoreEdits) {
        return;
      }

      int leftCount;
      int rightCount;
      if (isLeft) {
        leftCount = Integer.parseInt(editable.toString());
        rightCount = (int) Math.floor(fleet.num_ships) - leftCount;
      } else {
        rightCount = Integer.parseInt(editable.toString());
        leftCount = (int) Math.floor(fleet.num_ships) - rightCount;
      }
      update(leftCount, rightCount);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after) {
    }
  };
}
