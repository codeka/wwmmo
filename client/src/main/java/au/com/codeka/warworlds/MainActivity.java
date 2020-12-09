package au.com.codeka.warworlds;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.ui.DrawerController;

/**
 * The main activity of the whole app. All our fragments are children of this.
 */
// TODO: when there's no other activities, get rid of BaseActivity.
public class MainActivity extends BaseActivity {
  private static final Log log = new Log("MainActivity");

  private DrawerController drawerController;
  private StarfieldManager starfieldManager;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Util.loadProperties();
    Util.setup(this);

    if (onBlueStacks()) {
      Toast.makeText(
          this,
          "Sorry, this platform is not supported. Please use a supported platform.",
          Toast.LENGTH_LONG).show();
      finish();
    }

    setContentView(R.layout.main_activity);
    setSupportActionBar(findViewById(R.id.toolbar));

    RenderSurfaceView renderSurfaceView = findViewById(R.id.render_surface);
    renderSurfaceView.create();
    starfieldManager = new StarfieldManager(renderSurfaceView);
    starfieldManager.create();
  }

  @Override
  protected void onStart() {
    super.onStart();

    NavController navController = Navigation.findNavController(this, R.id.main_content);

    final SharedPreferences prefs = Util.getSharedPreferences();
    if (!prefs.getBoolean("WarmWelcome", false)) {
      // if we've never done the warm-welcome, do it now
      log.info("Starting Warm Welcome");
      navController.navigate(R.id.warmWelcomeFragment);
    } else if (RealmContext.i.getCurrentRealm() == null) {
      log.info("No realm selected, switching to RealmSelectActivity");
      navController.navigate(R.id.realmSelectFragment);
    }

    DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
    AppBarConfiguration appBarConfiguration =
        new AppBarConfiguration.Builder(R.id.starfieldFragment, R.id.solarSystemFragment)
            .setOpenableLayout(drawerLayout)
            .build();
    NavigationUI.setupWithNavController(
        (Toolbar) findViewById(R.id.toolbar), navController, appBarConfiguration);
    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    drawerController = new DrawerController(navController, drawerLayout);
    drawerController.start();

    navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
      ActionBar actionBar = getSupportActionBar();
      if (actionBar == null) {
        return;
      }

      if (arguments != null && arguments.getBoolean("hideToolbar")) {
        actionBar.hide();
      } else {
        actionBar.show();
      }
    });
  }

  @Override
  protected void onStop() {
    super.onStop();
    drawerController.stop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    starfieldManager.destroy();
    starfieldManager = null;
  }

  public StarfieldManager getStarfieldManager() {
    return starfieldManager;
  }

  private boolean onBlueStacks() {
    File sharedFolder = new File(
        Environment.getExternalStorageDirectory(), "/windows/BstSharedFolder");
    return sharedFolder.exists();
  }
}
