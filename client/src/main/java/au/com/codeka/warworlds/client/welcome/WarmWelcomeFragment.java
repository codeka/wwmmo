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
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
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
    return inflater.inflate(R.layout.frag_warm_welcome, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    ViewBackgroundGenerator.setBackground(view);

    TransparentWebView welcome = (TransparentWebView) view.findViewById(R.id.welcome);
    String msg = TransparentWebView.getHtmlFile(getContext(), "html/warm-welcome.html");
    welcome.loadHtml("html/skeleton.html", msg);

    view.findViewById(R.id.next_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // save the fact that we've finished the warm welcome
        GameSettings.i.edit()
            .setBoolean(GameSettings.Key.WARM_WELCOME_SEEN, true)
            .commit();

        getFragmentTransitionManager().replaceFragment(
            CreateEmpireFragment.class,
            SharedViewHolder.builder()
                .addSharedView(R.id.title_icon, "title_icon")
                .addSharedView(R.id.title, "title")
                .addSharedView(R.id.next_btn, "done_btn")
                .build());
      }
    });

    view.findViewById(R.id.help_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
        startActivity(i);
      }
    });

    view.findViewById(R.id.privacy_policy_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://www.war-worlds.com/privacy-policy"));
        startActivity(i);
      }
    });
  }
}
