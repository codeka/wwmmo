package au.com.codeka.warworlds.client.starfield;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;

/**
 * This is the main fragment that shows the starfield, lets you navigating around, select stars
 * and fleets and so on.
 */
public class StarfieldFragment extends BaseFragment {
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.frag_starfield, container, false);
  }
}
