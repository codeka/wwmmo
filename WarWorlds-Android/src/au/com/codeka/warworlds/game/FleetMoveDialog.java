package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.starfield.StarfieldSurfaceView;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;

public class FleetMoveDialog extends Dialog {
    private Activity mActivity;
    private Fleet mFleet;
    private Star mSourceStar;
    private StarfieldSurfaceView mStarfield;

    public static final int ID = 1008;

    public FleetMoveDialog(Activity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fleet_move_dlg);

        // we want the window to be slightly taller than a square
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        int displayWidth = display.getWidth();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = displayWidth;
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
        mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY);
    }
}
