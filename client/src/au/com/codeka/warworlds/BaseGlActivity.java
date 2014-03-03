package au.com.codeka.warworlds;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import org.andengine.engine.Engine;
import org.andengine.engine.LimitedFPSEngine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.ui.activity.SimpleLayoutGameActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
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
import android.view.Window;
import android.view.WindowManager;
import au.com.codeka.warworlds.ctrl.DebugView;
import au.com.codeka.warworlds.model.PurchaseManager;

/**
 * This is a base class for our activities which inherit from andengine's BaseActivity. We have to
 * duplicate a few things that come from our own BaseActivity.
 */
@SuppressLint("Registered") // it's a base class
public abstract class BaseGlActivity extends SimpleLayoutGameActivity {
    private static Logger log = LoggerFactory.getLogger(BaseGlActivity.class);
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private DebugView mDebugView;
    private WindowManager.LayoutParams mDebugViewLayout;
    private SensorEventListener mBugReportShakeListener = new BugReportSensorListener(this);
    private long mForegroundStartTimeMs;

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

    public int getWidth() {
        return mCameraWidth;
    }
    public int getHeight() {
        return mCameraHeight;
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        Point size = new Point();
        getRealDisplaySize(size);
        mCameraWidth = size.x;
        mCameraHeight = size.y;

        mCamera = createCamera();
        EngineOptions options =  new EngineOptions(false, ScreenOrientation.MANIFEST,
                new RatioResolutionPolicy(mCameraWidth, mCameraHeight), mCamera);
        options.setUpdateThreadPriority(Thread.NORM_PRIORITY - 2);

        return options;
    }

    @Override
    public Engine onCreateEngine(final EngineOptions engineOptions) {
        Engine engine = null;
        if (getNumCores() == 1) {
            log.info("Single-core device detected, using a Limited-FPS engine.");
            engine = new LimitedFPSEngine(engineOptions, 5);
        } else {
            log.info("Multi-core device detected, using regular engine.");
            engine = new Engine(engineOptions);
        }

        return engine;
    }

    /** Create the camera, we create a ZoomCamera by default. */
    protected Camera createCamera() {
        ZoomCamera camera = new ZoomCamera(0, 0, mCameraWidth, mCameraHeight);

        final float zoomFactor = getResources().getDisplayMetrics().density;
        camera.setZoomFactor(zoomFactor);

        return camera;
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

        BackgroundDetector.i.onActivityPause(this, System.currentTimeMillis() - mForegroundStartTimeMs);
        super.onPause();
    }

    @Override
    public void onResume() {
        Util.loadProperties();

        mForegroundStartTimeMs = System.currentTimeMillis();
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
    @SuppressWarnings("deprecation") // need to support older devices as well
    protected boolean isPortrait() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        return height > width;
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result.
     */
    private static int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }      
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch(Exception e) {
            //Default to return 1 core
            return 1;
        }
    }
}
