package au.com.codeka.warworlds.client.activity;

import com.google.common.base.Preconditions;

/**
 * Manages transitions between fragments in a {@link BaseFragmentActivity}.
 */
public class FragmentTransitionManager {
  private final BaseFragmentActivity activity;
  private final int fragmentContainerId;

  public FragmentTransitionManager(BaseFragmentActivity activity, int fragmentContainerId) {
    this.activity = Preconditions.checkNotNull(activity);
    this.fragmentContainerId = fragmentContainerId;
  }

  /** Replace the current fragment stack with a new instance of the given fragment class. */
  public void replaceFragment(Class<? extends BaseFragment> fragmentClass) {
    BaseFragment fragment = createFragment(fragmentClass);
    activity.getSupportFragmentManager()
        .beginTransaction()
        .replace(fragmentContainerId, fragment)
        .commit();
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
