package au.com.codeka.warworlds.client.activity;

import android.os.Build;
import android.support.v4.app.FragmentTransaction;
import android.transition.TransitionInflater;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.R;

/**
 * Manages transitions between fragments in a {@link BaseFragmentActivity}.
 */
public class FragmentTransitionManager {
  private final BaseFragmentActivity activity;
  private final int fragmentContainerId;

  private BaseFragment currFragment;

  public FragmentTransitionManager(BaseFragmentActivity activity, int fragmentContainerId) {
    this.activity = Preconditions.checkNotNull(activity);
    this.fragmentContainerId = fragmentContainerId;
  }

  /** Replace the current fragment stack with a new instance of the given fragment class. */
  public void replaceFragment(Class<? extends BaseFragment> fragmentClass) {
    BaseFragment fragment = createFragment(fragmentClass);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      fragment.setSharedElementEnterTransition(Transitions.transform());
      fragment.setSharedElementReturnTransition(Transitions.transform());
      fragment.setEnterTransition(Transitions.fade());
      fragment.setExitTransition(Transitions.fade());
    }

    FragmentTransaction trans = activity.getSupportFragmentManager().beginTransaction();
    if (currFragment != null) {
     // trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
     // trans.setCustomAnimations(R.anim.grow_fade_in, R.anim.shrink_fade_out);
      trans.addSharedElement(activity.findViewById(R.id.help_btn), "help_btn_trans");
      trans.addSharedElement(activity.findViewById(R.id.title), "title_trans");
      trans.addSharedElement(activity.findViewById(R.id.privacy_policy_btn), "website_btn_trans");
      trans.addSharedElement(activity.findViewById(R.id.start_btn), "start_btn_trans");
      trans.addSharedElement(activity.findViewById(R.id.title_icon), "title_icon_trans");
      trans.replace(fragmentContainerId, fragment);
      trans.addToBackStack(null);
    } else {
      trans.add(fragmentContainerId, fragment);
    }
    trans.commit();
    currFragment = fragment;
  }

  private <T extends BaseFragment> T createFragment(Class<T> fragmentClass) {
    try {
      return fragmentClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Fragment class does not have zero-arg constructor: "
          + fragmentClass.getName());
    }
  }
}
