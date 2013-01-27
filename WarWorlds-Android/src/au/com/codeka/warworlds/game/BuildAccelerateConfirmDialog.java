package au.com.codeka.warworlds.game;

import java.util.Locale;

import org.joda.time.Duration;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class BuildAccelerateConfirmDialog extends DialogFragment {
    private BuildRequest mBuildRequest;
    private Star mStar;

    public void setBuildRequest(Star star, BuildRequest buildRequest) {
        mBuildRequest = buildRequest;
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Duration speedUpTime = Duration.millis(
                mBuildRequest.getRemainingTime().getMillis() / 2);
        if (speedUpTime.compareTo(Duration.standardMinutes(10)) < 0) {
            // if it's less than a 10 minute speed up, we make it instant
            speedUpTime = mBuildRequest.getRemainingTime();
        }

        float speedUpTimeInHours = speedUpTime.toStandardSeconds().getSeconds() / 3600.0f;
        float cost = speedUpTimeInHours * 100.0f;

        if (cost > EmpireManager.getInstance().getEmpire().getCash()) {
            String msg = String.format(Locale.ENGLISH,
                    "Accelerating this build would cost <font color=\"red\">$%d</font> "+
                    "(and speed up the build by %s), but you don't have that much cash "+
                    "available. Do you want to buy some cash now?",
                    (int) Math.floor(cost), TimeInHours.format(speedUpTime));

            return new StyledDialog.Builder(getActivity())
            .setTitle("Accelerate Build")
            .setMessage(Html.fromHtml(msg))
            .setPositiveButton("Buy Cash", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                                  // TODO
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
        } else {
            String msg = String.format(Locale.ENGLISH,
                    "Do you want to accelerate this build? It will cost <font color=\"green\">"+
                    "$%d</font> and speed up the build by %s.",
                    (int) Math.floor(cost), TimeInHours.format(speedUpTime));

            return new StyledDialog.Builder(getActivity())
                               .setMessage(Html.fromHtml(msg))
                               .setPositiveButton("Accelerate", new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog, int which) {
                                       accelerateBuild();
                                   }
                               })
                               .setNegativeButton("Cancel", null)
                               .create();
        }
    }

    private void accelerateBuild() {
        final StyledDialog dialog = ((StyledDialog) getDialog());
        dialog.setCloseable(false);

        new AsyncTask<Void, Void, BuildRequest>() {
            @Override
            protected BuildRequest doInBackground(Void... arg0) {
                String url = "stars/"+mStar.getKey()+"/build/"+mBuildRequest.getKey()+"/accelerate";

                try {
                    Messages.BuildRequest build = ApiClient.postProtoBuf(url, null, Messages.BuildRequest.class);

                    return BuildRequest.fromProtocolBuffer(build);
                } catch (ApiException e) {
                    //log.error("Error issuing build request", e);
                }

                return null;
            }
            @Override
            protected void onPostExecute(BuildRequest buildRequest) {
                // tell the StarManager that this star has been updated
                StarManager.getInstance().refreshStar(getActivity(), mStar.getKey());

                // tell the EmpireManager to update the empire (since our cash will have gone down)
                EmpireManager.getInstance().refreshEmpire();

                dismiss();
            }
        }.execute();
    }
}
