package au.com.codeka.warworlds.game.starfield;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.view.Window;
import au.com.codeka.warworlds.BaseActivity;

public class TacticalMapActivity extends BaseActivity {
    private static final Logger log = LoggerFactory.getLogger(TacticalMapActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
    }
}
