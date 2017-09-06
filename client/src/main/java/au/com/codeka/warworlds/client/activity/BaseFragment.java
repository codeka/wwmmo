package au.com.codeka.warworlds.client.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.client.ui.Screen;

/**
 * Base class for "fragments". As we migrate from "real" fragments to {@link Screen}s, this class no
 * longer actually inherits from Android's Fragments, and instead just implements the subset of
 * methods we care about.
 */
public class BaseFragment {
  private Context context;
  private View rootView;
  private Bundle arguments;

  public BaseFragmentActivity getFragmentActivity() {
    return (BaseFragmentActivity) context;
  }

  public void startActivity(Intent intent) {
    getFragmentActivity().startActivity(intent);
  }

  public Context getContext() {
    return context;
  }

  public FragmentTransitionManager getFragmentTransitionManager() {
    return getFragmentActivity().getFragmentTransitionManager();
  }

  public void onCreate(Bundle savedInstanceState) {
  }

  public void setArguments(Bundle bundle) {
    this.arguments = bundle;
  }

  protected Bundle getArguments() {
    return arguments;
  }

  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      Bundle savedInstanceState) {
    this.context = inflater.getContext();

    if (rootView == null) {
      int resourceId = getViewResourceId();
      if (resourceId > 0) {
        rootView = inflater.inflate(getViewResourceId(), container, false);
      }
    }

    this.onViewCreated(rootView, savedInstanceState);
    return rootView;
  }

  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
  }

  public void onStart() {
  }

  public void onResume() {
  }

  public void onPause() {
  }

  public void onStop() {
  }

  public void onDestroy() {
  }

  protected int getViewResourceId() {
    return 0;
  }
}
