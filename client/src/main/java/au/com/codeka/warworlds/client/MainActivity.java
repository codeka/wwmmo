package au.com.codeka.warworlds.client;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import au.com.codeka.warworlds.client.ctrl.DebugView;
import au.com.codeka.warworlds.client.ctrl.drawer.DrawerController;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen;
import au.com.codeka.warworlds.client.game.welcome.CreateEmpireScreen;
import au.com.codeka.warworlds.client.game.welcome.WarmWelcomeScreen;
import au.com.codeka.warworlds.client.game.welcome.WelcomeScreen;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.ui.ScreenStack;
import au.com.codeka.warworlds.client.util.GameSettings;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity {
  // Will be non-null between of onCreate/onDestroy.
  @Nullable private StarfieldManager starfieldManager;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private ScreenStack screenStack;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private DrawerController drawerController;

  private FrameLayout fragmentContainer;
  private View topPane;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    topPane = findViewById(R.id.top_pane);

    setSupportActionBar(findViewById(R.id.toolbar));

    RenderSurfaceView renderSurfaceView = checkNotNull(findViewById(R.id.render_surface));
    renderSurfaceView.setRenderer();
    starfieldManager = new StarfieldManager(renderSurfaceView);
    starfieldManager.create();

    DebugView debugView = checkNotNull(findViewById(R.id.debug_view));
    debugView.setFrameCounter(renderSurfaceView.getFrameCounter());

    fragmentContainer = checkNotNull(findViewById(R.id.fragment_container));
    screenStack = new ScreenStack(this, fragmentContainer);

    drawerController = new DrawerController(
        this,
        screenStack,
        getSupportActionBar(),
        findViewById(R.id.drawer_layout),
        findViewById(R.id.drawer_content));

    if (savedInstanceState != null) {
      // TODO: restore the view state?
    }
    if (!GameSettings.i.getBoolean(GameSettings.Key.WARM_WELCOME_SEEN)) {
      screenStack.push(new WarmWelcomeScreen());
    } else if (GameSettings.i.getString(GameSettings.Key.COOKIE).isEmpty()) {
      screenStack.push(new CreateEmpireScreen());
    } else {
      screenStack.push(new WelcomeScreen());
    }
  }

  public void setToolbarVisible(boolean visible) {
    int marginSize;
    if (visible) {
      TypedValue typedValue = new TypedValue();
      getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);

      int[] attribute = new int[]{android.R.attr.actionBarSize};
      TypedArray array = obtainStyledAttributes(typedValue.resourceId, attribute);
      marginSize = array.getDimensionPixelSize(0, -1);
      array.recycle();

      // Adjust the margin by a couple of dp, the top pane has that strip of transparent pixels
      marginSize -= TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

      topPane.setVisibility(View.VISIBLE);
      getSupportActionBar().show();
    } else {
      marginSize = 0;

      topPane.setVisibility(View.GONE);
      getSupportActionBar().hide();
    }

    ((FrameLayout.LayoutParams) fragmentContainer.getLayoutParams()).topMargin = marginSize;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        drawerController.toggleDrawer();
        return true;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    if (!screenStack.backTo(StarfieldScreen.class)) {
      super.onBackPressed();
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
