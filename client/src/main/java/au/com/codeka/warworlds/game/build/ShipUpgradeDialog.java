package au.com.codeka.warworlds.game.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.BuildEstimateView;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

import com.google.protobuf.InvalidProtocolBufferException;

import static com.google.common.base.Preconditions.checkNotNull;

public class ShipUpgradeDialog extends DialogFragment {
  private final Log log = new Log("ShipUpgradeDialog");
  private Button buildNowBtn;
  private Star star;
  private Colony colony;
  private Fleet fleet;
  private BuildEstimateView buildEstimateView;
  private ShipDesign.Upgrade upgrade;

  public void setup(Star star, Colony colony, Fleet fleet) {
    Bundle args = new Bundle();

    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
    star.toProtocolBuffer(star_pb);
    args.putByteArray("au.com.codeka.warworlds.Star", star_pb.build().toByteArray());
    args.putString("au.com.codeka.warworlds.FleetKey", fleet.getKey());
    args.putString("au.com.codeka.warworlds.ColonyKey", colony.getKey());

    setArguments(args);
  }

  @Override
  @NonNull
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    fetchArguments();

    final Activity activity = checkNotNull(getActivity());
    LayoutInflater inflater = activity.getLayoutInflater();
    @SuppressLint("InflateParams") // no parent for dialogs
    View view = inflater.inflate(R.layout.build_ship_upgrade_dlg, null);
    buildNowBtn = view.findViewById(R.id.build_now_btn);

    ImageView fleetIcon = view.findViewById(R.id.fleet_icon);
    TextView fleetName = view.findViewById(R.id.fleet_name);
    ListView upgradesList = view.findViewById(R.id.upgrades);
    TextView upgradesNone = view.findViewById(R.id.upgrades_none);
    buildEstimateView = view.findViewById(R.id.build_estimate);
    buildEstimateView.setOnBuildEstimateRefreshRequired(
        new BuildEstimateView.BuildEstimateRefreshRequiredHandler() {
          @Override
          public void onBuildEstimateRefreshRequired() {
            refreshBuildEstimate();
            refreshBuildNowCost();
          }
        });

    ShipDesign design = fleet.getDesign();
    Sprite sprite = SpriteManager.i.getSprite(design.getSpriteName());
    fleetIcon.setImageDrawable(new SpriteDrawable(sprite));

    fleetName.setText(String.format(Locale.ENGLISH, "%d × %s",
        (int) fleet.getNumShips(), design.getDisplayName()));

    final UpgradeListAdapter upgradeListAdapter = new UpgradeListAdapter();
    upgradesList.setAdapter(upgradeListAdapter);

