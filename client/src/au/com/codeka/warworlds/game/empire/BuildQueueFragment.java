package au.com.codeka.warworlds.game.empire;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.game.build.BuildAccelerateDialog;
import au.com.codeka.warworlds.game.build.BuildStopConfirmDialog;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.StarSummary;


public class BuildQueueFragment extends BaseFragment {
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        View v = inflator.inflate(R.layout.empire_buildqueue_tab, null);
        BuildQueueList buildQueueList = (BuildQueueList) v.findViewById(R.id.build_queue);
        buildQueueList.refresh(BuildManager.getInstance().getBuildRequests());
        buildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
            @Override
            public void onAccelerateClick(StarSummary star, BuildRequest buildRequest) {
                BuildAccelerateDialog dialog = new BuildAccelerateDialog();
                dialog.setBuildRequest(star, buildRequest);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }

            @Override
            public void onStopClick(StarSummary star, BuildRequest buildRequest) {
                BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
                dialog.setBuildRequest(star, buildRequest);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }
        });

        return v;
    }
}
