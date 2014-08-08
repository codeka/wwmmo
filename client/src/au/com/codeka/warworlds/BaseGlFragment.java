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
import org.andengine.ui.fragment.SimpleLayoutGameFragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import au.com.codeka.common.Log;

/**
 * This is a base class for our fragments which inherit from andengine's
 * BaseFragment. We have to duplicate a few things that come from our own
 * BaseActivity and BaseGlActivity.
 */
@SuppressLint("Registered") // it's a base class
public abstract class BaseGlFragment extends SimpleLayoutGameFragment {
    private static final Log log = new Log("BaseGlFragment");

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

    /** Helper function to determine whether we're in portrait orientation or not. */
    @SuppressWarnings("deprecation") // need to support older devices as well
    protected boolean isPortrait() {
        Display display = ((WindowManager) getActivity().getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
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
