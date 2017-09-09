package au.com.codeka.warworlds.client.game.welcome;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;

/**
 * This fragment is shown the first time you start the game. We give you a quick intro, some links
 * to the website and stuff like that.
 */
public class WarmWelcomeScreen extends Screen {
  private WarmWelcomeLayout layout;

  @Override
  public void onCreate(ScreenContext context, LayoutInflater layoutInflater, ViewGroup container) {
    layout = new WarmWelcomeLayout(layoutInflater.getContext(), new WarmWelcomeLayout.Callbacks() {
      @Override
      public void onStartClick() {
        // save the fact that we've finished the warm welcome
        GameSettings.i.edit()
            .setBoolean(GameSettings.Key.WARM_WELCOME_SEEN, true)
            .commit();

        context.pushScreen(new CreateEmpireScreen());
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
  public View onShow() {
    return layout;
  }
}
