package au.com.codeka.warworlds;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.ui.activity.SimpleLayoutGameActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import au.com.codeka.warworlds.ctrl.DebugView;
import au.com.codeka.warworlds.model.PurchaseManager;

/**
 * This is a base class for our activities which inherit from andengine's BaseActivity. We have to
 * duplicate a few things that come from our own BaseActivity.
 */
public abstract class BaseGlActivity extends SimpleLayoutGameActivity {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private DebugView mDebugView;
    private WindowManager.LayoutParams mDebugViewLayout;
    private SensorEventListener mBugReportShakeListener = new BugReportSensorListener(this);

    protected int mCameraWidth;
    protected int mCameraHeight;

    protected Camera mCamera;

    private void getRealDisplaySize(Point pt) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        pt.x = dm.widthPixels;
        pt.y = dm.heightPixels;

        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            pt.y -= getResources().getDimensionPixelSize(resId);
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        Point size = new Point();
        getRealDisplaySize(size);
        mCameraWidth = size.x;
        mCameraHeight = size.y;

        mCamera = createCamera();
        return new EngineOptions(false, ScreenOrientation.PORTRAIT_SENSOR,
                new RatioResolutionPolicy(mCameraWidth, mCameraHeight), mCamera);
    }

    /** Create the camera, we create a ZoomCamera by default. */
    protected Camera createCamera() {
        return new ZoomCamera(0, 0, mCameraWidth, mCameraHeight);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
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

        BackgroundDetector.i.onActivityPause(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        Util.loadProperties();

        mSensorManager.registerListener(mBugReportShakeListener, mAccelerometer,
                                        SensorManager.SENSOR_DELAY_UI);

        if (mDebugView != null) {
            getWindowManager().addView(mDebugView, mDebugViewLayout);
        }

        BackgroundDetector.i.onActivityResume(this);
        super.onResume();
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
    protected boolean isPortrait() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            return true;
        }
        return false;
    }}
