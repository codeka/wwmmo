package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.BaseFleetUpgrade;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.FleetListRow;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.StarManager;

public class FleetMergeDialog extends DialogFragment {
  private Fleet fleet;
  private List<Fleet> potentialMergeTargets;
  private View view;
  private Fleet selectedFleet;
  private FleetListRow selectedRow;
  private LinearLayout fleetList;
  private StyledDialog dialog;

  public FleetMergeDialog() {
  }

  public void setup(Fleet fleet, List<Fleet> potentialFleets) {
    potentialMergeTargets = potentialFleets;
    this.fleet = fleet;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.fleet_merge_dlg, null);

    fleetList = view.findViewById(R.id.ship_list);
    final TextView note = view.findViewById(R.id.note);
    boolean isError = false;

    if (!fleet.getState().equals(Fleet.State.IDLE)) {
      note.setText("You cannot merge a fleet unless it is Idle.");
      isError = true;
    } else {
      ArrayList<Fleet> otherFleets = new ArrayList<>();
      for (Fleet f : potentialMergeTargets) {
        if (f.getKey().equals(fleet.getKey())) {
          continue;
        }
        if (!f.getStarKey().equals(fleet.getStarKey())) {
          continue;
        }
        if (f.getEmpireKey() == null || fleet.getEmpireKey() == null
            || !f.getEmpireKey().equals(fleet.getEmpireKey())) {
          continue;
        }
        if (!f.getDesignID().equals(fleet.getDesignID())) {
          continue;
        }
        if (!f.getState().equals(Fleet.State.IDLE)) {
          continue;
        }
        otherFleets.add(f);
      }

      if (otherFleets.isEmpty()) {
        note.setText("No other fleet is suitable for merging.");
        isError = true;
      } else {
        refreshFleets(otherFleets);
      }
    }

    StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
    b.setView(view);
    b.setTitle("Merge Fleet");

    if (!isError) {
      b.setPositiveButton("Merge", (dialog, which) -> onMergeClick());
    }

    b.setNegativeButton("Cancel", null);

    dialog = b.create();
    dialog.setOnShowListener(d -> dialog.getPositiveButton().setEnabled(false));
    return dialog;
  }

  private void onMergeClick() {
    if (selectedFleet == null) {
      return;
    }

    // if they have different upgrades, issue a warning that you'll lose them
    boolean hasDifferentUpgrades = false;
    for (BaseFleetUpgrade upgrade1 : fleet.getUpgrades()) {
      boolean exists = false;
      for (BaseFleetUpgrade upgrade2 : selectedFleet.getUpgrades()) {
        if (upgrade1.getUpgradeID().equals(upgrade2.getUpgradeID())) {
          exists = true;
        }
      }
      if (!exists) {
        hasDifferentUpgrades = true;
      }
    }
    if (hasDifferentUpgrades) {
      new StyledDialog.Builder(getActivity())
          .setMessage("These fleets have different upgrades, you'll lose any upgrades that aren't" +
              " on both fleets. Are you sure you want to merge them?")
          .setTitle("Different upgrades")
          .setPositiveButton("Merge", (dialog, which) -> {
            dialog.dismiss();
            doMerge();
          }).setNegativeButton("Cancel", null)
          .create().show();
    } else {
      doMerge();
    }
  }

  private void doMerge() {
    dismiss();

    new BackgroundRunner<Boolean>() {
      @Override
      protected Boolean doInBackground() {
        String url = String.format("stars/%s/fleets/%s/orders",
            fleet.getStarKey(),
            fleet.getKey());
        Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
            .setOrder(Messages.FleetOrder.FLEET_ORDER.MERGE)
            .setMergeFleetKey(selectedFleet.getKey())
            .build();

        try {
          return ApiClient.postProtoBuf(url, fleetOrder);
        } catch (ApiException e) {
          // TODO: do something..?
          return false;
        }
      }

      @Override
      protected void onComplete(Boolean success) {
        // the star this fleet is attached to needs to be refreshed...
        StarManager.i.refreshStar(Integer.parseInt(fleet.getStarKey()));
      }

    }.execute();
  }

  private void refreshFleets(ArrayList<Fleet> fleets) {
    Collections.sort(fleets, (lhs, rhs) -> {
      // by definition, they'll all be the same design so just sort based on number of ships.
      return (int) (rhs.getNumShips() - lhs.getNumShips());
    });

    for (Fleet fleet : fleets) {
      FleetListRow row = new FleetListRow(getContext());
      row.setFleet(fleet);
      row.setTag(fleet);
      row.setOnClickListener(rowClickListener);

      fleetList.addView(row);
    }
  }

  private View.OnClickListener rowClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (selectedRow != null) {
        selectedRow.setBackgroundResource(android.R.color.transparent);
      }
      selectedFleet = (Fleet) v.getTag();
      selectedRow = (FleetListRow) v;
      selectedRow.setBackgroundResource(R.color.list_item_selected);

      dialog.getPositiveButton().setEnabled(true);
    }
  };
}
