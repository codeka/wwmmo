package au.com.codeka.warworlds.ui;

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
}
