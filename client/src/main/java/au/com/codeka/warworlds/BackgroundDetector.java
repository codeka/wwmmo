package au.com.codeka.warworlds;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.eventbus.EventBus;

/**
 * This class is used to detect when the app goes into the background.
 *
 * The algorithm used to detect when we go to the background is described in this Stack Overflow
 * article: http://stackoverflow.com/questions/4414171/ Basically:
 * <p/>
 * * in onResume, increment the number of running activities
 * * in onPause, decrement the number of running activities
 * * If the number of running activities hits zero, you're going into the background, UNLESS a new
 * activity from the same package is being started.
 * <p/>
 * To detect when a new activity from the same package is being started, we need to override
 * startActivity() and save the package of the starting activity. In onPostResume(), we clear it
 * again. If there's a saved package name when the activity count hits zero, then we're not going
 * into the background.
 */
public class BackgroundDetector {
  public static final BackgroundDetector i = new BackgroundDetector();
  public static final EventBus eventBus = new EventBus();

  private static final Log log = new Log("BackgroundDetector");

  private int numActiveActivities;
  private String startingActivityPackage;
  private boolean isInBackground;
  private boolean isTransitioningToBackground;
  private Handler handler;
  private String lastActivityName;
  private long startTimeMs;
  private long totalForegroundTimeMs;
  private boolean needBackStackReset;

  @Nullable
  private Activity currentVisibleActivity;

  private BackgroundDetector() {
    isInBackground = true;
    isTransitioningToBackground = false;
    handler = new Handler();
  }

  /** Gets the name (class name) of the last activity that was running. */
  public String getLastActivityName() {
    return lastActivityName;
  }

  public boolean isInBackground() {
    return isInBackground;
  }

  public long getTotalRunTime() {
    return SystemClock.elapsedRealtime() - startTimeMs;
  }

  public long getTotalForegroundTime() {
    return totalForegroundTimeMs;
  }

  public void onBackgroundStatusChange() {
    if (!Util.isSetup() || RealmContext.i.getCurrentRealm() == null) {
      return;
    }

    eventBus.publish(new BackgroundChangeEvent(isInBackground));
  }

  /**
   * Called when something happens that means we need to reset the game back to the welcome screen
   * (e.g. blitz reset). If we're currently foregrounded, we'll just do it immediately. If we're
   * in the background, then we'll delay until we're back in the foreground again.
   */
  public void resetBackStack() {
    if (currentVisibleActivity != null) {
      if (!(currentVisibleActivity instanceof WarWorldsActivity)) {
        currentVisibleActivity.startActivity(
            new Intent(currentVisibleActivity, WarWorldsActivity.class));
        currentVisibleActivity.finish();
      }
      needBackStackReset = false;
    } else {
      needBackStackReset = true;
    }
  }

  private void transitionToBackground() {
    if (isTransitioningToBackground) {
      return;
    }

    // If one of our activities doesn't resume in the next 5 seconds, we'll consider ourselves
    // in the background.
    isTransitioningToBackground = true;
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (isTransitioningToBackground) {
          isTransitioningToBackground = false;
          isInBackground = true;
          onBackgroundStatusChange();
        }
      }
    }, 5000);
  }

  private void transitionToForeground() {
    if (isTransitioningToBackground) {
      isTransitioningToBackground = false;
      return;
    }

    isInBackground = false;
    onBackgroundStatusChange();

    if (needBackStackReset) {
      resetBackStack();
    }
  }

  public void onActivityPause(long activityRunTimeMs) {
    totalForegroundTimeMs += activityRunTimeMs;

    numActiveActivities--;
    if (numActiveActivities <= 0) {
      if (startingActivityPackage != null
          && startingActivityPackage.startsWith("au.com.codeka.warworlds")) {
        // it's our activity that we're pausing for, don't transition to background.
      } else {
        currentVisibleActivity = null;
        transitionToBackground();
      }
    }
  }

  public void onActivityResume(Activity activity) {
    if (startTimeMs == 0) {
      startTimeMs = SystemClock.elapsedRealtime();
    }

    currentVisibleActivity = activity;
    numActiveActivities++;
    if (numActiveActivities == 1) {
      if (startingActivityPackage != null
          && startingActivityPackage.startsWith("au.com.codeka.warworlds")) {
        // it's our activity that we're resuming for, don't transition FROM background.
      } else {
        transitionToForeground();
      }
    }
  }

  public void onStartActivity(Activity callingActivity, Intent intent) {
    startingActivityPackage = null;

    ComponentName componentName = intent.getComponent();
    if (componentName != null) {
      startingActivityPackage = componentName.getPackageName();
    }

    lastActivityName = callingActivity.getLocalClassName();
  }

  public void onActivityPostResume(Activity activity) {
    startingActivityPackage = null;
    lastActivityName = activity.getLocalClassName();
  }

  /** This event is posted to our event bus when we go in/out of the background. */
  public class BackgroundChangeEvent {
    public boolean isInBackground;

    public BackgroundChangeEvent(boolean isInBackground) {
      this.isInBackground = isInBackground;
    }
  }
}
