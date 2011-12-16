package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import au.com.codeka.warworlds.R;

/**
 * The \c StarfieldActivity is the "home" screen of the game, and displays the
 * starfield where you scroll around and interact with stars, etc.
 */
public class StarfieldActivity extends Activity {

    TextView mUsername;
    TextView mMoney;

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

        mUsername = (TextView) findViewById(R.id.username);
        mMoney = (TextView) findViewById(R.id.money);

        mUsername.setText("codeka");
        mMoney.setText("$ 12,345");
    }
}
