package au.com.codeka.warworlds.client.build;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.TabManager;

public class QueueFragment extends BuildFragment.BaseTabFragment implements TabManager.Reloadable {
 // private BuildQueueList buildQueueList;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.frag_build_queue, container, false);
/*
    final Star star = getStar();
    final Colony colony = getColony();
    if (star == null)
      return inflater.inflate(R.layout.build_loading_tab, container, false);

    buildQueueList = (BuildQueueList) v.findViewById(R.id.build_queue);
    buildQueueList.setShowStars(false);
    buildQueueList.refresh(star, colony);

    buildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
      @Override
      public void onAccelerateClick(Star star, BuildRequest buildRequest) {
        BuildAccelerateDialog dialog = new BuildAccelerateDialog();
        dialog.setBuildRequest(star, buildRequest);
        dialog.show(getActivity().getSupportFragmentManager(), "");
      }

      @Override
      public void onStopClick(Star star, BuildRequest buildRequest) {
        BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
        dialog.setBuildRequest(star, buildRequest);
        dialog.show(getActivity().getSupportFragmentManager(), "");
      }
    });
*/
    return v;
  }

  @Override
  public void reloadTab() {
    //if (buildQueueList != null) {
   //   buildQueueList.refreshSelection();
   // }
  }
}