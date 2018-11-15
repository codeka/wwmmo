package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * The planet list that shows up on the starfield view. It also includes details about empires
 * around the star.
 */
public class PlanetListSimple extends LinearLayout {
  private Context context;
  private Star star;
  private List<Planet> planets;
  private PlanetSelectedHandler planetSelectedHandler;
  private View.OnClickListener onClickListener;

  public PlanetListSimple(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
  }

  public PlanetListSimple(Context context) {
    super(context);
    this.context = context;
  }

  public void setPlanetSelectedHandler(PlanetSelectedHandler handler) {
    planetSelectedHandler = handler;
  }

  public void setStar(Star s) {
    star = s;
    refresh();
  }

  private void refresh() {
    if (onClickListener == null) {
      onClickListener = v -> {
        Planet planet = (Planet) v.getTag();
        if (planetSelectedHandler != null) {
          planetSelectedHandler.onPlanetSelected(planet);
        }
      };
    }

    planets = new ArrayList<>(star.planets);

    removeAllViews();
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    Empire myEmpire = EmpireManager.i.getMyEmpire();
    HashSet<Long> empires = new HashSet<>();
    for (Fleet fleet : star.fleets) {
      if (fleet.empire_id == null) {
        continue;
      }
      if (fleet.empire_id.equals(myEmpire.id)) {
        continue;
      }
      if (empires.contains(fleet.empire_id)) {
        continue;
      }
      empires.add(fleet.empire_id);
    }
    for (Planet planet : star.planets) {
      if (planet.colony == null) {
        continue;
      }
      if (planet.colony.empire_id == null) {
        continue;
      }
      if (planet.colony.empire_id.equals(myEmpire.id)) {
        continue;
      }
      if (empires.contains(planet.colony.empire_id)) {
        continue;
      }
      empires.add(planet.colony.empire_id);
    }
    for (long empireID : empires) {
      View rowView = getEmpireRowView(inflater, empireID);
      addView(rowView);
    }
    if (!empires.isEmpty()) {
      // add a spacer...
      View spacer = new View(context);
      spacer.setLayoutParams(new LinearLayout.LayoutParams(10, 10));
      addView(spacer);
      spacer = new View(context);
      spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
      spacer.setBackgroundColor(0x33ffffff);
      addView(spacer);
    }
    for (int i = 0; i < planets.size(); i++) {
      View rowView = getPlanetRowView(inflater, planets.get(i), i);
      addView(rowView);
    }
  }

  private View getPlanetRowView(LayoutInflater inflater, Planet planet, int planetIndex) {
    View view = inflater.inflate(R.layout.ctrl_planet_list_simple_row, this, false);

    final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
    Picasso.get()
        .load(ImageHelper.getPlanetImageUrl(getContext(), star, planetIndex, 32, 32))
        .into(icon);

    TextView planetTypeTextView = (TextView) view.findViewById(R.id.starfield_planet_type);
    planetTypeTextView.setText(planet.planet_type.toString());

    Colony colony = planet.colony;
    final TextView colonyTextView = (TextView) view.findViewById(R.id.starfield_planet_colony);
    if (colony != null) {
      if (colony.empire_id == null) {
        colonyTextView.setText(getContext().getString(R.string.native_colony));
      } else {
        Empire empire = EmpireManager.i.getEmpire(colony.empire_id);
        if (empire != null) {
          colonyTextView.setText(empire.display_name);
        } else {
          colonyTextView.setText(context.getString(R.string.colonized));
          // TODO: update when the empire comes around.
        }
      }
    } else {
      colonyTextView.setText("");
    }

    view.setOnClickListener(onClickListener);
    view.setTag(planet);
    return view;
  }

  private View getEmpireRowView(LayoutInflater inflater, long empireID) {
    final View view = inflater.inflate(R.layout.ctrl_planet_list_simple_row, this, false);
    final ImageView icon = (ImageView) view.findViewById(R.id.starfield_planet_icon);
    final TextView empireName = (TextView) view.findViewById(R.id.starfield_planet_type);
    final TextView allianceName = (TextView) view.findViewById(R.id.starfield_planet_colony);

    Empire empire = EmpireManager.i.getEmpire(empireID);
    if (empire != null) {
      Picasso.get()
          .load(ImageHelper.getEmpireImageUrl(getContext(), empire, 32, 32))
          .into(icon);

      empireName.setText(empire.display_name);
//      if (empire.alliance != null) {
//        allianceName.setText(empire.alliance.name);
//      }
    }

    view.setOnClickListener(onClickListener);
    view.setTag(empireID);
    return view;
  }

  public interface PlanetSelectedHandler {
    void onPlanetSelected(Planet planet);
  }
}
