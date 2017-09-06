package au.com.codeka.warworlds.client.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import au.com.codeka.warworlds.client.ui.ScreenStack;

/**
 * Base class for {@link au.com.codeka.warworlds.client.MainActivity} which manages transitions to
 * other fragments, the back stack and so on.
 */
public class BaseFragmentActivity extends AppCompatActivity {
  private FragmentTransitionManager transitionManager;

  /**
   * This must be called after you call {@link #setContentView} to create the fragment transition
   * manager.
   */
  protected void createFragmentTransitionManager(ScreenStack screenStack) {
    transitionManager = new FragmentTransitionManager(this, screenStack);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public boolean onSupportNavigateUp() {
    getSupportFragmentManager().popBackStack();
    return true;
  }

  public FragmentTransitionManager getFragmentTransitionManager() {
    return transitionManager;
  }
}
