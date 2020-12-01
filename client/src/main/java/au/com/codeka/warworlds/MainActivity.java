package au.com.codeka.warworlds;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.ui.FragmentConfig;

/**
 * The main activity of the whole app. All our fragments are children of this.
 */
// TODO: when there's no other activities, get rid of BaseActivity.
public class MainActivity extends BaseActivity {
  private static final Log log = new Log("MainActivity");

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

    // TODO: support this
    //setSupportActionBar(findViewById(R.id.toolbar));

    final SharedPreferences prefs = Util.getSharedPreferences();
    if (!prefs.getBoolean("WarmWelcome", false)) {
      // if we've never done the warm-welcome, do it now
      log.info("Starting Warm Welcome");
      startActivity(new Intent(this, WarmWelcomeActivity.class));
      return;
    }

    if (RealmContext.i.getCurrentRealm() == null) {
      log.info("No realm selected, switching to RealmSelectActivity");
      startActivity(new Intent(this, RealmSelectActivity.class));
      return;
    }

    setContentFragment(new WelcomeFragment());
  }

  private void setContentFragment(Fragment fragment) {
    getSupportFragmentManager().beginTransaction()
        .add(R.id.main_content, fragment)
        .runOnCommit(() -> {
          updateForFragment(fragment);
        })
        .commit();
  }

  private void updateForFragment(Fragment fragment) {
    FragmentConfig config = fragment.getClass().getAnnotation(FragmentConfig.class);
    if (config == null) {
      // create a default config
      config = DefaultFragmentConfig.class.getAnnotation(FragmentConfig.class);
    }

    // TODO: support this
    /*
    if (config.hideToolbar()) {
      getSupportActionBar().hide();
    } else {
      getSupportActionBar().show();
    }
    */
  }

  /**
   * This is a dummy class solely for the purpose of retaining a default instance of the fragment
   * config.
   */
  @FragmentConfig
  private static class DefaultFragmentConfig {
  }

  private boolean onBlueStacks() {
    File sharedFolder = new File(
        Environment.getExternalStorageDirectory(), "/windows/BstSharedFolder");
    return sharedFolder.exists();
  }
}
