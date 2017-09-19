package au.com.codeka.warworlds.client.game.empire;

import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.common.Log;

/**
 * This screen shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireScreen extends Screen {
  private static final Log log = new Log("EmpireScreen");

  private EmpireLayout layout;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    layout = new EmpireLayout(context.getActivity());
  }

  @Override
  public View onShow() {
    return layout;
  }
}
