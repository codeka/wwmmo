package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.shared.StarfieldStar;

/**
 * The \c StarfieldActivity is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldActivity extends Activity {

    StarfieldSurfaceView mStarfield;
    TextView mUsername;
    TextView mMoney;
    TextView mStarName;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
    }

    @Override
    public void onResume() {
        super.onResume();

        setContentView(R.layout.starfield);

        mStarfield = (StarfieldSurfaceView) findViewById(R.id.starfield);
        mUsername = (TextView) findViewById(R.id.username);
        mMoney = (TextView) findViewById(R.id.money);
        mStarName = (TextView) findViewById(R.id.star_name);

        mUsername.setText("codeka");
        mMoney.setText("$ 12,345");
        mStarName.setText("");

        mStarfield.addStarSelectedListener(new StarfieldSurfaceView.OnStarSelectedListener() {
            @Override
            public void onStarSelected(StarfieldStar star) {
                mStarName.setText(star.getName());
                // todo: load the rest of the star's details...
            }
        });
    }
}
