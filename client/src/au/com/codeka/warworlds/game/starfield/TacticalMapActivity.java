package au.com.codeka.warworlds.game.starfield;

import org.andengine.entity.scene.Scene;

import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.R;

public class TacticalMapActivity extends BaseGlActivity {
    private TacticalMapSceneManager mSceneManager;
/*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.tactical_map);

        mTacticalMapView = (TacticalMapView) findViewById(R.id.tactical_map);

        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            long sectorX = extra.getLong("au.com.codeka.warworlds.SectorX", 0);
            long sectorY = extra.getLong("au.com.codeka.warworlds.SectorY", 0);
            int offsetX = extra.getInt("au.com.codeka.warworlds.OffsetX", 0);
            int offsetY = extra.getInt("au.com.codeka.warworlds.OffsetY", 0);

            mTacticalMapView.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
        }

        mTacticalMapView.setDoubleTapHandler(new TacticalMapView.DoubleTapHandler() {
            @Override
            public void onDoubleTapped(BaseStar star) {
                Intent intent = new Intent();
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                intent.putExtra("au.com.codeka.warworlds.OffsetX", star.getOffsetX());
                intent.putExtra("au.com.codeka.warworlds.OffsetY", star.getOffsetY());
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());

                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
*/

    public TacticalMapActivity() {
        mSceneManager = new TacticalMapSceneManager(this);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.tactical_map;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.tactical_map;
    }

    @Override
    public void onCreateResources() {
        mSceneManager.onLoadResources();
    }

    @Override
    public void onStart() {
        super.onStart();
        mSceneManager.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSceneManager.onStop();
    }

    @Override
    protected Scene onCreateScene() {
        return mSceneManager.createScene();
    }
}
