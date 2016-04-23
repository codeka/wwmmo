package au.com.codeka.warworlds.client.welcome;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;

/**
 * This fragment is shown the first time you start the game. We give you a quick intro, some links
 * to the website and stuff like that.
 */
public class WarmWelcomeFragment extends BaseFragment {
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.frag_warm_welcome, container, false);
    ViewBackgroundGenerator.setBackground(rootView);

    TransparentWebView welcome = (TransparentWebView) rootView.findViewById(R.id.welcome);
    String msg = TransparentWebView.getHtmlFile(getContext(), "html/warm-welcome.html");
    welcome.loadHtml("html/skeleton.html", msg);

    rootView.findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // save the fact that we've finished the warm welcome
        GameSettings.i.edit()
            .setBoolean(GameSettings.Key.WARM_WELCOME_SEEN, true)
            .commit();

        // and move to the Welcome fragment
        getFragmentTransitionManager().replaceFragment(WelcomeFragment.class);
      }
    });

    rootView.findViewById(R.id.help_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
        startActivity(i);
      }
    });

    rootView.findViewById(R.id.privacy_policy_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/privacy-policy"));
        startActivity(i);
      }
    });

    return rootView;
  }
}
