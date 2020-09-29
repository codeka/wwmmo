package au.com.codeka.warworlds.game.build;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.BuildingsList;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Star;

public class BuildingsFragment extends BuildActivity.BaseTabFragment {
  private BuildingsList buildingsList;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.build_buildings_tab, container, false);
    final Star star = getStar();
    final Colony colony = getColony();

    buildingsList = v.findViewById(R.id.building_list);
    if (colony != null) {
      buildingsList.setColony(star, colony);
    }

    buildingsList.setOnItemClickListener((parent, view, position, id) -> {
      BuildingsList.Entry entry = buildingsList.getItem(position);
      if (entry.design != null) {
        BuildConfirmDialog dialog = new BuildConfirmDialog();
        dialog.setup(entry.design, star, colony);
        dialog.show(getActivity().getSupportFragmentManager(), "");
      } else if (entry.building != null) {
        BuildConfirmDialog dialog = new BuildConfirmDialog();
        dialog.setup(entry.building, star, colony);
        dialog.show(getActivity().getSupportFragmentManager(), "");
      }
    });

    return v;
  }
}
