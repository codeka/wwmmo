package au.com.codeka.warworlds.client.game.empire;

import android.content.Context;
import android.support.design.widget.TabLayout;

/**
 * Layout for the {@link EmpireScreen}.
 */
public class EmpireLayout extends TabLayout {
  public EmpireLayout(Context context) {
    super(context);

    addTab(newTab().setText("Overview"));
    addTab(newTab().setText("Colonies"));
    addTab(newTab().setText("Build"));
    addTab(newTab().setText("Fleets"));
  }
}
