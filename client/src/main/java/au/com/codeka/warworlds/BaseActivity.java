package au.com.codeka.warworlds;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.ctrl.DebugView;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.PurchaseManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

@SuppressLint("Registered") // it's a base class
public class BaseActivity extends AppCompatActivity {
  private static final Log log = new Log("BaseActivity");
  private SensorManager sensorManager;
  private Sensor accelerometer;
  private DebugView debugView;
  private WindowManager.LayoutParams debugViewLayout;
  private SensorEventListener bugReportShakeListener = new BugReportSensorListener(this);

  private long foregroundStartTimeMs;

  protected boolean isResumed;

  public static final int AUTH_RECOVERY_REQUEST = 2397;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!wantsActionBar()) {
      // If we don't want the action bar, then hide it.
      getSupportActionBar().hide();
    }

    // register our bug report shake listener with the accelerometer
    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    Util.loadProperties();
    if (Util.isDebug()) {
      debugView = new DebugView(this);
      debugViewLayout = new WindowManager.LayoutParams(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.TYPE_APPLICATION,
          WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
          PixelFormat.TRANSLUCENT);
      debugViewLayout.gravity = Gravity.TOP;
    }
  }

  /**
   * If you want the action bar in your activity, override this and return true.
   */
  protected boolean wantsActionBar() {
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
    }
  }

  @Override
  public void onPause() {
    sensorManager.unregisterListener(bugReportShakeListener, accelerometer);

    if (debugView != null) {
      getWindowManager().removeView(debugView);
    }

    BackgroundDetector.i.onActivityPause(System.currentTimeMillis() - foregroundStartTimeMs);
    isResumed = false;
    super.onPause();
  }

  @Override
  public void onResumeFragments() {
    Util.loadProperties();

    int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (result != ConnectionResult.SUCCESS) {
      Dialog dialog = GooglePlayServicesUtil.getErrorDialog(result, this, 0);
      dialog.show();
      finish();
    }

    foregroundStartTimeMs = System.currentTimeMillis();
    sensorManager.registerListener(bugReportShakeListener, accelerometer,
        SensorManager.SENSOR_DELAY_UI);

    if (debugView != null) {
      getWindowManager().addView(debugView, debugViewLayout);
    }

    BackgroundDetector.i.onActivityResume(this);
    isResumed = true;
    super.onResumeFragments();
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

  /**
   * Helper function to determine whether we're in portrait orientation or not.
   */
  @SuppressWarnings("deprecation") // need to support older devices as well
  protected boolean isPortrait() {
    Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();
    return height > width;
  }
}