    ArrayList<ShipDesign.Upgrade> upgrades = new ArrayList<>();
    for (ShipDesign.Upgrade upgrade : design.getUpgrades()) {
      if (!fleet.hasUpgrade(upgrade.getID())) {
        upgrades.add(upgrade);
      }
    }
    if (upgrades.size() > 0) {
      log.debug("%d updates available.", upgrades.size());
      upgradeListAdapter.setup(upgrades);

      // select the first one by default
      upgradeListAdapter.setSelectedItem(0);
      upgrade = (ShipDesign.Upgrade) upgradeListAdapter.getItem(0);
      refreshBuildEstimate();
      refreshBuildNowCost();

      upgradesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
          upgradeListAdapter.setSelectedItem(position);

          upgrade = (ShipDesign.Upgrade) upgradeListAdapter.getItem(position);
          refreshBuildEstimate();
          refreshBuildNowCost();
        }
      });
    } else {
      log.debug("No upgrades available.");
      upgradesList.setVisibility(View.GONE);
      upgradesNone.setVisibility(View.VISIBLE);
    }

    buildNowBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onUpgradeClick(true);
      }
    });

    return new StyledDialog.Builder(getActivity())
        .setView(view)
        .setPositiveButton("Upgrade", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            onUpgradeClick(false);
          }
        })
        .setNegativeButton("Cancel", null)
        .create();
  }

  private void fetchArguments() {
    try {
      Bundle args = checkNotNull(getArguments());
      Messages.Star star_pb =
          Messages.Star.parseFrom(args.getByteArray("au.com.codeka.warworlds.Star"));
      star = new Star();
      star.fromProtocolBuffer(star_pb);

      String fleetKey = args.getString("au.com.codeka.warworlds.FleetKey");
      for (BaseFleet baseFleet : star.getFleets()) {
        if (baseFleet.getKey().equals(fleetKey)) {
          fleet = (Fleet) baseFleet;
          break;
        }
      }

      String colonyKey = args.getString("au.com.codeka.warworlds.ColonyKey");
      for (BaseColony baseColony : star.getColonies()) {
        if (baseColony.getKey().equals(colonyKey)) {
          colony = (Colony) baseColony;
          break;
        }
      }
    } catch (InvalidProtocolBufferException e) {
      // ignore . . .
    }
  }

  private void refreshBuildEstimate() {
    if (upgrade == null) {
      buildEstimateView.refresh(star, null);
      return;
    }

    final DateTime startTime = DateTime.now();

    BuildRequest buildRequest = new BuildRequest("FAKE_BUILD_REQUEST",
        DesignKind.SHIP, fleet.getDesignID(), colony.getKey(), startTime,
        (int) fleet.getNumShips(), null, 0, Integer.parseInt(fleet.getKey()),
        upgrade.getID(), star.getKey(), colony.getPlanetIndex(), colony.getEmpireKey(),
        null);

    buildEstimateView.refresh(star, buildRequest);
  }

  private void refreshBuildNowCost() {
    double cost = upgrade.getBuildCost().getCostInMinerals() * fleet.getNumShips();
    buildNowBtn.setText(String.format(Locale.ENGLISH, "Upgrade now ($%.0f)", cost));
  }

  private void onUpgradeClick(boolean accelerateImmediately) {
    if (upgrade == null) {
      dismiss();
      return;
    }

    final Activity activity = getActivity();

    BuildManager.i.build(
        activity, colony, fleet.getDesign(), Integer.parseInt(fleet.getKey()),
        (int) fleet.getNumShips(), upgrade.getID(), accelerateImmediately);
    dismiss();
  }

  /**
   * This adapter is used to populate the list of upgrade designs in our view.
   */
  private class UpgradeListAdapter extends BaseAdapter {
    private List<ShipDesign.Upgrade> mEntries;
    private ShipDesign.Upgrade mSelectedEntry;

    public void setup(List<ShipDesign.Upgrade> upgrades) {
      mEntries = new ArrayList<>(upgrades);
      notifyDataSetChanged();
    }

    void setSelectedItem(int position) {
      mSelectedEntry = mEntries.get(position);
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      if (mEntries == null)
        return 0;
      return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
      if (mEntries == null)
        return null;
      return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ShipDesign.Upgrade entry = mEntries.get(position);

      View view = convertView;
      if (view == null) {
        Activity activity = getActivity();
        if (activity == null) {
          return null;
        }

        LayoutInflater inflater =
            (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.build_ship_upgrade_row, parent, false);
      }

      if (mSelectedEntry != null && mSelectedEntry.getID().equals(entry.getID())) {
        view.setBackgroundResource(R.color.list_item_selected);
      } else {
        view.setBackgroundResource(R.color.list_item_normal);
      }

      ImageView upgradeIcon = view.findViewById(R.id.upgrade_icon);
      TextView upgradeName = view.findViewById(R.id.upgrade_name);
      TextView upgradeDescription = view.findViewById(R.id.upgrade_description);

      upgradeIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(entry.getSpriteName())));
      upgradeName.setText(entry.getDisplayName());
      upgradeDescription.setText(Html.fromHtml(entry.getDescription()));

      return view;
    }
  }
}
