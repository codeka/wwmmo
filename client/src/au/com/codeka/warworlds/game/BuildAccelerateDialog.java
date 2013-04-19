package au.com.codeka.warworlds.game;

import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

public class BuildAccelerateDialog extends DialogFragment {
    private BuildRequest mBuildRequest;
    private StarSummary mStar;
    private View mView;

    public void setBuildRequest(StarSummary star, BuildRequest buildRequest) {
        mBuildRequest = buildRequest;
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.build_accelerate_dlg, null);

        SeekBar accelerateAmount = (SeekBar) mView.findViewById(R.id.accelerate_amount);
        accelerateAmount.setMax(50);
        accelerateAmount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePercentAndCost();
            }
        });
        updatePercentAndCost();

        return new StyledDialog.Builder(getActivity())
                           .setView(mView)
                           .setPositiveButton("Accelerate", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   accelerateBuild();
                               }
                           })
                           .setNegativeButton("Cancel", null)
                           .create();
    }

    /**
     * This algorithm needs to be kept in sync with the on for accelerateBuild() in ctrl/empire.py
     * in the server.
     */
    private void updatePercentAndCost() {
        double accelerateAmount = getAccelerateAmount();

        TextView acceleratePct = (TextView) mView.findViewById(R.id.accelerate_pct);
        acceleratePct.setText(String.format(Locale.ENGLISH, "%d %%", (int)(accelerateAmount * 100)));

        double remainingProgress = 1.0 - mBuildRequest.getProgress(true);
        double progressToComplete = remainingProgress * accelerateAmount;

        Design design = DesignManager.i.getDesign(mBuildRequest.getDesignKind(), mBuildRequest.getDesignID());
        double mineralsToUse = design.getBuildCost().getCostInMinerals() * progressToComplete;
        double cost = mineralsToUse * mBuildRequest.getCount();

        TextView accelerateCost = (TextView) mView.findViewById(R.id.accelerate_cost);
        if (cost < EmpireManager.getInstance().getEmpire().getCash()) {
            accelerateCost.setText(String.format(Locale.ENGLISH, "$%d", (int) cost));
        } else {
            accelerateCost.setText(Html.fromHtml(String.format(Locale.ENGLISH,
                    "<font color=\"red\">$%d</font>", (int) cost)));
        }
    }

    private double getAccelerateAmount() {
        SeekBar seekBar = (SeekBar) mView.findViewById(R.id.accelerate_amount);
        return ((double) seekBar.getProgress() + 50.0) / 100.0;
    }

    private void accelerateBuild() {
        final Activity activity = getActivity();
        dismiss();

        new BackgroundRunner<BuildRequest>() {
            private String mErrorMsg;

            @Override
            protected BuildRequest doInBackground() {
                String url = "stars/"+mStar.getKey()+"/build/"+mBuildRequest.getKey()+"/accelerate";
                url += "?amount="+getAccelerateAmount();

                try {
                    Messages.BuildRequest pb = ApiClient.postProtoBuf(url, null, Messages.BuildRequest.class);
                    if (pb == null) {
                        return null;
                    }
                    
                    BuildRequest br = new BuildRequest();
                    br.fromProtocolBuffer(pb);
                    return br;
                } catch (ApiException e) {
                    if (e.getServerErrorCode() > 0) {
                        mErrorMsg = e.getServerErrorMessage();
                    }
                }

                return null;
            }
            @Override
            protected void onComplete(BuildRequest buildRequest) {
                // tell the StarManager that this star has been updated
                StarManager.getInstance().refreshStar(activity.getApplicationContext(), mStar.getKey());

                // tell the EmpireManager to update the empire (since our cash will have gone down)
                EmpireManager.getInstance().refreshEmpire(activity);

                if (mErrorMsg != null) {
                    new StyledDialog.Builder(activity.getApplicationContext())
                                    .setMessage(mErrorMsg)
                                    .setTitle("Error accelerating")
                                    .setNeutralButton("OK", null)
                                    .create().show(false);
                }
            }
        }.execute();
    }
}
