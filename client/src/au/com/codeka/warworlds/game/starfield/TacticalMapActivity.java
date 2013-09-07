package au.com.codeka.warworlds.game.starfield;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;

public class TacticalMapActivity extends BaseActivity {
    private TacticalMapView mTacticalMapView;

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
            public void onDoubleTapped(Star star) {
                Intent intent = new Intent();
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.sector_x);
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.sector_y);
                intent.putExtra("au.com.codeka.warworlds.OffsetX", star.offset_x);
                intent.putExtra("au.com.codeka.warworlds.OffsetY", star.offset_y);
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.key);

                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
