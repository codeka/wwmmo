package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bottom pane of the fleets view that contains the "split" function.
 */
public class SplitBottomPane extends RelativeLayout {
  public interface Callback {
    void onCancel();
  }

  private final Callback callback;

  public SplitBottomPane(Context context, @Nonnull Callback callback) {
    super(context, null);
    this.callback = checkNotNull(callback);

    inflate(context, R.layout.ctrl_fleet_split_bottom_pane, this);
    //FleetListHelper.populateFleetRow(findViewById(R.id.fleet), );

    findViewById(R.id.cancel_btn).setOnClickListener(this::onCancelClick);
  }

  private void onCancelClick(View view) {
    callback.onCancel();
  }
}
