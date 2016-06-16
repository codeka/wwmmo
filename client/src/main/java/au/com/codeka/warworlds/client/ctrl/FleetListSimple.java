package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.build.BuildHelper;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Represents a simple list of fleets, shown inside a {@link LinearLayout}.
 */
public class FleetListSimple extends LinearLayout {
  private Context context;
  private Star star;
  private List<Fleet> fleets;
  private FleetSelectedHandler fleetSelectedHandler;
  private View.OnClickListener onClickListener;

  public FleetListSimple(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
  }

  public FleetListSimple(Context context) {
    super(context);
    this.context = context;
  }

  public void setFleetSelectedHandler(FleetSelectedHandler handler) {
    fleetSelectedHandler = handler;
  }

  public void setStar(Star s) {
    star = s;
    refresh();
  }

  private void refresh() {
    if (onClickListener == null) {
      onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Fleet fleet = (Fleet) v.getTag();
          if (fleetSelectedHandler != null) {
            fleetSelectedHandler.onFleetSelected(fleet);
          }
        }
      };
    }

    fleets = new ArrayList<>();
    if (star.fleets != null) {
      for (Fleet f : star.fleets) {
        if (!f.state.equals(Fleet.FLEET_STATE.MOVING) && f.num_ships > 0.01f) {
          fleets.add(f);
        }
      }
    }

    removeAllViews();
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    for (Fleet fleet : fleets) {
      View rowView = getRowView(inflater, fleet);
      addView(rowView);
    }
  }

  private View getRowView(LayoutInflater inflater, Fleet fleet) {
    View view = inflater.inflate(R.layout.ctrl_fleet_list_simple_row, this, false);
    Design design = DesignHelper.getDesign(fleet.design_type);

    ImageView icon = (ImageView) view.findViewById(R.id.fleet_icon);
    LinearLayout row1 = (LinearLayout) view.findViewById(R.id.ship_row1);
    LinearLayout row2 = (LinearLayout) view.findViewById(R.id.ship_row2);

    BuildHelper.setDesignIcon(design, icon);

    row1.removeAllViews();
    row2.removeAllViews();

    FleetListHelper.populateFleetNameRow(context, row1, fleet, design);
    FleetListHelper.populateFleetStanceRow(context, row2, fleet);

    view.setOnClickListener(onClickListener);
    view.setTag(fleet);
    return view;
  }

  public interface FleetSelectedHandler {
    void onFleetSelected(Fleet fleet);
  }
}
