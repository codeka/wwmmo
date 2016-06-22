package au.com.codeka.warworlds.client.fleets;

import android.os.Bundle;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.common.Log;

/**
 * This fragment contains a list of fleets
 */
public class FleetsFragment extends BaseFragment {
  private static final Log log = new Log("FleetFragment");
  private static final String STAR_ID_KEY = "StarID";

  public static Bundle createArguments(long starId) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starId);
    return args;
  }

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_fleets;
  }
}
