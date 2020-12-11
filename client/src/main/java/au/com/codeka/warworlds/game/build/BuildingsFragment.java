package au.com.codeka.warworlds.game.build;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.BuildingsList;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Star;

public class BuildingsFragment extends BuildFragment.BaseTabFragment {
  private Star star;
  private Colony colony;
  private BuildingsList buildingsList;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.build_buildings_tab, container, false);

    buildingsList = v.findViewById(R.id.building_list);

    buildingsList.setOnItemClickListener((parent, view, position, id) -> {
      BuildingsList.Entry entry = buildingsList.getItem(position);
      if (entry.design != null) {
        BuildConfirmDialog dialog = new BuildConfirmDialog();
        dialog.setup(entry.design, star, colony);
        dialog.show(getChildFragmentManager(), "");
      } else if (entry.building != null) {
        BuildConfirmDialog dialog = new BuildConfirmDialog();
        dialog.setup(entry.building, star, colony);
        dialog.show(getChildFragmentManager(), "");
      }
    });

    return v;
  }

  @Override
  protected void refresh(Star star, Colony colony) {
    this.star = star;
    this.colony = colony;

    buildingsList.setColony(star, colony);
  }
}
