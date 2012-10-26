package au.com.codeka.warworlds.game.solarsystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildQueueManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.Design.DesignKind;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.StarManager;

public class BuildConfirmDialog extends Dialog implements DialogManager.DialogConfigurable {
    private static Logger log = LoggerFactory.getLogger(BuildConfirmDialog.class);
    private Colony mColony;
    private Design mDesign;
    private int mCurrentQueueSize;

    public static final int ID = 1001;

    public BuildConfirmDialog(Activity activity) {
        super(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_build_confirm_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        final SeekBar countSeekBar = (SeekBar) findViewById(R.id.build_count_seek);
        final EditText countEdit = (EditText) findViewById(R.id.build_count_edit);

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

        final Button okButton = (Button) findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okButton.setEnabled(false);
                new AsyncTask<Void, Void, BuildRequest>() {
                    @Override
                    protected BuildRequest doInBackground(Void... arg0) {
                        warworlds.Warworlds.BuildRequest.BUILD_KIND kind;
                        if (mDesign.getDesignKind() == Design.DesignKind.BUILDING) {
                            kind = warworlds.Warworlds.BuildRequest.BUILD_KIND.BUILDING;
                        } else {
                            kind = warworlds.Warworlds.BuildRequest.BUILD_KIND.SHIP;
                        }

                        int count = 1;
                        if (mDesign.canBuildMultiple()) {
                            count = Integer.parseInt(countEdit.getText().toString());
                        }

                        warworlds.Warworlds.BuildRequest build = warworlds.Warworlds.BuildRequest.newBuilder()
                                .setBuildKind(kind)
                                .setColonyKey(mColony.getKey())
                                .setEmpireKey(mColony.getEmpireKey())
                                .setDesignName(mDesign.getID())
                                .setCount(count)
                                .build();
                        try {
                            build = ApiClient.postProtoBuf("buildqueue", build,
                                    warworlds.Warworlds.BuildRequest.class);

                            return BuildRequest.fromProtocolBuffer(build);
                        } catch (ApiException e) {
                            log.error("Error issuing build request", e);
                        }

                        return null;
                    }
                    @Override
                    protected void onPostExecute(BuildRequest buildRequest) {
                        // notify the BuildQueueManager that something's changed.
                        BuildQueueManager.getInstance().refresh(buildRequest);

                        // tell the StarManager that this star has been updated
                        StarManager.getInstance().refreshStar(mColony.getStarKey());
                    }
                }.execute();

                okButton.setEnabled(true);
                dismiss();
            }
        });
    }

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        String designID = bundle.getString("au.com.codeka.warworlds.DesignID");
        if (designID == null)
            designID = "";
        DesignKind dk = DesignKind.fromInt(bundle.getInt("au.com.codeka.warworlds.DesignKind",
                                           DesignKind.BUILDING.getValue()));

        mColony = (Colony) bundle.getParcelable("au.com.codeka.warworlds.Colony");
        mCurrentQueueSize = bundle.getInt("au.com.codeka.warworlds.BuildQueueSize");

        // TODO: this could be encapsulated in the DesignManager base class....
        Design design;
        if (dk == DesignKind.BUILDING) {
            design = BuildingDesignManager.getInstance().getDesign(designID);
        } else {
            design = ShipDesignManager.getInstance().getDesign(designID);
        }

        refresh(design);
    }

    private void refresh(Design design) {
        mDesign = design;

        TextView nameTextView = (TextView) findViewById(R.id.building_name);
        ImageView iconImageView = (ImageView) findViewById(R.id.building_icon);
        TextView descriptionTextView = (TextView) findViewById(R.id.building_description);

        nameTextView.setText(design.getDisplayName());
        iconImageView.setImageDrawable(new SpriteDrawable(design.getSprite()));
        descriptionTextView.setText(Html.fromHtml(design.getDescription()));

        View buildCountContainer = findViewById(R.id.build_count_container);
        if (design.canBuildMultiple()) {
            buildCountContainer.setVisibility(View.VISIBLE);
        } else {
            buildCountContainer.setVisibility(View.GONE);
        }

        refreshBuildEstimates();
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
            final EditText countEdit = (EditText) findViewById(R.id.build_count_edit);
            count = Integer.parseInt(countEdit.getText().toString());
        }

        float timeInHours = (count * mDesign.getBuildTimeSeconds()) / 3600.0f;
        timeInHours *= (100.0f / workersPerBuildRequest);
        TextView timeToBuildText = (TextView) findViewById(R.id.building_timetobuild);
        timeToBuildText.setText(TimeInHours.format(timeInHours));

        float totalMineralsCost = count * mDesign.getBuildCostMinerals();
        TextView mineralsToBuildText = (TextView) findViewById(R.id.building_mineralstobuild);
        mineralsToBuildText.setText(Html.fromHtml(
                                    String.format("%d (<font color=\"red\">%.2f</font>/hr)",
                                    (int) totalMineralsCost, totalMineralsCost / timeInHours)));
    }
}
