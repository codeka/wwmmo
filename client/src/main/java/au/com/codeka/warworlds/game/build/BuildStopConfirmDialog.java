package au.com.codeka.warworlds.game.build;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class BuildStopConfirmDialog extends DialogFragment {
  private BuildRequest mBuildRequest;
  private Star mStar;

  public void setBuildRequest(Star star, BuildRequest buildRequest) {
    mBuildRequest = buildRequest;
    mStar = star;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    String msg =
        "Are you <em>sure</em> you want to stop this build? Stopping the " +
            "build will not return any resources to you, though it will free " +
            "up your population to work on other constructions.";

    return new StyledDialog.Builder(getActivity())
        .setTitle("Stop Build")
        .setMessage(Html.fromHtml(msg))
        .setPositiveButton("Stop Build", (dialog, which) -> stopBuild())
        .setNegativeButton("Cancel", null)
        .create();
  }

  private void stopBuild() {
    final StyledDialog dialog = ((StyledDialog) getDialog());
    dialog.setCloseable(false);

    App.i.getTaskRunner().runTask(() -> {
      String url = "stars/" + mStar.getKey() + "/build/" + mBuildRequest.getKey() + "/stop";

      try {
        ApiClient.postProtoBuf(url, null);
        App.i.getTaskRunner().runTask(() -> {
          EmpireManager.i.refreshEmpire();
          StarManager.i.refreshStar(mStar.getID());
          dismiss();
        }, Threads.UI);
      } catch (ApiException e) {
        //log.error("Error issuing build request", e);
      }
    }, Threads.BACKGROUND);
  }
}
