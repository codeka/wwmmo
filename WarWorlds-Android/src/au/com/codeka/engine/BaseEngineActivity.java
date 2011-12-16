package au.com.codeka.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

/**
 * You would create an \c Activity that is a sub-class of this to get all of the
 * OpenGL goodness.
 * 
 * @author dean@codeka.com.au
 */
public class BaseEngineActivity extends Activity {

	private RenderSurfaceView mView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // by default, we hide the window title, but leave the notification
        // area visible.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mView = new RenderSurfaceView(this);
        mView.createRenderer();
        setContentView(mView);
    }
}
