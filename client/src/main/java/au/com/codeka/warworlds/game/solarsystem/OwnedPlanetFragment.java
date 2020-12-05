package au.com.codeka.warworlds.game.solarsystem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.ActivityBackgroundGenerator;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiRequest;
import au.com.codeka.warworlds.api.RequestManager;
import au.com.codeka.warworlds.ctrl.PlanetDetailsView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.build.BuildActivity;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class OwnedPlanetFragment extends Fragment {
  private static final Log log = new Log("OwnedPlanetActivity");

  private Star star;
  private Planet planet;
  private Colony colony;

  private FocusView focusView;
  private PlanetDetailsView planetDetails;
  private TextView buildQueueLength;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.planet_owned, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ActivityBackgroundGenerator.setBackground(view);

    final Button buildButton = view.findViewById(R.id.build_btn);
    buildButton.setOnClickListener(v -> {
      if (star == null) {
        return; // can happen before the star loads
      }
      if (colony == null) {
        return; // shouldn't happen, the button should be hidden.
      }

      Intent intent = new Intent(requireContext(), BuildActivity.class);
      intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());

      Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
      colony.toProtocolBuffer(colony_pb);
      intent.putExtra("au.com.codeka.warworlds.Colony", colony_pb.build().toByteArray());
      startActivity(intent);
    });

    planetDetails = view.findViewById(R.id.planet_details);
    focusView = view.findViewById(R.id.focus);
    view.findViewById(R.id.update_focus_btn).setOnClickListener(v -> focusView.save());
    buildQueueLength = view.findViewById(R.id.build_queue_length);

    view.findViewById(R.id.abandon_btn).setOnClickListener(v -> onAbandonColony());

    if (requireContext() instanceof OwnedPlanetActivity) {
      refresh(((OwnedPlanetActivity) requireContext()).getStar());
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    StarManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    StarManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star s) {
      if (star != null && !star.getKey().equals(s.getKey())) {
        return;
      }

      refresh(s);
    }
  };

  private void onAbandonColony() {
    new StyledDialog.Builder(requireContext()).setMessage(Html.fromHtml(
        "Are you sure you want to abandon this colony? The colony will revert to native if you" +
            " do."))
        .setTitle("Abandon colony")
        .setPositiveButton("Abandon", (dialog, which) -> {
          new BackgroundRunner<Void>() {
            @Override
            protected Void doInBackground() {
              String url = String.format("stars/%s/colonies/%s/abandon",
                  colony.getStarKey(), colony.getKey());

              RequestManager.i.sendRequest(new ApiRequest.Builder(url, "POST").build());
              return null;
            }

            @Override
            protected void onComplete(Void unused) {
              // notify the StarManager that this star has been updated
              StarManager.i.refreshStar(Integer.parseInt(colony.getStarKey()));

              // also finish the activity and go back to the solarsystem view.
              //TODO finish();
            }
          }.execute();

          dialog.dismiss();;
        })
        .setNegativeButton("Cancel", null).create().show();
  }

  private void refresh(Star s) {
    int planetIndex = requireArguments().getInt("au.com.codeka.warworlds.PlanetIndex");

    star = s;
    planet = (Planet) star.getPlanets()[planetIndex - 1];
    for (BaseColony colony : star.getColonies()) {
      if (colony.getPlanetIndex() == planetIndex) {
        this.colony = (Colony) colony;
      }
    }

    planetDetails.setup(star, planet, colony);
    focusView.setColony(star, colony);

    int totalBuildRequests = 0;
    for (BaseBuildRequest buildRequest : star.getBuildRequests()) {
      if (buildRequest.getPlanetIndex() == planetIndex) {
        totalBuildRequests ++;
      }
    }
    buildQueueLength.setText(String.format(Locale.US, "Build queue: %d", totalBuildRequests));
  }
}
