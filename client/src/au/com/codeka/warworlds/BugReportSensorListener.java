package au.com.codeka.warworlds;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.view.View;
import au.com.codeka.common.Vector3;

public class BugReportSensorListener implements SensorEventListener {
    private static Logger log = LoggerFactory.getLogger(BugReportSensorListener.class);

    private Vector3 mAcceleration;
    private Vector3 mPreviousAcceleration;
    private DateTime mLastShakeTime;
    private DateTime mLastTriggerTime;
    private int mNumShakes;
    private boolean mIsFirstResult = true;
    private Activity mActivity;

    // how much difference in acceleration constities one "shake"
    private static final double sShakeThreshold = 20.0;
    // how many seconds needs to elapse before we consider two consective shakes "separate"
    private static final double sShakeTime = 1.0;
    // home many consective shakes are required to trigger the "report bug" functionality
    private static final int sNumShakesBeforeBugPopup = 16;
    // how many seconds before we'll consider another trigger, should be a fairly long amount of time
    private static final double sMaxTriggerGap = 10.0;

    public BugReportSensorListener(Activity activity) {
        mActivity = activity;
    }

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
                            try {
                                triggerBugPopup();
                            } catch (Exception e) {
                            }
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

    private void triggerBugPopup() {
        View rootView = mActivity.getWindow().getDecorView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bmp = rootView.getDrawingCache();
        String path = Images.Media.insertImage(mActivity.getContentResolver(), bmp, "screenshot", null);
        Uri uri = Uri.parse(path);
        rootView.setDrawingCacheEnabled(false);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"dean@war-worlds.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug report, screenshot attached.");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mActivity.startActivity(Intent.createChooser(intent, "Send bug report"));
    }
}
