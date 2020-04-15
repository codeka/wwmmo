package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.build.BuildViewHelper;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.sim.DesignHelper;

/**
 * Represents a simple list of fleets, shown inside a {@link LinearLayout}.
 */
public class FleetListSimple extends LinearLayout {
  private static final Log log = new Log("FleetListSimple");

  private Context context;
  private Star star;
  @Nullable private List<Fleet> fleets;
  @Nullable private FleetFilter filter;
  private FleetSelectedHandler fleetSelectedHandler;
  private View.OnClickListener onClickListener;

  /** An interface you can implement to filter the list of fleets we display in the list. */
  public interface FleetFilter {
    boolean showFleet(Fleet fleet);
  }

  public FleetListSimple(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    setOrientation(LinearLayout.VERTICAL);
  }

  public FleetListSimple(Context context) {
    super(context);
    this.context = context;
    setOrientation(LinearLayout.VERTICAL);
  }

  public void setFleetSelectedHandler(FleetSelectedHandler handler) {
    fleetSelectedHandler = handler;
  }

  public void setStar(Star s) {
    star = s;
    filter = null;
    refresh();
  }

  public void setStar(Star s, FleetFilter f) {
    star = s;
    filter = f;
    refresh();
  }

  public int getNumFleets() {
    return fleets == null ? 0 : fleets.size();
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
        if (!f.state.equals(Fleet.FLEET_STATE.MOVING) && f.num_ships > 0.01f &&
            (filter == null || filter.showFleet(f))) {
          fleets.add(f);
        }
      }
    }

    removeAllViews();
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    if (inflater == null) {
      // TODO: huh?
      return;
    }
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
    if (fleet.fuel_amount >= maxFuel) {
      fuelLevel.setVisibility(View.GONE);
    } else {
      fuelLevel.setVisibility(View.VISIBLE);
      fuelLevel.setMax(maxFuel);
      fuelLevel.setProgress(Math.round(fleet.fuel_amount));
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
