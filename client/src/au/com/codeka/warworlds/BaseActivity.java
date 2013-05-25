package au.com.codeka.warworlds;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import au.com.codeka.common.Vector3;
import au.com.codeka.warworlds.ctrl.DebugView;
import au.com.codeka.warworlds.model.PurchaseManager;

@SuppressLint("Registered") // it's a base class
public class BaseActivity extends FragmentActivity {
    private static Logger log = LoggerFactory.getLogger(BaseActivity.class);
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private DebugView mDebugView;
    private WindowManager.LayoutParams mDebugViewLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register our bug report shake listener with the accelerometer
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Util.loadProperties(this);
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

        BackgroundDetector.getInstance().onActivityPause(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        Util.loadProperties(this);

        mSensorManager.registerListener(mBugReportShakeListener, mAccelerometer,
                                        SensorManager.SENSOR_DELAY_UI);

        if (mDebugView != null) {
            getWindowManager().addView(mDebugView, mDebugViewLayout);
        }

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

    /**
     * Helper function to determine whether we're in portrait orientation or not.
     */
    protected boolean isPortrait() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            return true;
        }
        return false;
    }

    private void triggerBugPopup() {
        View rootView = getWindow().getDecorView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bmp = rootView.getDrawingCache();
        String path = Images.Media.insertImage(getContentResolver(), bmp, "screenshot", null);
        Uri uri = Uri.parse(path);
        rootView.setDrawingCacheEnabled(false);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"dean@war-worlds.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug report, screenshot attached.");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(Intent.createChooser(intent, "Send bug report"));
    }

    /**
     * This implementation of SensorEventListener will call \c triggerBugReport after a certain
     * number of shakes of the phone.
     */
    private SensorEventListener mBugReportShakeListener = new SensorEventListener() {
        private Vector3 mAcceleration;
        private Vector3 mPreviousAcceleration;
        private DateTime mLastShakeTime;
        private DateTime mLastTriggerTime;
        private int mNumShakes;
        private boolean mIsFirstResult = true;

        // how much difference in acceleration constities one "shake"
        private static final double sShakeThreshold = 20.0;
        // how many seconds needs to elapse before we consider two consective shakes "separate"
        private static final double sShakeTime = 1.0;
        // home many consective shakes are required to trigger the "report bug" functionality
        private static final int sNumShakesBeforeBugPopup = 16;
        // how many seconds before we'll consider another trigger, should be a fairly long amount of time
        private static final double sMaxTriggerGap = 10.0;

        /**
         * This is called when a sensor detects some change.
         */
        @Override
        public void onSensorChanged(SensorEvent se) {
            Vector3 accel = new Vector3(se.values[0], se.values[1], se.values[2]);
            if (mIsFirstResult) {
                mPreviousAcceleration = accel;
                mIsFirstResult = false;
            } else {
                mPreviousAcceleration = mAcceleration;
            }
            mAcceleration = accel;

            double distance = Vector3.distanceBetween(mAcceleration, mPreviousAcceleration);

            if (distance > sShakeThreshold) {
                DateTime now = DateTime.now(DateTimeZone.UTC);
                if (mLastShakeTime == null) {
                    mNumShakes = 1;
                } else {
                    long millis = now.getMillis() - mLastShakeTime.getMillis();
                    if (millis / 1000.0 < sShakeTime) {
                        mNumShakes ++;
                        if (mNumShakes > sNumShakesBeforeBugPopup) {
                            if (mLastTriggerTime == null ||
                                ((now.getMillis() - mLastTriggerTime.getMillis()) / 1000.0 > sMaxTriggerGap)) {
                                triggerBugPopup();
                                mLastTriggerTime = now;
                            }
                        }
                    } else {
                        // we'll reset the number of shakes back to 1
                        mNumShakes = 1;
                    }
                }
                mLastShakeTime = now;

                log.debug(String.format("shake distance: %.2f num shakes: %d",
                        distance, mNumShakes));
            }
        }

        /**
         * This is called when the accuracy of a sensor changes (e.g. when you go from Wi-Fi to
         * GPS-based location, etc). We don't care for our use.
         */
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

}
