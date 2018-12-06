package au.com.codeka.warworlds.client.game.empire;

import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.net.HttpRequest;
import au.com.codeka.warworlds.client.net.ServerUrl;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.PatreonBeginRequest;
import au.com.codeka.warworlds.common.proto.PatreonBeginResponse;

/**
 * This screen shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireScreen extends Screen {
  private static final Log log = new Log("EmpireScreen");

  private ScreenContext context;
  private EmpireLayout layout;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);
    this.context = context;
    layout = new EmpireLayout(context.getActivity(), new SettingsCallbacks());
  }

  @Override
  public ShowInfo onShow() {
    return ShowInfo.builder().view(layout).build();
  }

  private class SettingsCallbacks implements SettingsView.Callback {
    @Override
    public void onPatreonConnectClick(
        SettingsView.PatreonConnectCompleteCallback completeCallback) {
      App.i.getTaskRunner().runTask(() -> {
        HttpRequest req = new HttpRequest.Builder()
            .url(ServerUrl.getUrl("/accounts/patreon-begin"))
            .authenticated()
            .body(new PatreonBeginRequest.Builder()
                .empire_id(EmpireManager.i.getMyEmpire().id)
                .build().encode())
            .method(HttpRequest.Method.POST)
            .build();
        if (req.getResponseCode() != 200 || req.getException() != null) {
          // TODO: better error handling.
          log.error("Error starting patreon connect request: %d %s",
              req.getResponseCode(), req.getException());
          completeCallback.onPatreonConnectComplete("Unexpected error.");
          return;
        }

        PatreonBeginResponse resp = req.getBody(PatreonBeginResponse.class);
        if (resp == null) {
          // TODO: better error handling.
          log.error("Got an empty response?");
          completeCallback.onPatreonConnectComplete("Unexpected error.");
          return;
        }

        final String uri = "https://www.patreon.com/oauth2/authorize?response_type=code"
            + "&client_id=" + resp.client_id
            + "&redirect_uri=" + Uri.encode(resp.redirect_uri)
            + "&state=" + Uri.encode(resp.state);
        log.info("Opening URL: %s", uri);
        App.i.getTaskRunner().runTask(() -> {
          Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
          context.getActivity().startActivity(intent);
        }, Threads.UI);
      }, Threads.BACKGROUND);
    }
  }
}
