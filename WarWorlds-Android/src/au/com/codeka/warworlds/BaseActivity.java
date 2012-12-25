package au.com.codeka.warworlds;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

public class BaseActivity extends FragmentActivity {

    @Override
    public void onPause() {
        BackgroundDetector.getInstance().onActivityPause(this);
        super.onPause();
    }

    @Override
    public void onResume() {
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
}
