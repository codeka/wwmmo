package au.com.codeka.warworlds.game.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
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
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

import com.google.protobuf.InvalidProtocolBufferException;

public class ShipUpgradeDialog extends DialogFragment {
    private View mView;
    private Star mStar;
    private Colony mColony;
    private Fleet mFleet;
    private BuildEstimateView mBuildEstimateView;
    private ShipDesign.Upgrade mUpgrade;

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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        fetchArguments();

        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.build_ship_upgrade_dlg, null);

        ImageView fleetIcon = (ImageView) mView.findViewById(R.id.fleet_icon);
        TextView fleetName = (TextView) mView.findViewById(R.id.fleet_name);
        ListView upgradesList = (ListView) mView.findViewById(R.id.upgrades);
        mBuildEstimateView = (BuildEstimateView) mView.findViewById(R.id.build_estimate);
        mBuildEstimateView.setOnBuildEstimateRefreshRequired(new BuildEstimateView.BuildEstimateRefreshRequiredHandler() {
            @Override
            public void onBuildEstimateRefreshRequired() {
                refreshBuildEstimate();
            }
        });

        ShipDesign design = mFleet.getDesign();
        Sprite sprite = SpriteManager.i.getSprite(design.getSpriteName());
        fleetIcon.setImageDrawable(new SpriteDrawable(sprite));

        fleetName.setText(String.format(Locale.ENGLISH, "%d × %s",
                (int) mFleet.getNumShips(), design.getDisplayName()));

        final UpgradeListAdapter upgradeListAdapter = new UpgradeListAdapter();
        upgradesList.setAdapter(upgradeListAdapter);
        upgradeListAdapter.setup(design.getUpgrades());

        upgradesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                upgradeListAdapter.setSelectedItem(position);

                mUpgrade = (ShipDesign.Upgrade) upgradeListAdapter.getItem(position);
                refreshBuildEstimate();
            }
        });

        return new StyledDialog.Builder(getActivity())
               .setView(mView)
               .setPositiveButton("Upgrade", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       onUpgradeClick();
                   }
               })
               .setNegativeButton("Cancel", null)
                       .create();
    }

    private void fetchArguments() {
        try {
            Bundle args = getArguments();
            Messages.Star star_pb = Messages.Star.parseFrom(args.getByteArray("au.com.codeka.warworlds.Star"));
            mStar = new Star();
            mStar.fromProtocolBuffer(star_pb);

            String fleetKey = args.getString("au.com.codeka.warworlds.FleetKey");
            for (BaseFleet baseFleet : mStar.getFleets()) {
                if (baseFleet.getKey().equals(fleetKey)) {
                    mFleet = (Fleet) baseFleet;
                    break;
                }
            }

            String colonyKey = args.getString("au.com.codeka.warworlds.ColonyKey");
            for (BaseColony baseColony : mStar.getColonies()) {
                if (baseColony.getKey().equals(colonyKey)) {
                    mColony = (Colony) baseColony;
                    break;
                }
            }
        } catch (InvalidProtocolBufferException e) {
            // ignore . . .
        }
    }

    private void refreshBuildEstimate() {
        if (mUpgrade == null) {
            mBuildEstimateView.refresh(mStar, null);
            return;
        }

        final DateTime startTime = DateTime.now();

        BuildRequest buildRequest = new BuildRequest("FAKE_BUILD_REQUEST",
                DesignKind.SHIP, mFleet.getDesignID(), mColony.getKey(), startTime,
                (int) mFleet.getNumShips(), null, 0, Integer.parseInt(mFleet.getKey()),
                mUpgrade.getID(), mStar.getKey(), mColony.getPlanetIndex(), mColony.getEmpireKey());

        mBuildEstimateView.refresh(mStar, buildRequest);
    }

    private void onUpgradeClick() {
        final Activity activity = getActivity();

        BuildManager.getInstance().build(activity, mColony, mFleet.getDesign(), Integer.parseInt(mFleet.getKey()),
                (int) mFleet.getNumShips(), mUpgrade.getID());

        dismiss();
    }

    /** This adapter is used to populate the list of upgrade designs in our view. */
    private class UpgradeListAdapter extends BaseAdapter {
        private List<ShipDesign.Upgrade> mEntries;
        private ShipDesign.Upgrade mSelectedEntry;

        public void setup(List<ShipDesign.Upgrade> upgrades) {
            mEntries = new ArrayList<ShipDesign.Upgrade>(upgrades);
            notifyDataSetChanged();
        }

        public void setSelectedItem(int position) {
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
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.build_ship_upgrade_row, parent, false);
            }

            if (mSelectedEntry != null && mSelectedEntry.getID().equals(entry.getID())) {
                view.setBackgroundResource(R.drawable.list_item_selected);
            } else {
                view.setBackgroundResource(R.drawable.list_item_normal);
            }

            ImageView upgradeIcon = (ImageView) view.findViewById(R.id.upgrade_icon);
            TextView upgradeName = (TextView) view.findViewById(R.id.upgrade_name);
            TextView upgradeDescription = (TextView) view.findViewById(R.id.upgrade_description);

            upgradeIcon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(entry.getSpriteName())));
            upgradeName.setText(entry.getDisplayName());
            upgradeDescription.setText(Html.fromHtml(entry.getDescription()));

            return view;
        }
    }
}
