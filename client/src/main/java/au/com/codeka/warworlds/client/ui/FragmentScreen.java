package au.com.codeka.warworlds.client.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.activity.BaseFragment;

/**
 * Helper {@link Screen} to wrap an android {@link BaseFragment} (until we can migrate all of them
 * to the new system).
 *
 * Note that we don't honor the full fragment lifecycle, we just call enough methods to make sure
 * things works for our app. This is definitely *not* a generic implementation!
 */
public class FragmentScreen extends Screen {
  private final BaseFragment fragment;
  private View view;

  public FragmentScreen(BaseFragment fragment) {
    this.fragment = fragment;
  }

  @Override
  public void onCreate(ScreenContext context, LayoutInflater inflater, ViewGroup container) {
    fragment.onCreate(null);
    view = fragment.onCreateView(inflater, container, null);
  }

  @Override
  public View onShow() {
    fragment.onStart();
    fragment.onResume();
    return view;
  }

  @Override
  public void onHide() {
    fragment.onPause();
    fragment.onStop();
  }

  @Override
  public void onDestroy() {
    fragment.onDestroy();
  }
}
