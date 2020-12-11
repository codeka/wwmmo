package au.com.codeka.warworlds.game.build;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Star;

public class QueueFragment extends BuildFragment.BaseTabFragment implements TabManager.Reloadable {
  private BuildQueueList buildQueueList;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.build_queue_tab, container, false);

    buildQueueList = v.findViewById(R.id.build_queue);
    buildQueueList.setShowStars(false);

    buildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
      @Override
      public void onAccelerateClick(Star star, BuildRequest buildRequest) {
        BuildAccelerateDialog dialog = new BuildAccelerateDialog();
        dialog.setBuildRequest(star, buildRequest);
        dialog.show(getChildFragmentManager(), "");
      }

      @Override
      public void onStopClick(Star star, BuildRequest buildRequest) {
        BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
        dialog.setBuildRequest(star, buildRequest);
        dialog.show(getChildFragmentManager(), "");
      }
    });

    return v;
  }

  @Override
  public void reloadTab() {
    if (buildQueueList != null) {
      buildQueueList.refreshSelection();
    }
  }

  @Override
  protected void refresh(Star star, Colony colony) {
    buildQueueList.refresh(star, colony);
  }
}