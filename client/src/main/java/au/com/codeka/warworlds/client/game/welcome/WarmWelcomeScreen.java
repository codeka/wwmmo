package au.com.codeka.warworlds.client.game.welcome;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.SharedViews;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.client.util.GameSettings;

/**
 * This fragment is shown the first time you start the game. We give you a quick intro, some links
 * to the website and stuff like that.
 */
public class WarmWelcomeScreen extends Screen {
  private WarmWelcomeLayout layout;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);

    layout = new WarmWelcomeLayout(context.getActivity(), new WarmWelcomeLayout.Callbacks() {
      @Override
      public void onStartClick() {
        // save the fact that we've finished the warm welcome
        GameSettings.i.edit()
            .setBoolean(GameSettings.Key.WARM_WELCOME_SEEN, true)
            .commit();

        context.pushScreen(
            new CreateEmpireScreen(),
            SharedViews.builder()
                .addSharedView(R.id.title_icon)
                .addSharedView(R.id.title)
                .addSharedView(R.id.next_btn)
                .build());
      }

      @Override
      public void onPrivacyPolicyClick() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/privacy-policy"));
        context.startActivity(i);
      }

      @Override
      public void onHelpClick() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
        context.startActivity(i);
      }
    });
  }

  @Override
  public ShowInfo onShow() {
    return ShowInfo.builder().view(layout).toolbarVisible(false).build();
  }
}
