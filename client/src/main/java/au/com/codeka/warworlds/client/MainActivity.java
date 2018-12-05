package au.com.codeka.warworlds.client;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import au.com.codeka.warworlds.client.ctrl.DebugView;
import au.com.codeka.warworlds.client.game.empire.EmpireScreen;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen;
import au.com.codeka.warworlds.client.game.welcome.CreateEmpireScreen;
import au.com.codeka.warworlds.client.game.welcome.WarmWelcomeScreen;
import au.com.codeka.warworlds.client.game.welcome.WelcomeScreen;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.ui.ScreenStack;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.common.Log;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity {
  private static final Log log = new Log("MainActivity");

  // Will be non-null between of onCreate/onDestroy.
  @Nullable private StarfieldManager starfieldManager;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private ScreenStack screenStack;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private ActionBarDrawerToggle drawerToggle;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private DrawerLayout drawerLayout;

  // Will be non-null between onCreate/onDestroy.
  @Nullable private NavigationView navigationView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    drawerLayout = findViewById(R.id.drawer_layout);
    navigationView = findViewById(R.id.navigation_view);

    setSupportActionBar(findViewById(R.id.toolbar));
    ActionBar actionBar = checkNotNull(getSupportActionBar());
    actionBar.show();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    drawerToggle =
        new ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.drawer_open,
            R.string.drawer_close) {
          @Override
          public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            refreshTitle();
          }

          @Override
          public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            refreshTitle();
            //searchListAdapter.setCursor(StarManager.i.getMyStars());
          }
        };
    drawerLayout.addDrawerListener(drawerToggle);
    drawerToggle.syncState();

    navigationView.setNavigationItemSelectedListener(item -> {
      //item.setChecked(true);
      switch (item.getItemId()) {
        case R.id.nav_starfield:
          screenStack.home();
          screenStack.push(new StarfieldScreen());
          break;
        case R.id.nav_empire:
          screenStack.home();
          screenStack.push(new EmpireScreen());
          break;
      }
      drawerLayout.closeDrawers();
      return true;
    });

    RenderSurfaceView renderSurfaceView = checkNotNull(findViewById(R.id.render_surface));
    renderSurfaceView.setRenderer();
    starfieldManager = new StarfieldManager(renderSurfaceView);
    starfieldManager.create();

    DebugView debugView = checkNotNull(findViewById(R.id.debug_view));
    debugView.setFrameCounter(renderSurfaceView.getFrameCounter());

    screenStack = new ScreenStack(this, findViewById(R.id.fragment_container));

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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        drawerLayout.openDrawer(GravityCompat.START);
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onResume() {
    super.onResume();
    drawerToggle.syncState();
  }

  @Override
  public void onBackPressed() {
    if (!screenStack.pop()) {
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

  private void refreshTitle() {
    ActionBar actionBar = checkNotNull(getSupportActionBar());
    if (drawerLayout.isDrawerOpen(navigationView)) {
      actionBar.setTitle("Star Search");
    } else {
      actionBar.setTitle("War Worlds 2");
    }
  }

}
