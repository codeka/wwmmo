package au.com.codeka.warworlds;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import au.com.codeka.warworlds.model.Realm;
import au.com.codeka.warworlds.model.RealmManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class RealmSelectFragment extends BaseFragment {
  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.realm_select, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ActivityBackgroundGenerator.setBackground(view);

    if (RealmManager.i.getRealms().size() == 1) {
      // if there's only one realm, select it and move on.
      RealmManager.i.selectRealm(RealmManager.i.getRealms().get(0).getDisplayName());
      navigateToWelcome();
      return;
    }

    CharSequence[] realmNames = new CharSequence[RealmManager.i.getRealms().size()];
    for (int i = 0; i < realmNames.length; i++) {
      Realm realm = RealmManager.i.getRealms().get(i);
      realmNames[i] = Html.fromHtml(String
          .format("<font color=\"#ffffff\"><b>%s</b></font><br/><small>%s</small>",
              realm.getDisplayName(), realm.getDescription()));
    }

    final ListView realmsListView = view.findViewById(R.id.realms);
    realmsListView.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.account, realmNames));
    realmsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    for (int i = 0; i < realmNames.length; i++) {
      Realm realm = RealmManager.i.getRealms().get(i);
      if (realm.getDisplayName().equals("Default")) {
        realmsListView.setItemChecked(i, true);
      }
    }

    Button startBtn = view.findViewById(R.id.start_btn);
    startBtn.setOnClickListener(v -> {
      int selectedPosition = realmsListView.getCheckedItemPosition();
      Realm realm = RealmManager.i.getRealms().get(selectedPosition);

      // we need to register on the new realm, but we don't necessarily want to unregister from the
      // old realm...
      DeviceRegistrar.unregister(false);

      RealmManager.i.selectRealm(realm.getDisplayName());
      navigateToWelcome();
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

  private void navigateToWelcome() {
    NavHostFragment.findNavController(this).navigate(
        R.id.welcomeFragment,
        null,
        new NavOptions.Builder()
            .setPopUpTo(R.id.welcomeFragment, true)
            .build());
  }
}
