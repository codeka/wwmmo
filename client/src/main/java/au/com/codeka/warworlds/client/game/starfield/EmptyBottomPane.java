package au.com.codeka.warworlds.client.game.starfield;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;

/**
 * The bottom pane when you have nothing selected.
 */
public class EmptyBottomPane extends FrameLayout {

  public EmptyBottomPane(Context context) {
    super(context, null);
    inflate(context, R.layout.starfield_bottom_pane_empty, this);
  }
}

