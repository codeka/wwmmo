package au.com.codeka.warworlds.client;

import static com.google.common.base.Preconditions.checkNotNull;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import au.com.codeka.warworlds.client.activity.BaseFragmentActivity;
import au.com.codeka.warworlds.client.ctrl.DebugView;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.welcome.CreateEmpireFragment;
import au.com.codeka.warworlds.client.game.welcome.WarmWelcomeFragment;
import au.com.codeka.warworlds.client.game.welcome.WelcomeScreen;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.ui.ScreenStack;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.common.Log;

public class MainActivity extends BaseFragmentActivity {
  private static final Log log = new Log("MainActivity");

  // Will be non-null between of onCreate/onDestroy.
  @Nullable private StarfieldManager starfieldManager;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private ScreenStack screenStack;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    setSupportActionBar(findViewById(R.id.toolbar));
    checkNotNull(getSupportActionBar()).hide();

    RenderSurfaceView renderSurfaceView = checkNotNull(findViewById(R.id.render_surface));
    renderSurfaceView.setRenderer();
    starfieldManager = new StarfieldManager(renderSurfaceView);
    starfieldManager.create();

    DebugView debugView = checkNotNull(findViewById(R.id.debug_view));
    debugView.setFrameCounter(renderSurfaceView.getFrameCounter());

    screenStack = new ScreenStack(findViewById(R.id.fragment_container));
    createFragmentTransitionManager(screenStack);

    if (savedInstanceState != null) {
      // TODO: restore the view state?
    }
    if (!GameSettings.i.getBoolean(GameSettings.Key.WARM_WELCOME_SEEN)) {
      getFragmentTransitionManager().replaceFragment(WarmWelcomeFragment.class);
    } else if (GameSettings.i.getString(GameSettings.Key.COOKIE).isEmpty()) {
      getFragmentTransitionManager().replaceFragment(CreateEmpireFragment.class);
    } else {
      screenStack.push(new WelcomeScreen());
    }
  }

  @NonNull
  public StarfieldManager getStarfieldManager() {
    return checkNotNull(starfieldManager);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    starfieldManager.destroy();
    starfieldManager = null;
  }
}
