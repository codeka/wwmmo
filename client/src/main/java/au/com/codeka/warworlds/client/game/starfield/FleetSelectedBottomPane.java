package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.widget.RelativeLayout;

import au.com.codeka.warworlds.client.R;

/**
 * Bottom pane for when you have a fleet selected.
 */
public class FleetSelectedBottomPane extends RelativeLayout {
  public FleetSelectedBottomPane(Context context) {
    super(context);

    inflate(context, R.layout.starfield_bottom_pane_fleet, this);
  }
}
