package au.com.codeka.warworlds.ui;

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.MainActivity;

public class BaseFragment extends Fragment {
  @Nullable
  protected MainActivity getMainActivity() {
    return (MainActivity) getContext();
  }

  protected MainActivity requireMainActivity() {
    return (MainActivity) requireContext();
  }

  /**
   * Helper function to determine whether we're in portrait orientation or not.
   */
//  @SuppressWarnings("deprecation") // need to support older devices as well
  protected boolean isPortrait() {
    // TODO: use a resource selector instead.
    Display display = ((WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    int width = display.getWidth();
    int height = display.getHeight();
    return height > width;
  }
}
