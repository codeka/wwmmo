package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.BuildRequest;
import warworlds.Warworlds.BuildRequest.BUILD_KIND;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;

public class SolarSystemBuildingsConfirmDialog extends Dialog {
    private static Logger log = LoggerFactory.getLogger(WarWorldsActivity.class);
    private Colony mColony;
    private BuildingDesign mDesign;

    public SolarSystemBuildingsConfirmDialog(SolarSystemActivity activity) {
        super(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_buildings_build);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        final Button okButton = (Button) findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okButton.setEnabled(false);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... arg0) {
                        BuildRequest build = BuildRequest.newBuilder()
                                .setBuildKind(BUILD_KIND.BUILDING)
                                .setColonyKey(mColony.getKey())
                                .setDesignName(mDesign.getID())
                                .build();
                        try {
                            build = ApiClient.postProtoBuf("buildqueue", build, warworlds.Warworlds.BuildRequest.class);
                            // todo: do something??
                        } catch (ApiException e) {
                            log.error("Error issuing build request", e);
                        }

                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        okButton.setEnabled(true);
                        dismiss();
                    }
                }.execute();
            }
        });
    }

    public void setColony(Colony colony) {
        mColony = colony;
    }

    public void setBuildingDesign(BuildingDesign design) {
        mDesign = design;

        TextView nameTextView = (TextView) findViewById(R.id.building_name);
        ImageView iconImageView = (ImageView) findViewById(R.id.building_icon);
        TransparentWebView descriptionWebView = (TransparentWebView) findViewById(R.id.building_description);

        nameTextView.setText(design.getName());
        Bitmap bm = BuildingDesignManager.getInstance().getDesignIcon(design);
        iconImageView.setImageBitmap(bm);

        descriptionWebView.loadHtml("html/building-description-template.html", design.getDescription());
    }
}
