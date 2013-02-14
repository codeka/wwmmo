package au.com.codeka.warworlds;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import au.com.codeka.warworlds.model.PurchaseManager;

@SuppressLint("Registered") // it's a base class
public class BaseActivity extends FragmentActivity {

    @Override
    public void onPause() {
        BackgroundDetector.getInstance().onActivityPause(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        // if we're resuming after a long break, we could have been GCed in
        // which case need to set things up again...
        Util.loadProperties(this);
        Util.setup(this);

        BackgroundDetector.getInstance().onActivityResume(this);
        super.onResume();
    }

    @Override
    public void startActivity(Intent intent) {
        BackgroundDetector.getInstance().onStartActivity(this, intent);
        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        BackgroundDetector.getInstance().onStartActivity(this, intent);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onPostResume() {
        BackgroundDetector.getInstance().onActivityPostResume(this);
        super.onPostResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        PurchaseManager.getInstance().onActivityResult(requestCode, resultCode, intent);
    }
}
