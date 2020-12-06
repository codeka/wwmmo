package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

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

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
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
    if (star.getFleets() != null) {
      for (BaseFleet f : star.getFleets()) {
        if (!f.getState().equals(Fleet.State.MOVING)
            && f.getNumShips() > 0.01f) {
          fleets.add((Fleet) f);
        }
      }
    }

    removeAllViews();
    LayoutInflater inflater = (LayoutInflater) context.getSystemService
        (Context.LAYOUT_INFLATER_SERVICE);
    for (Fleet fleet : fleets) {
      View rowView = getRowView(inflater, fleet);
      addView(rowView);
    }
  }

  private View getRowView(LayoutInflater inflater, Fleet fleet) {
    View view = inflater.inflate(R.layout.fleet_list_simple_row, null);
    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());

    ImageView icon = view.findViewById(R.id.fleet_icon);
    LinearLayout row1 = view.findViewById(R.id.ship_row1);
    LinearLayout row2 = view.findViewById(R.id.ship_row2);

    icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

    row1.removeAllViews();
    row2.removeAllViews();

    FleetListRow.populateFleetNameRow(context, row1, fleet, design);
    FleetListRow.populateFleetStanceRow(context, row2, fleet);
    if (fleet.getState() == BaseFleet.State.PROPAGANDIZING) {
      // If this fleet is propagandizing, we'll want to update the view every now & then
      postDelayed(new StanceRefreshRunnable(row2, fleet), 1000);
    }

    view.setOnClickListener(onClickListener);
    view.setTag(fleet);
    return view;
  }

  private class StanceRefreshRunnable implements Runnable {
    private final LinearLayout row2;
    private final Fleet fleet;

    public StanceRefreshRunnable(LinearLayout row2, Fleet fleet) {
      this.row2 = row2;
      this.fleet = fleet;
    }

    @Override
    public void run() {
      if (getContext() == null) {
        return;
      }

      postDelayed(this, 1000);
      FleetListRow.populateFleetStanceRow(context, row2, fleet);
    }
  }

  public interface FleetSelectedHandler {
    void onFleetSelected(Fleet fleet);
  }
}
