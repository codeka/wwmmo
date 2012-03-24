package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import au.com.codeka.warworlds.R;

public class ChatActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
    }

    @Override
    public void onResume() {
        super.onResume();

        setContentView(R.layout.chat);
    }

}
