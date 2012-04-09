package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.TransparentWebView;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;

public class SolarSystemBuildingsConfirmDialog extends Dialog {
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
        params.horizontalMargin = 20;
        params.verticalMargin = 20;
        getWindow().setAttributes(params);
    }

    public void setBuildingDesign(BuildingDesign design) {
        TextView nameTextView = (TextView) findViewById(R.id.building_name);
        ImageView iconImageView = (ImageView) findViewById(R.id.building_icon);
        TransparentWebView descriptionWebView = (TransparentWebView) findViewById(R.id.building_description);

        nameTextView.setText(design.getName());
        Bitmap bm = BuildingDesignManager.getInstance().getDesignIcon(design);
        iconImageView.setImageBitmap(bm);

        descriptionWebView.loadHtml("html/building-description-template.html", design.getDescription());
    }
}
