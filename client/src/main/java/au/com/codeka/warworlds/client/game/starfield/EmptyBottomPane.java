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
  public interface Callback {
    void onEmpireClicked(View view);
    void onSitrepClicked(View view);
    void onAllianceClicked(View view);
  }

  public EmptyBottomPane(Context context, @Nonnull Callback callback) {
    super(context, null);
    inflate(context, R.layout.starfield_bottom_pane_empty, this);
    findViewById(R.id.empire_btn).setOnClickListener(callback::onEmpireClicked);
    findViewById(R.id.sitrep_btn).setOnClickListener(callback::onSitrepClicked);
    findViewById(R.id.alliance_btn).setOnClickListener(callback::onAllianceClicked);
  }
}

