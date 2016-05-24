package au.com.codeka.warworlds.client.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.common.Log;

/**
 * Manages transitions between fragments in a {@link BaseFragmentActivity}.
 */
public class FragmentTransitionManager {
  private static final Log log = new Log("FragmentTransitionManager");
  private final BaseFragmentActivity activity;
  private final int fragmentContainerId;

  private BaseFragment currFragment;

  public FragmentTransitionManager(BaseFragmentActivity activity, int fragmentContainerId) {
    this.activity = Preconditions.checkNotNull(activity);
    this.fragmentContainerId = fragmentContainerId;
  }

  /** Replace the current fragment stack with a new instance of the given fragment class. */
  public void replaceFragment(Class<? extends BaseFragment> fragmentClass) {
    replaceFragment(fragmentClass, null, null);
  }

  /** Replace the current fragment stack with a new instance of the given fragment class. */
  public void replaceFragment(Class<? extends BaseFragment> fragmentClass, @Nullable Bundle args) {
    replaceFragment(fragmentClass, args, null);
  }

  /** Replace the current fragment stack with a new instance of the given fragment class. */
  public void replaceFragment(Class<? extends BaseFragment> fragmentClass,
      @Nullable SharedViewHolder sharedViews) {
    replaceFragment(fragmentClass, null, sharedViews);
  }

  /** Replace the current fragment stack with a new instance of the given fragment class. */
  public void replaceFragment(Class<? extends BaseFragment> fragmentClass,
      @Nullable Bundle args, @Nullable SharedViewHolder sharedViews) {
    BaseFragment fragment = createFragment(fragmentClass, args);
    FragmentTransaction trans = activity.getSupportFragmentManager().beginTransaction();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      fragment.setSharedElementEnterTransition(Transitions.transform());
      fragment.setSharedElementReturnTransition(Transitions.transform());
      fragment.setEnterTransition(Transitions.fade());
      fragment.setExitTransition(Transitions.fade());
    } else {
      trans.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
      trans.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    if (sharedViews != null) {
      for (SharedViewHolder.SharedView sharedView : sharedViews.getSharedViews()) {
        View v = activity.findViewById(sharedView.getViewId());
        if (v == null) {
          log.warning("No shared view with id #%d for transition to '%s' in %s.",
              sharedView.getViewId(), sharedView.getTransitionName(),
              fragmentClass.getSimpleName());
          continue;
        }
        trans.addSharedElement(v, sharedView.getTransitionName());
      }
    }

    if (currFragment != null) {
      trans.replace(fragmentContainerId, fragment);
      trans.addToBackStack(null);
    } else {
      trans.add(fragmentContainerId, fragment);
    }
    trans.commit();
    currFragment = fragment;
  }

  private <T extends BaseFragment> T createFragment(Class<T> fragmentClass, @Nullable Bundle args) {
    try {
      T fragment = fragmentClass.newInstance();
      fragment.setArguments(args);
      return fragment;
    } catch (Exception e) {
      throw new RuntimeException("Fragment class does not have zero-arg constructor: "
          + fragmentClass.getName());
    }
  }
}
