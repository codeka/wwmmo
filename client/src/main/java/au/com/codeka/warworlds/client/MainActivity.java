package au.com.codeka.warworlds.client;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.activity.BaseFragmentActivity;
import au.com.codeka.warworlds.client.ctrl.DebugView;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.game.welcome.CreateEmpireFragment;
import au.com.codeka.warworlds.client.game.welcome.WarmWelcomeFragment;
import au.com.codeka.warworlds.client.game.welcome.WelcomeFragment;
import au.com.codeka.warworlds.common.Log;

public class MainActivity extends BaseFragmentActivity {
  private static final Log log = new Log("MainActivity");

  // Will be non-null between of onCreate/onDestroy.
  @Nullable private StarfieldManager starfieldManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    createFragmentTransitionManager(R.id.fragment_container);

    setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    Preconditions.checkNotNull(getSupportActionBar()).hide();

    RenderSurfaceView renderSurfaceView =
        (RenderSurfaceView) Preconditions.checkNotNull(findViewById(R.id.render_surface));
    renderSurfaceView.setRenderer();
    starfieldManager = new StarfieldManager(renderSurfaceView);
    starfieldManager.create();

    DebugView debugView = (DebugView) Preconditions.checkNotNull(findViewById(R.id.debug_view));
    debugView.setFrameCounter(renderSurfaceView.getFrameCounter());

    if (savedInstanceState == null) {
      if (!GameSettings.i.getBoolean(GameSettings.Key.WARM_WELCOME_SEEN)) {
        getFragmentTransitionManager().replaceFragment(WarmWelcomeFragment.class);
      } else if (GameSettings.i.getString(GameSettings.Key.COOKIE).isEmpty()) {
        getFragmentTransitionManager().replaceFragment(CreateEmpireFragment.class);
      } else {
        getFragmentTransitionManager().replaceFragment(WelcomeFragment.class);
      }
    }
  }

  @NonNull
  public StarfieldManager getStarfieldManager() {
    return Preconditions.checkNotNull(starfieldManager);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    starfieldManager.destroy();
    starfieldManager = null;
  }
}
