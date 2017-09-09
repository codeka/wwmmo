package au.com.codeka.warworlds.client.game.welcome;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.SharedViewHolder;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.net.HttpRequest;
import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.NewAccountRequest;
import au.com.codeka.warworlds.common.proto.NewAccountResponse;

/**
 * This screen is shown when you don't have a cookie saved. We'll want to either let you create
 * a new empire, or sign in with an existing account (if you have one).
 */
public class CreateEmpireScreen extends Screen {
  private static final Log log = new Log("CreateEmpireScreen");

  private CreateEmpireLayout layout;
  private ScreenContext context;

  @Override
  public void onCreate(ScreenContext context, LayoutInflater inflater, ViewGroup container) {
    layout = new CreateEmpireLayout(inflater.getContext(), layoutCallbacks);
    this.context = context;
  }

  @Override
  public View onShow() {
    return layout;
  }

  private final CreateEmpireLayout.Callbacks layoutCallbacks = new CreateEmpireLayout.Callbacks() {
    @Override
    public void onDoneClick(String empireName) {
      registerEmpire(empireName);
    }
  };

  private void registerEmpire(String empireName) {
    layout.showSpinner();

    App.i.getTaskRunner().runTask(() -> {
      HttpRequest request = new HttpRequest.Builder()
          .url(ServerUrl.getUrl() + "accounts")
          .method(HttpRequest.Method.POST)
          .header("Content-Type", "application/x-protobuf")
          .body(new NewAccountRequest.Builder()
              .empire_name(empireName)
              .build().encode())
          .build();
      NewAccountResponse resp = request.getBody(NewAccountResponse.class);
      if (resp == null) {
        // TODO: report the error to the server?
        log.error("Didn't get NewAccountResponse, as expected.", request.getException());
      } else if (resp.cookie == null) {
        App.i.getTaskRunner().runTask(() -> layout.showError(resp.message), Threads.UI);
      } else {
        log.info(
            "New account response, cookie: %s, message: %s",
            resp.cookie,
            resp.message);
        App.i.getTaskRunner().runTask(() -> onRegisterSuccess(resp), Threads.UI);
      }
    }, Threads.BACKGROUND);
  }

  private void onRegisterSuccess(NewAccountResponse resp) {
    // Save the cookie.
    GameSettings.i.edit()
        .setString(GameSettings.Key.COOKIE, resp.cookie)
        .commit();

    // Tell the Server we can now connect.
    App.i.getServer().connect();

    context.pushScreen(new WelcomeScreen());
  }
}
