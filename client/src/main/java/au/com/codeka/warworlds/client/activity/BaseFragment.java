package au.com.codeka.warworlds.client.activity;

import android.support.v4.app.Fragment;

import com.google.common.base.Preconditions;

/**
 * Base class for fragments, managed by {@link FragmentTransitionManager} and hosted inside of
 * {@link BaseFragmentActivity}.
 */
public class BaseFragment extends Fragment {
  public BaseFragmentActivity getFragmentActivity() {
    return (BaseFragmentActivity) getActivity();
  }

  public FragmentTransitionManager getFragmentTransitionManager() {
    return getFragmentActivity().getFragmentTransitionManager();
  }
}
