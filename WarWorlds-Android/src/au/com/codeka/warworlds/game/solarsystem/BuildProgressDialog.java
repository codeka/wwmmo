package au.com.codeka.warworlds.game.solarsystem;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.FloatMath;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
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

public class BuildProgressDialog extends Dialog implements DialogManager.DialogConfigurable {
    private static Logger log = LoggerFactory.getLogger(BuildProgressDialog.class);

    public static final int ID = 48976;
    private Context mContext;
    private BuildRequest mBuildRequest;
    private String mStarKey;

    public BuildProgressDialog(Activity activity) {
        super(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.build_progress_dlg);

        Button accelerateBtn = (Button) findViewById(R.id.accelerate_btn);
        accelerateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Duration speedUpTime = Duration.millis(
                        mBuildRequest.getRemainingTime().getMillis() / 2);
                if (speedUpTime.compareTo(Duration.standardMinutes(10)) < 0) {
                    // if it's less than a 10 minute speed up, we make it instant
                    speedUpTime = mBuildRequest.getRemainingTime();
                }

                float speedUpTimeInHours = speedUpTime.toStandardSeconds().getSeconds() / 3600.0f;
                float cost = speedUpTimeInHours * 100.0f;

                if (cost > EmpireManager.getInstance().getEmpire().getCash()) {
                    String msg = String.format("Accelerating this build would cost "+
                                               "<font color=\"red\">$%d</font> (and speed up the "+
                                               "build by %s), but you don't have that much cash "+
                                               "available. Do you want to buy some cash now?",
                                               (int) FloatMath.floor(cost),
                                               TimeInHours.format(speedUpTime));

                    AlertDialog dlg = new AlertDialog.Builder(mContext)
                                                     .setCancelable(true)
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
                    dlg.show();
                } else {
                    String msg = String.format(
                            "Do you want to accelerate this build? It will cost <font color=\"green\">"+
                            "$%d</font> and speed up the build by %s.",
                            (int) FloatMath.floor(cost), TimeInHours.format(speedUpTime));

                    AlertDialog dlg = new AlertDialog.Builder(mContext)
                                                     .setCancelable(true)
                                                     .setMessage(Html.fromHtml(msg))
                                                     .setPositiveButton("Accelerate", new DialogInterface.OnClickListener() {
                                                         @Override
                                                         public void onClick(DialogInterface dialog, int which) {
                                                             accelerateBuild();
                                                         }
                                                     })
                                                     .setNegativeButton("Cancel", null)
                                                     .create();
                    dlg.show();
                }
            }
        });
    }

    private void accelerateBuild() {
        final Button accelerateBtn = (Button) findViewById(R.id.accelerate_btn);
        final Button cancelBtn = (Button) findViewById(R.id.cancel_btn);

        accelerateBtn.setEnabled(false);
        cancelBtn.setEnabled(false);

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
                accelerateBtn.setEnabled(true);
                cancelBtn.setEnabled(true);

                // notify the BuildQueueManager that something's changed.
                BuildQueueManager.getInstance().refresh(buildRequest);

                // tell the StarManager that this star has been updated
                StarManager.getInstance().refreshStar(mStarKey);

                if (buildRequest != null) {
                    mBuildRequest = buildRequest;
                    refresh();
                }
            }
        }.execute();

    }

    @Override
    public void onStop() {
    }

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        mBuildRequest = (BuildRequest) bundle.getParcelable("au.com.codeka.warworlds.BuildRequest");
        mStarKey = bundle.getString("au.com.codeka.warworlds.StarKey");
        refresh();
    }

    private void refresh() {
        Design design = DesignManager.getInstance(mBuildRequest.getBuildKind())
                                     .getDesign(mBuildRequest.getDesignID());

        ImageView imgView = (ImageView) findViewById(R.id.build_icon);
        imgView.setImageDrawable(new SpriteDrawable(design.getSprite()));

        TextView designNameView = (TextView) findViewById(R.id.build_design_name);
        if (mBuildRequest.getCount() == 1) {
            designNameView.setText(design.getDisplayName());
        } else {
            designNameView.setText(String.format("%s (× %d)",
                                   design.getDisplayName(),
                                   mBuildRequest.getCount()));
        }

        TextView buildTimeRemainingView = (TextView) findViewById(R.id.build_time_remaining);
        Duration remainingDuration = mBuildRequest.getRemainingTime();
        if (remainingDuration.equals(Duration.ZERO)) {
            buildTimeRemainingView.setText(String.format("%d %%, not enough resources to complete.",
                              (int) mBuildRequest.getPercentComplete()));
        } else {
            buildTimeRemainingView.setText(String.format("%d %%, %s left",
                              (int) mBuildRequest.getPercentComplete(),
                              TimeInHours.format(remainingDuration)));
        }

        ProgressBar buildProgressBar = (ProgressBar) findViewById(R.id.build_progress);
        buildProgressBar.setProgress((int) mBuildRequest.getPercentComplete());
    }
}
