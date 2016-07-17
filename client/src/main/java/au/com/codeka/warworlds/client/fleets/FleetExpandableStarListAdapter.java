package au.com.codeka.warworlds.client.fleets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.ExpandableStarListAdapter;
import au.com.codeka.warworlds.client.util.NumberFormatter;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.client.world.StarCollection;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;

/**
 * Represents an {@link ExpandableStarListAdapter} which shows a list of fleets under each star.
 */
public class FleetExpandableStarListAdapter extends ExpandableStarListAdapter {
  private final LayoutInflater inflater;
  private final long myEmpireId;

  public FleetExpandableStarListAdapter(LayoutInflater inflater, StarCollection stars) {
    super(stars);
    this.inflater = inflater;
    myEmpireId = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire()).id;
  }

  @Override
  public int getNumChildren(Star star) {
    int numFleets = 0;
    for (int i = 0; i < star.fleets.size(); i++) {
      Long empireID = star.fleets.get(i).empire_id;
      if (empireID != null && empireID == myEmpireId) {
        numFleets++;
      }
    }

    return numFleets;
  }

  @Override
  public Object getChild(Star star, int index) {
    int fleetIndex = 0;
    for (int i = 0; i < star.fleets.size(); i++) {
      Long empireID = star.fleets.get(i).empire_id;
      if (empireID != null && empireID == myEmpireId) {
        if (fleetIndex == index) {
          return star.fleets.get(i);
        }
        fleetIndex++;
      }
    }

    // Shouldn't get here...
    return null;
  }

  @Override
  public View getStarView(Star star, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      view = inflater.inflate(R.layout.fleets_star_row, parent, false);
    }

    ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
    TextView starName = (TextView) view.findViewById(R.id.star_name);
    TextView starType = (TextView) view.findViewById(R.id.star_type);
    TextView fightersTotal = (TextView) view.findViewById(R.id.fighters_total);
    TextView nonFightersTotal = (TextView) view.findViewById(R.id.nonfighters_total);

    if (star == null) {
      starIcon.setImageBitmap(null);
      starName.setText("");
      starType.setText("");
      fightersTotal.setText("...");
      nonFightersTotal.setText("...");
    } else {
      Picasso.with(view.getContext())
          .load(ImageHelper.getStarImageUrl(view.getContext(), star, 16, 16))
          .into(starIcon);

      starName.setText(star.name);
      starType.setText(star.classification.toString());

      Empire myEmpire = Preconditions.checkNotNull(EmpireManager.i.getMyEmpire());
      float numFighters = 0.0f;
      float numNonFighters = 0.0f;

      for (Fleet fleet : star.fleets) {
        if (fleet.empire_id == null || !fleet.empire_id.equals(myEmpire.id)) {
          continue;
        }

        if (fleet.design_type == Design.DesignType.FIGHTER) {
          numFighters += fleet.num_ships;
        } else {
          numNonFighters += fleet.num_ships;
        }
      }

      fightersTotal
          .setText(String.format(Locale.ENGLISH, "%s", NumberFormatter.format(numFighters)));
      nonFightersTotal
          .setText(String.format(Locale.ENGLISH, "%s", NumberFormatter.format(numNonFighters)));
    }
    return view;
  }

  @Override
  public View getChildView(Star star, int index, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      view = inflater.inflate(R.layout.fleets_fleet_row, parent, false);
    }

    if (star != null) {
      Fleet fleet = (Fleet) getChild(star, index);
      if (fleet != null) {
        FleetListHelper.populateAllFleetRows(
            view.getContext(),
            (ViewGroup) view,
            star,
            fleet,
            DesignHelper.getDesign(fleet.design_type));
/*
        Fleet selectedFleet = fleetSelectionPanel.getFleet();
        if (selectedFleet != null && selectedFleet.getKey().equals(fleet.getKey())) {
          view.setBackgroundResource(R.color.list_item_selected);
        } else {
          view.setBackgroundResource(android.R.color.transparent);
        }
*/
      }
    }

    return view;
  }
}