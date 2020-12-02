package au.com.codeka.warworlds;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * This fragment is shown the first time you start the game. We give you a quick intro, some links
 * to the website and stuff like that.
 */
public class WarmWelcomeFragment extends BaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.warm_welcome, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ActivityBackgroundGenerator.setBackground(view);

    TransparentWebView welcome = view.findViewById(R.id.welcome);
    String msg = TransparentWebView.getHtmlFile(requireContext(), "html/warm-welcome.html");
    welcome.loadHtml("html/skeleton.html", msg);

    view.findViewById(R.id.start_btn).setOnClickListener(v -> {
      // save the fact that we've finished the warm welcome
      SharedPreferences prefs = Util.getSharedPreferences();
      prefs.edit().putBoolean("WarmWelcome", true).apply();

      // this activity is finished, move to realmSelectFragment if there's more than one to choose
      // from, or straight to welcomeFragment if there's just one.
      if (RealmManager.i.getRealms().size() == 1) {
        // if there's only one realm, select it and move on.
        RealmManager.i.selectRealm(RealmManager.i.getRealms().get(0).getDisplayName());
        NavHostFragment.findNavController(this).navigate(
            R.id.welcomeFragment,
            null,
            new NavOptions.Builder()
                .setPopUpTo(R.id.welcomeFragment, true)
                .build());
      } else {
        NavHostFragment.findNavController(this).navigate(R.id.realmSelectFragment);
      }
    });

    view.findViewById(R.id.help_btn).setOnClickListener(v -> {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
      startActivity(i);
    });

    view.findViewById(R.id.privacy_policy_btn).setOnClickListener(v -> {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://www.war-worlds.com/privacy-policy"));
      startActivity(i);
    });
  }
}
