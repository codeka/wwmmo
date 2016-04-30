package au.com.codeka.warworlds.client.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.net.HttpRequest;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.proto.NewAccountRequest;

/**
 * This fragment is shown when you don't have a cookie saved. We'll want to either let you create
 * a new empire, or sign in with an existing account (if you have one).
 */
public class CreateEmpireFragment extends BaseFragment {
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.frag_create_empire, container, false);
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
            .url("http://192.168.1.3:8080/accounts")
            .method(HttpRequest.Method.POST)
            .header("Content-Type", "application/x-protobuf")
            .body(new NewAccountRequest.Builder()
                .empire_name(empireName)
                .build().encode())
            .build();
        byte[] respBytes = request.getBody();
        if (respBytes == null) {
          // TODO: report the error
          App.i.getTaskRunner().runTask(new Runnable() {
            @Override
            public void run() {
              onRegisterError(rootView, "An unknown error occurred.");
            }
          }, Threads.UI);
        } else {
          App.i.getTaskRunner().runTask(new Runnable() {
            @Override
            public void run() {
              onRegisterSuccess(rootView);
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

  private void onRegisterSuccess(View rootView) {
    // TODO: save the cookie
    // TODO: tell Server we can now connect.
    getFragmentTransitionManager().replaceFragment(
        WelcomeFragment.class,
        SharedViewHolder.builder()
            .addSharedView(R.id.title_icon, "title_icon")
            .addSharedView(R.id.title, "title")
            .addSharedView(R.id.done_btn, "start_btn")
            .build());
  }
}
