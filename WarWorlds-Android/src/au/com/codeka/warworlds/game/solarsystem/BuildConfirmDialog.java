package au.com.codeka.warworlds.game.solarsystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.BuildQueueManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Design.DesignKind;
import au.com.codeka.warworlds.model.StarManager;

public class BuildConfirmDialog extends Dialog implements DialogManager.DialogConfigurable {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Colony mColony;
    private Design mDesign;

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

                        warworlds.Warworlds.BuildRequest build = warworlds.Warworlds.BuildRequest.newBuilder()
                                .setBuildKind(kind)
                                .setColonyKey(mColony.getKey())
                                .setEmpireKey(mColony.getEmpireKey())
                                .setDesignName(mDesign.getID())
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

                        okButton.setEnabled(true);
                        dismiss();
                    }
                }.execute();
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

        nameTextView.setText(design.getName());
        Bitmap bm;
        if (design.getDesignKind() == Design.DesignKind.BUILDING) {
            bm = BuildingDesignManager.getInstance().getDesignIcon(design);
        } else {
            bm = ShipDesignManager.getInstance().getDesignIcon(design);
        }
        iconImageView.setImageBitmap(bm);

        descriptionTextView.setText(Html.fromHtml(design.getDescription()));
    }
}
