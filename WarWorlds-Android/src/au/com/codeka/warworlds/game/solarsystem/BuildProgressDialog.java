package au.com.codeka.warworlds.game.solarsystem;

import java.util.Locale;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildQueueManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class BuildProgressDialog extends DialogFragment {
    private static Logger log = LoggerFactory.getLogger(BuildProgressDialog.class);

    private BuildRequest mBuildRequest;
    private String mStarKey;
    private View mView;

    public BuildProgressDialog() {
    }

    public void setBuildRequest(BuildRequest buildRequest, String starKey) {
        mBuildRequest = buildRequest;
        mStarKey = starKey;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.build_progress_dlg, null);

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setPositiveButton("Accelerate", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAccelerateClick();
            }
        });

        b.setNeutralButton("Stop", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                
            }
        });

        b.setNegativeButton("Close", null);

        refresh();
        return b.create();
    }

    private void onAccelerateClick() {
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

            StyledDialog dlg = new StyledDialog.Builder(getActivity())
                               .setTitle("Accelerate Build")
                               .setMessage(Html.fromHtml(msg))
                               .setPositiveButton("Buy Cash", new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                                     // TODO
                                   }
                               })
                               .setNegativeButton("Cancel", null)
                               .create();
            dlg.show();
        } else {
            String msg = String.format(Locale.ENGLISH,
                    "Do you want to accelerate this build? It will cost <font color=\"green\">"+
                    "$%d</font> and speed up the build by %s.",
                    (int) Math.floor(cost), TimeInHours.format(speedUpTime));

            final StyledDialog dlg = new StyledDialog.Builder(getActivity())
                               .setMessage(Html.fromHtml(msg))
                               .setPositiveButton("Accelerate", true, new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       accelerateBuild();
                                   }
                               })
                               .setNegativeButton("Cancel", null)
                               .create();
            dlg.show();
        }
    }

    private void accelerateBuild() {
        ((StyledDialog) getDialog()).getPositiveButton().setEnabled(false);

        new AsyncTask<Void, Void, BuildRequest>() {
            @Override
            protected BuildRequest doInBackground(Void... arg0) {
                String url = "stars/"+mStarKey+"/build/"+mBuildRequest.getKey()+"/accelerate";

                try {
                    Messages.BuildRequest build = ApiClient.postProtoBuf(url, null, Messages.BuildRequest.class);

                    return BuildRequest.fromProtocolBuffer(build);
                } catch (ApiException e) {
                    log.error("Error issuing build request", e);
                }

                return null;
            }
            @Override
            protected void onPostExecute(BuildRequest buildRequest) {
                ((StyledDialog) getDialog()).getPositiveButton().setEnabled(true);

                // notify the BuildQueueManager that something's changed.
                BuildQueueManager.getInstance().refresh(buildRequest);

                // tell the StarManager that this star has been updated
                StarManager.getInstance().refreshStar(getActivity(), mStarKey);

                if (buildRequest != null) {
                    mBuildRequest = buildRequest;
                    refresh();
                }
            }
        }.execute();

    }

    private void refresh() {
        Design design = DesignManager.getInstance(mBuildRequest.getBuildKind())
                                     .getDesign(mBuildRequest.getDesignID());

        ImageView imgView = (ImageView) mView.findViewById(R.id.build_icon);
        imgView.setImageDrawable(new SpriteDrawable(design.getSprite()));

        TextView designNameView = (TextView) mView.findViewById(R.id.build_design_name);
        if (mBuildRequest.getCount() == 1) {
            designNameView.setText(design.getDisplayName());
        } else {
            designNameView.setText(String.format("%s (× %d)",
                                   design.getDisplayName(),
                                   mBuildRequest.getCount()));
        }

        TextView buildTimeRemainingView = (TextView) mView.findViewById(R.id.build_time_remaining);
        Duration remainingDuration = mBuildRequest.getRemainingTime();
        if (remainingDuration.equals(Duration.ZERO)) {
            buildTimeRemainingView.setText(String.format("%d %%, not enough resources to complete.",
                              (int) mBuildRequest.getPercentComplete()));
        } else {
            buildTimeRemainingView.setText(String.format("%d %%, %s left",
                              (int) mBuildRequest.getPercentComplete(),
                              TimeInHours.format(remainingDuration)));
        }

        ProgressBar buildProgressBar = (ProgressBar) mView.findViewById(R.id.build_progress);
        buildProgressBar.setProgress((int) mBuildRequest.getPercentComplete());
    }
}
