package au.com.codeka.warworlds.client.game.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.net.HttpRequest;
import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.NewAccountRequest;
import au.com.codeka.warworlds.common.proto.NewAccountResponse;

/**
 * This fragment is shown when you don't have a cookie saved. We'll want to either let you create
 * a new empire, or sign in with an existing account (if you have one).
 */
public class CreateEmpireFragment extends BaseFragment {
  private static final Log log = new Log("CreateEmpireFragment");

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_create_empire;
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    ViewBackgroundGenerator.setBackground(view);

    view.findViewById(R.id.done_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        registerEmpire(view);
      }
    });
  }

  private void registerEmpire(final View rootView) {
    // Hide the edit field and account switch button, show the progress bar
    rootView.findViewById(R.id.empire_name).setVisibility(View.GONE);
    rootView.findViewById(R.id.switch_account_btn).setVisibility(View.GONE);
    rootView.findViewById(R.id.progress).setVisibility(View.VISIBLE);

    final String empireName = ((EditText) rootView.findViewById(R.id.empire_name)).getText().toString();
    App.i.getTaskRunner().runTask(new Runnable() {
      @Override
      public void run() {
        HttpRequest request = new HttpRequest.Builder()
            .url(ServerUrl.getUrl() + "accounts")
            .method(HttpRequest.Method.POST)
            .header("Content-Type", "application/x-protobuf")
            .body(new NewAccountRequest.Builder()
                .empire_name(empireName)
                .build().encode())
            .build();
        NewAccountResponse resp = null;
        try {
          final byte[] respBytes = request.getBody();
          if (respBytes != null) {
            resp = NewAccountResponse.ADAPTER.decode(respBytes);
          }
        } catch (IOException e) {
          log.warning("Error decoding new account response.", e);
        }
        if (resp == null) {
          // TODO: report the error
          App.i.getTaskRunner().runTask(new Runnable() {
            @Override
            public void run() {
              onRegisterError(rootView, "An unknown error occurred.");
            }
          }, Threads.UI);
        } else {
          final NewAccountResponse finalResponse = resp;
          App.i.getTaskRunner().runTask(new Runnable() {
            @Override
            public void run() {
              onRegisterSuccess(finalResponse);
            }
          }, Threads.UI);
        }
      }
    }, Threads.BACKGROUND);
  }

  private void onRegisterError(View rootView, String errorMsg) {
    rootView.findViewById(R.id.empire_name).setVisibility(View.VISIBLE);
    rootView.findViewById(R.id.switch_account_btn).setVisibility(View.VISIBLE);
    rootView.findViewById(R.id.progress).setVisibility(View.GONE);
  }

  private void onRegisterSuccess(NewAccountResponse resp) {
    // Save the cookie.
    GameSettings.i.edit()
        .setString(GameSettings.Key.COOKIE, resp.cookie)
        .commit();

    // Tell the Server we can now connect.
    App.i.getServer().connect();

    getFragmentTransitionManager().replaceFragment(
        WelcomeFragment.class,
        SharedViewHolder.builder()
            .addSharedView(R.id.title_icon, "title_icon")
            .addSharedView(R.id.title, "title")
            .addSharedView(R.id.done_btn, "start_btn")
            .build());
  }
}
