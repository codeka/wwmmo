package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;

/**
 * Bottom pane of the fleets view that contains the "move" function.
 */
public class MoveBottomPane extends RelativeLayout {
  /** Implement this when you want to get notified about when we want to close this pane. */
  public interface Callback {
    void onClose();
  }

  private final Callback callback;

  public MoveBottomPane(Context context, @Nonnull Callback callback) {
    super(context, null);
    this.callback = callback;

    inflate(context, R.layout.ctrl_fleet_move_bottom_pane, this);

    findViewById(R.id.move_btn).setOnClickListener(this::onMoveClick);
    findViewById(R.id.cancel_btn).setOnClickListener(this::onCancelClick);
  }

  private void onMoveClick(View view) {
    // TODO: move

    callback.onClose();
  }

  private void onCancelClick(View view) {
    callback.onClose();
  }
}
