package au.com.codeka.warworlds.client.build;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import au.com.codeka.warworlds.client.R;

public class BuildingsFragment extends BuildFragment.BaseTabFragment {
  //private BuildingsList buildingsList;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.frag_build_buildings, container, false);
    //final Star star = getStar();
    //final Colony colony = getColony();

    //buildingsList = (BuildingsList) v.findViewById(R.id.building_list);
    //if (colony != null) {
    //  buildingsList.setColony(star, colony);
    //}

    //buildingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    //  @Override
    //  public void onItemClick(AdapterView<?> parent, View view,
    //      int position, long id) {
    //    BuildingsList.Entry entry = buildingsList.getItem(position);
    //    if (entry.design != null) {
    //      BuildConfirmDialog dialog = new BuildConfirmDialog();
    //      dialog.setup(entry.design, star, colony);
    //      dialog.show(getActivity().getSupportFragmentManager(), "");
    //    } else if (entry.building != null) {
    //      BuildConfirmDialog dialog = new BuildConfirmDialog();
    //      dialog.setup(entry.building, star, colony);
    //      dialog.show(getActivity().getSupportFragmentManager(), "");
    //    }
    //  }
    //});

    return v;
  }
}
