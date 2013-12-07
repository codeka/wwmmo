package au.com.codeka.warworlds.game.build;

import org.joda.time.DateTime;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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
import au.com.codeka.common.model.Design;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.BuildEstimateView;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

public class BuildConfirmDialog extends DialogFragment {
    private Star mStar;
    private Colony mColony;
    private Design mDesign;
    private Building mExistingBuilding;
    private View mView;
    private BuildEstimateView mBuildEstimateView;

    public BuildConfirmDialog() {
    }

    public void setup(Design design, Star star, Colony colony) {
        mDesign = design;
        mStar = star;
        mColony = colony;
    }

    public void setup(Building existingBuilding, Star star, Colony colony) {
        mExistingBuilding = existingBuilding;
        mDesign = existingBuilding.getDesign();
        mStar = star;
        mColony = colony;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.build_confirm_dlg, null);

        final SeekBar countSeekBar = (SeekBar) mView.findViewById(R.id.build_count_seek);
        final EditText countEdit = (EditText) mView.findViewById(R.id.build_count_edit);
        mBuildEstimateView = (BuildEstimateView) mView.findViewById(R.id.build_estimate);
        mBuildEstimateView.setOnBuildEstimateRefreshRequired(new BuildEstimateView.BuildEstimateRefreshRequiredHandler() {
            @Override
            public void onBuildEstimateRefreshRequired() {
                refreshBuildEstimates();
            }
        });

        TextView nameTextView = (TextView) mView.findViewById(R.id.building_name);
        ImageView iconImageView = (ImageView) mView.findViewById(R.id.building_icon);
        TextView descriptionTextView = (TextView) mView.findViewById(R.id.building_description);

        nameTextView.setText(mDesign.getDisplayName());
        iconImageView.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(mDesign.getSpriteName())));
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
        int count = 1;
        if (mDesign.canBuildMultiple()) {
            final EditText countEdit = (EditText) mView.findViewById(R.id.build_count_edit);
            count = Integer.parseInt(countEdit.getText().toString());
        }

        final DateTime startTime = DateTime.now();

        BuildRequest buildRequest = new BuildRequest("FAKE_BUILD_REQUEST",
                mDesign.getDesignKind(), mDesign.getID(), mColony.getKey(),
                startTime, count,
                (mExistingBuilding == null ? null : mExistingBuilding.getKey()),
                (mExistingBuilding == null ? 0 : mExistingBuilding.getLevel()),
                null, null, mStar.getKey(), mColony.getPlanetIndex(), mColony.getEmpireKey(),
                null);

        mBuildEstimateView.refresh(mStar, buildRequest);
    }

    class RefreshResult {
        public BuildRequest buildRequest;
        public EmpirePresence empire;
    }

    private void onBuildClick() {
        final EditText countEdit = (EditText) mView.findViewById(R.id.build_count_edit);
        final Activity activity = getActivity();

        int count = 1;
        if (mDesign.canBuildMultiple()) {
            count = Integer.parseInt(countEdit.getText().toString());
        }

        BuildManager.getInstance().build(activity, mColony, mDesign, mExistingBuilding, count);

        dismiss();
    }
}
