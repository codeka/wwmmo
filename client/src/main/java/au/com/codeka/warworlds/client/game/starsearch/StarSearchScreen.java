package au.com.codeka.warworlds.client.game.starsearch;

import android.view.ViewGroup;

import au.com.codeka.warworlds.client.game.solarsystem.SolarSystemScreen;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;

public class StarSearchScreen extends Screen {
  private StarSearchLayout layout;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    layout =
        new StarSearchLayout(
            context.getActivity(),
            star -> context.pushScreen(new SolarSystemScreen(star, -1)));
  }

  @Override
  public ShowInfo onShow() {
    return ShowInfo.builder().view(layout).build();
  }
}
