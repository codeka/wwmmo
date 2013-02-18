package au.com.codeka.warworlds.game.starfield;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Star;

public class TacticalMapActivity extends BaseActivity {
    private static final Logger log = LoggerFactory.getLogger(TacticalMapActivity.class);
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
            log.debug("Scrolling to: "+sectorX+","+sectorY+" : "+offsetX+","+offsetY);
            mTacticalMapView.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
        }

        mTacticalMapView.setDoubleTapHandler(new TacticalMapView.DoubleTapHandler() {
            @Override
            public void onDoubleTapped(Star star) {
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
}
