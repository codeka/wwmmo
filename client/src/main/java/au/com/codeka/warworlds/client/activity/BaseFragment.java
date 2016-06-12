package au.com.codeka.warworlds.client.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.R;

/**
 * Base class for fragments, managed by {@link FragmentTransitionManager} and hosted inside of
 * {@link BaseFragmentActivity}.
 */
public class BaseFragment extends Fragment {
  private View rootView;

  public BaseFragmentActivity getFragmentActivity() {
    return (BaseFragmentActivity) getActivity();
  }

  public FragmentTransitionManager getFragmentTransitionManager() {
    return getFragmentActivity().getFragmentTransitionManager();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    if (rootView == null) {
      int resourceId = getViewResourceId();
      if (resourceId > 0) {
        rootView = inflater.inflate(getViewResourceId(), container, false);
      }
    }
    return rootView;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (rootView != null && rootView.getParent() != null) {
      ((ViewGroup) rootView.getParent()).removeView(rootView);
    }
  }

  protected int getViewResourceId() {
    return 0;
  }
}
