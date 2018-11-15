package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.widget.ListView;

import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Star;

public class QueueView extends ListView implements TabContentView {
  private final Context context;
  private final Star star;
  private final Colony colony;

  public QueueView(Context context, Star star, Colony colony) {
    super(context);
    this.context = context;
    this.star = star;
    this.colony = colony;

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
  }

  @Override
  public void refresh(Star star, Colony colony) {
    // TODO
  }

}