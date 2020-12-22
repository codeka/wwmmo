package au.com.codeka.warworlds.game.solarsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

public class EmptyPlanetFragment extends BaseFragment {
  private Star star;
  private Planet planet;

  private PlanetDetailsView planetDetailsView;

  private EmptyPlanetFragmentArgs args;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.planet_empty, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    args = EmptyPlanetFragmentArgs.fromBundle(requireArguments());

    ActivityBackgroundGenerator.setBackground(view);
    planetDetailsView = view.findViewById(R.id.planet_details);

    Button colonizeBtn = (Button) view.findViewById(R.id.colonize_btn);
    colonizeBtn.setOnClickListener(v -> onColonizeClick());
  }

  @Override
  public void onResume() {
    super.onResume();

    ServerGreeter.waitForHello(requireMainActivity(), (success, greeting) -> {
      if (!success) {
        // TODO: should we return errors?
      } else {
        StarManager.eventBus.register(eventHandler);
        Star star = StarManager.i.getStar(args.getStarID());
        if (star != null) {
          refresh(star);
        }
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    StarManager.eventBus.unregister(eventHandler);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star s) {
      if (star != null && !star.getKey().equals(s.getKey())) {
        return;
      }

      refresh(s);
    }
  };

  private void refresh(Star s) {
    star = s;
    planet = (Planet) star.getPlanets()[args.getPlanetIndex() - 1];

    planetDetailsView.setup(star, planet, null);
  }

  private void onColonizeClick() {
    MyEmpire empire = EmpireManager.i.getEmpire();

    // check that we have a colony ship (the server will check too, but this is easy)
    boolean hasColonyShip = false;
    for (BaseFleet fleet : star.getFleets()) {
      if (fleet.getEmpireKey() == null) {
        continue;
      }

      if (fleet.getEmpireKey().equals(empire.getKey())) {
        if (fleet.getDesignID().equals("colonyship")) { // TODO: hardcoded?
          hasColonyShip = true;
        }
      }
    }

    if (!hasColonyShip) {
      // TODO: better errors...
      StyledDialog dialog = new StyledDialog.Builder(requireContext()).setMessage(
          "You don't have a colony ship around this star, so you cannot colonize this planet.")
          .setPositiveButton("OK", null).create();
      dialog.show();
    }

    empire.colonize(planet, colony -> NavHostFragment.findNavController(this).popBackStack());
  }
}
