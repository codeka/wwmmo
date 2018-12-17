package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.build.BuildViewHelper;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;

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
      onClickListener = v -> {
        Fleet fleet = (Fleet) v.getTag();
        if (fleetSelectedHandler != null) {
          fleetSelectedHandler.onFleetSelected(fleet);
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

    ImageView icon = view.findViewById(R.id.fleet_icon);
    TextView row1 = view.findViewById(R.id.fleet_row1);
    TextView row2 = view.findViewById(R.id.fleet_row2);
    ProgressBar fuelLevel = view.findViewById(R.id.fuel_level);


    int maxFuel = (int) (design.fuel_size * fleet.num_ships);
    int fuelAmount = fleet.fuel_amount == null ? 0 : (int) (float) fleet.fuel_amount;
    if (maxFuel == fuelAmount) {
      fuelLevel.setVisibility(View.GONE);
    } else {
      fuelLevel.setVisibility(View.VISIBLE);
      fuelLevel.setMax(maxFuel);
      fuelLevel.setProgress(fuelAmount);
    }

    BuildViewHelper.setDesignIcon(design, icon);

    row1.setText(FleetListHelper.getFleetName(fleet, design));
    row2.setText(FleetListHelper.getFleetStance(fleet));

    view.setOnClickListener(onClickListener);
    view.setTag(fleet);
    return view;
  }

  public interface FleetSelectedHandler {
    void onFleetSelected(Fleet fleet);
  }
}
