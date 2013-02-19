package au.com.codeka.warworlds.game.solarsystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class BuildConfirmDialog extends DialogFragment {
    private static Logger log = LoggerFactory.getLogger(BuildConfirmDialog.class);
    private Colony mColony;
    private Design mDesign;
    private Building mExistingBuilding;
    private int mCurrentQueueSize;
    private View mView;

    public BuildConfirmDialog() {
    }

    public void setup(Design design, Colony colony, int buildQueueSize) {
        mDesign = design;
        mColony = colony;
        mCurrentQueueSize = buildQueueSize;
    }

    public void setup(Building existingBuilding, Colony colony, int buildQueueSize) {
        mExistingBuilding = existingBuilding;
        mDesign = existingBuilding.getDesign();
        mColony = colony;
        mCurrentQueueSize = buildQueueSize;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.build_confirm_dlg, null);

        final SeekBar countSeekBar = (SeekBar) mView.findViewById(R.id.build_count_seek);
        final EditText countEdit = (EditText) mView.findViewById(R.id.build_count_edit);

        TextView nameTextView = (TextView) mView.findViewById(R.id.building_name);
        ImageView iconImageView = (ImageView) mView.findViewById(R.id.building_icon);
        TextView descriptionTextView = (TextView) mView.findViewById(R.id.building_description);

        nameTextView.setText(mDesign.getDisplayName());
        iconImageView.setImageDrawable(new SpriteDrawable(mDesign.getSprite()));
        descriptionTextView.setText(Html.fromHtml(mDesign.getDescription()));

        View upgradeContainer = mView.findViewById(R.id.upgrade_container);
        View buildCountContainer = mView.findViewById(R.id.build_count_container);
        if (mDesign.canBuildMultiple() && mExistingBuilding == null) {
            buildCountContainer.setVisibility(View.VISIBLE);
            upgradeContainer.setVisibility(View.GONE);
        } else {
            buildCountContainer.setVisibility(View.GONE);
            if (mExistingBuilding != null) {
                upgradeContainer.setVisibility(View.VISIBLE);

                TextView timeToBuildLabel = (TextView) mView.findViewById(R.id.building_timetobuild_label);
                timeToBuildLabel.setText("Time to upgrade:");

                TextView currentLevel = (TextView) mView.findViewById(R.id.upgrade_current_level);
                currentLevel.setText(Integer.toString(mExistingBuilding.getLevel()));
            }
        }

        countEdit.setText("1");
        countSeekBar.setMax(99);
        countSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    countEdit.setText(Integer.toString(progress + 1));
                    refreshBuildEstimates();
                }
            }
        });

        countEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 0) {
                    return;
                }
                int count = 1;
                try {
                    count = Integer.parseInt(s.toString());
                } catch (Exception e) {
                    // ignore errors here
                }
                if (count <= 0) {
                    count = 1;
                    countEdit.setText("1");
                }
                if (count <= 100) {
                    countSeekBar.setProgress(count - 1);
                } else {
                    countSeekBar.setProgress(99);
                }

                refreshBuildEstimates();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
        });

        refreshBuildEstimates();

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        if (mExistingBuilding == null) {
            b.setTitle("Build");
        } else {
            b.setTitle("Upgrade");
        }
        b.setView(mView);

        String label = (mExistingBuilding == null ? "Build" : "Upgrade");
        b.setPositiveButton(label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onBuildClick();
            }
        });

        b.setNegativeButton("Cancel", null);

        return b.create();
    }

    private void refreshBuildEstimates() {
        // estimate the build time, based on current queue size, construction focus, etc
        float totalWorkers = mColony.getPopulation() * mColony.getConstructionFocus();
        float workersPerBuildRequest = totalWorkers / (mCurrentQueueSize + 1);
        if (workersPerBuildRequest < 1) {
            workersPerBuildRequest = 1;
        }

        int count = 1;
        if (mDesign.canBuildMultiple()) {
            final EditText countEdit = (EditText) mView.findViewById(R.id.build_count_edit);
            count = Integer.parseInt(countEdit.getText().toString());
        }

        float timeInHours = (count * mDesign.getBuildCost().getTimeInSeconds()) / 3600.0f;
        timeInHours *= (100.0f / workersPerBuildRequest);
        TextView timeToBuildText = (TextView) mView.findViewById(R.id.building_timetobuild);
        timeToBuildText.setText(TimeInHours.format(timeInHours));

        float totalMineralsCost = count * mDesign.getBuildCost().getCostInMinerals();
        TextView mineralsToBuildText = (TextView) mView.findViewById(R.id.building_mineralstobuild);
        mineralsToBuildText.setText(Html.fromHtml(
                                    String.format("%d (<font color=\"red\">%.2f</font>/hr)",
                                    (int) totalMineralsCost, totalMineralsCost / timeInHours)));
    }

    private void onBuildClick() {
        final EditText countEdit = (EditText) mView.findViewById(R.id.build_count_edit);
        final Activity activity = getActivity();

        dismiss();

        new AsyncTask<Void, Void, BuildRequest>() {
            private int mErrorCode;
            private String mErrorMsg;

            @Override
            protected BuildRequest doInBackground(Void... arg0) {
                Messages.BuildRequest.BUILD_KIND kind;
                if (mDesign.getDesignKind() == Design.DesignKind.BUILDING) {
                    kind = Messages.BuildRequest.BUILD_KIND.BUILDING;
                } else {
                    kind = Messages.BuildRequest.BUILD_KIND.SHIP;
                }

                int count = 1;
                if (mDesign.canBuildMultiple()) {
                    count = Integer.parseInt(countEdit.getText().toString());
                }

                Messages.BuildRequest build = Messages.BuildRequest.newBuilder()
                        .setBuildKind(kind)
                        .setColonyKey(mColony.getKey())
                        .setEmpireKey(mColony.getEmpireKey())
                        .setDesignName(mDesign.getID())
                        .setCount(count)
                        .setExistingBuildingKey(mExistingBuilding == null ? "" : mExistingBuilding.getKey())
                        .build();
                try {
                    build = ApiClient.postProtoBuf("buildqueue", build, Messages.BuildRequest.class);

                    return BuildRequest.fromProtocolBuffer(build);
                } catch (ApiException e) {
                    log.error("Error issuing build request", e);
                    if (e.getServerErrorCode() > 0) {
                        mErrorCode = e.getServerErrorCode();
                        mErrorMsg = e.getServerErrorMessage();
                    }
                }

                return null;
            }
            @Override
            protected void onPostExecute(BuildRequest buildRequest) {
                if (mErrorCode > 0) {
                    new StyledDialog.Builder(activity)
                                    .setTitle("Cannot Build")
                                    .setMessage(mErrorMsg)
                                    .setPositiveButton("Close", true, null)
                                    .create().show();
                } else {
                    EmpireManager.getInstance().refreshEmpire();
                    StarManager.getInstance().refreshStar(activity, mColony.getStarKey());
                }
            }
        }.execute();
    }
}
