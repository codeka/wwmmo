package au.com.codeka.warworlds.client.starfield;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.transitionseverywhere.AutoTransition;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionManager;

import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This is the main fragment that shows the starfield, lets you navigate around, select stars
 * and fleets and so on.
 */
public class StarfieldFragment extends BaseFragment {
  private final Log log = new Log("StarfieldFragment");

  private ViewGroup bottomPane;
  private Button allianceBtn;

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.frag_starfield, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final SelectionDetailsView selectionDetailsView =
        (SelectionDetailsView) view.findViewById(R.id.selection_details);
    bottomPane = (ViewGroup) view.findViewById(R.id.bottom_pane);
    allianceBtn = (Button) view.findViewById(R.id.alliance_btn);

    hideBottomPane(false);
    ((MainActivity) getActivity()).getStarfieldManager().setTapListener(
        new StarfieldManager.TapListener() {
      @Override
      public void onStarTapped(@Nullable Star star) {
        if (star == null) {
          hideBottomPane(false);
        } else {
          showBottomPane();
          selectionDetailsView.showStar(star);
        }
      }
    });
  }

  private void hideBottomPane(boolean instant) {
    applyBottomPaneAnimation(false, instant);
  }

  private void showBottomPane() {
    applyBottomPaneAnimation(true, false);
  }

  private boolean isPortrait() {
    return getResources().getBoolean(R.bool.is_portrait);
  }

  private void applyBottomPaneAnimation(boolean isOpen, boolean instant) {
    float dp;
    if (isPortrait()) {
      if (isOpen) {
        dp = 180;
      } else {
        dp = 34;
      }
    } else {
      if (isOpen) {
        dp = 200;
      } else {
        dp = 100;
      }
    }

    Resources r = getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());

    if (!instant) {
      Transition transition = new AutoTransition()
          .setInterpolator(new AccelerateDecelerateInterpolator());
      TransitionManager.beginDelayedTransition(bottomPane, transition);
    }
    if (isPortrait()) {
      bottomPane.getLayoutParams().height = (int) px;
    } else {
      RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) allianceBtn.getLayoutParams();
      if (isOpen) {
        // NB: removeRule is not available until API level 17 :/
        lp.addRule(RelativeLayout.BELOW, 0);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        lp.topMargin =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, r.getDisplayMetrics());
      } else {
        lp.addRule(RelativeLayout.BELOW, R.id.empire_btn);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        lp.topMargin =
            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, r.getDisplayMetrics());
      }

      bottomPane.getLayoutParams().width = (int) px;
    }
    bottomPane.requestLayout();
  }
}
