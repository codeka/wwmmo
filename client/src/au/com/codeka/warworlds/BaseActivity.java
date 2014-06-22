package au.com.codeka.warworlds;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import au.com.codeka.warworlds.ctrl.DebugView;
import au.com.codeka.warworlds.model.PurchaseManager;

@SuppressLint("Registered") // it's a base class
public class BaseActivity extends FragmentActivity {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private DebugView mDebugView;
    private WindowManager.LayoutParams mDebugViewLayout;
    private SensorEventListener mBugReportShakeListener = new BugReportSensorListener(this);

    private long mForegroundStartTimeMs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register our bug report shake listener with the accelerometer
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Util.loadProperties();
        if (Util.isDebug()) {
            mDebugView = new DebugView(this);
            mDebugViewLayout = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            mDebugViewLayout.gravity = Gravity.TOP | Gravity.RIGHT;
        }
    }

    @Override
    public void onPause() {
        mSensorManager.unregisterListener(mBugReportShakeListener, mAccelerometer);

        if (mDebugView != null) {
            getWindowManager().removeView(mDebugView);
        }

        BackgroundDetector.i.onActivityPause(System.currentTimeMillis() - mForegroundStartTimeMs);
        super.onPause();
    }

    @Override
    public void onResume() {
        Util.loadProperties();

        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(result, this, 0);
            dialog.show();
            finish();
        }

        mForegroundStartTimeMs = System.currentTimeMillis();
        mSensorManager.registerListener(mBugReportShakeListener, mAccelerometer,
                                        SensorManager.SENSOR_DELAY_UI);

        if (mDebugView != null) {
            getWindowManager().addView(mDebugView, mDebugViewLayout);
        }

        BackgroundDetector.i.onActivityResume();
        super.onResume();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            MemoryTrimmer.trimMemory();
        }
    }

    @Override
    public void startActivity(Intent intent) {
        BackgroundDetector.i.onStartActivity(this, intent);
        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        BackgroundDetector.i.onStartActivity(this, intent);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onPostResume() {
        BackgroundDetector.i.onActivityPostResume(this);
        super.onPostResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        PurchaseManager.i.onActivityResult(requestCode, resultCode, intent);
    }

    /** Helper function to determine whether we're in portrait orientation or not. */
    @SuppressWarnings("deprecation") // need to support older devices as well
    protected boolean isPortrait() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        return height > width;
    }
}
