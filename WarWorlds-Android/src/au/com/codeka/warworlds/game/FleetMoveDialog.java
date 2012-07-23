package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.starfield.StarfieldSurfaceView;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

public class FleetMoveDialog extends Dialog {
    private Fleet mFleet;
    private Star mSourceStar;
    private UniverseElementActivity mActivity;
    private StarfieldSurfaceView mStarfield;

    public static final int ID = 1008;

    public FleetMoveDialog(UniverseElementActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fleet_move_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        mStarfield = (StarfieldSurfaceView) findViewById(R.id.starfield);
        mStarfield.setZOrderOnTop(true);
    }

    public void setFleet(Fleet fleet, Star sourceStar) {
        mSourceStar = sourceStar;

        setFleet(fleet, mSourceStar.getSectorX(),
                        mSourceStar.getSectorY(),
                        mSourceStar.getOffsetX(),
                        mSourceStar.getOffsetY(),
                        mSourceStar.getKey());
    }

    public void setFleet(Fleet fleet, long sectorX, long sectorY, int offsetX, int offsetY,
                          String starKey) {
        mFleet = fleet;

        offsetX = offsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
        offsetY = offsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());
        SectorManager.getInstance().scrollTo(sectorX, sectorY, offsetX, offsetY);
    }
}
