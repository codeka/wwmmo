package au.com.codeka.warworlds.game.empire;

import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.NumberFormatter;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.ctrl.FleetListRow;
import au.com.codeka.warworlds.ctrl.FleetSelectionPanel;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireStarsFetcher;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

public class FleetsFragment extends StarsFragment {
    private FleetsStarsListAdapter adapter;
    private EmpireStarsFetcher fetcher;
    private FleetSelectionPanel fleetSelectionPanel;

    public FleetsFragment() {
        fetcher = new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Fleets, null);
        fetcher.getStars(0, 20);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empire_fleets_tab, container, false);
        ExpandableListView starsList = (ExpandableListView) v.findViewById(R.id.stars);
        adapter = new FleetsStarsListAdapter(inflater, fetcher);
        starsList.setAdapter(adapter);

        fleetSelectionPanel = (FleetSelectionPanel) v.findViewById(R.id.bottom_pane);
        fleetSelectionPanel.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetView(Star star, Fleet fleet) {
                Intent intent = new Intent();
                intent.putExtra("au.com.codeka.warworlds.Result",
                        EmpireActivity.EmpireActivityResult.NavigateToFleet.getValue());
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                intent.putExtra("au.com.codeka.warworlds.StarOffsetX", star.getOffsetX());
                intent.putExtra("au.com.codeka.warworlds.StarOffsetY", star.getOffsetY());
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
                getActivity().setResult(EmpireActivity.RESULT_OK, intent);
                getActivity().finish();
            }

            @Override
            public void onFleetSplit(Star star, Fleet fleet) {
                Bundle args = new Bundle();

                Messages.Fleet.Builder fleet_pb = Messages.Fleet.newBuilder();
                fleet.toProtocolBuffer(fleet_pb);
                args.putByteArray("au.com.codeka.warworlds.Fleet", fleet_pb.build().toByteArray());

                FragmentManager fm = getActivity().getSupportFragmentManager();
                FleetSplitDialog dialog = new FleetSplitDialog();
                dialog.setFleet(fleet);
                dialog.show(fm, "");
            }

            @Override
            public void onFleetBoost(Star star, Fleet fleet) {
                FleetManager.i.boostFleet(fleet, null);
            }

            @Override
            public void onFleetMove(Star star, Fleet fleet) {
                FleetMoveActivity.show(getActivity(), fleet);
            }

            @Override
            public void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FleetMergeDialog dialog = new FleetMergeDialog();
                dialog.setup(fleet, potentialFleets);
                dialog.show(fm, "");
            }

            @Override
            public void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance) {
                EmpireManager.i.getEmpire().updateFleetStance(star, fleet, newStance);
            }
        });

        starsList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                Star star = (Star) adapter.getGroup(groupPosition);
                Fleet fleet = (Fleet) adapter.getChild(groupPosition, childPosition);

                fleetSelectionPanel.setSelectedFleet(star, fleet);
                adapter.notifyDataSetChanged();
                return false;
            }
        });

        StarManager.eventBus.register(eventHandler);

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StarManager.eventBus.unregister(eventHandler);
    }

    private Object eventHandler = new Object() {
        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onStarUpdated(Star star) {
            if (fetcher.onStarUpdated(star)) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    public class FleetsStarsListAdapter extends StarsListAdapter {
        private LayoutInflater inflater;
        private MyEmpire empire;

        public FleetsStarsListAdapter(LayoutInflater inflater, EmpireStarsFetcher fetcher) {
            super(fetcher);
            this.inflater = inflater;
            empire = EmpireManager.i.getEmpire();
        }

        @Override
        public int getNumChildren(Star star) {
            int numFleets = 0;
            for (int i = 0; i < star.getFleets().size(); i++) {
                Integer empireID = ((Fleet) star.getFleets().get(i)).getEmpireID();
                if (empireID != null && empireID == empire.getID()) {
                    numFleets ++;
                }
            }

            return numFleets;
        }

        @Override
        public Object getChild(Star star, int index) {
            int fleetIndex = 0;
            for (int i = 0; i < star.getFleets().size(); i++) {
                Integer empireID = ((Fleet) star.getFleets().get(i)).getEmpireID();
                if (empireID != null && empireID == empire.getID()) {
                    if (fleetIndex == index) {
                        return star.getFleets().get(i);
                    }
                    fleetIndex ++;
                }
            }

            // Shouldn't get here...
            return null;
        }

        @Override
        public View getStarView(Star star, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.empire_fleet_list_star_row, parent, false);
            }

            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            TextView starName = (TextView) view.findViewById(R.id.star_name);
            TextView starType = (TextView) view.findViewById(R.id.star_type);
            TextView fightersTotal = (TextView) view.findViewById(R.id.fighters_total);
            TextView nonFightersTotal = (TextView) view.findViewById(R.id.nonfighters_total);

            if (star == null) {
                starIcon.setImageBitmap(null);
                starName.setText("");
                starType.setText("");
                fightersTotal.setText("...");
                nonFightersTotal.setText("...");
            } else {
                int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
                Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
                starIcon.setImageDrawable(new SpriteDrawable(sprite));

                starName.setText(star.getName());
                starType.setText(star.getStarType().getDisplayName());

                MyEmpire myEmpire = EmpireManager.i.getEmpire();
                float numFighters = 0.0f;
                float numNonFighters = 0.0f;

                for (BaseFleet fleet : star.getFleets()) {
                    if (fleet.getEmpireKey() == null
                            || !fleet.getEmpireKey().equals(myEmpire.getKey())) {
                        continue;
                    }

                    if (fleet.getDesignID().equals("fighter")) {
                        numFighters += fleet.getNumShips();
                    } else {
                        numNonFighters += fleet.getNumShips();
                    }
                }

                fightersTotal.setText(String.format(Locale.ENGLISH, "%s",
                        NumberFormatter.format(numFighters)));
                nonFightersTotal.setText(String.format(Locale.ENGLISH, "%s",
                        NumberFormatter.format(numNonFighters)));
            }
            return view;
        }

        @Override
        public View getChildView(Star star, int index, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = new FleetListRow(inflater.getContext());
            }

            if (star != null) {
                Fleet fleet = (Fleet) getChild(star, index);
                if (fleet != null) {
                    ((FleetListRow) view).setFleet(fleet);

                    Fleet selectedFleet = fleetSelectionPanel.getFleet();
                    if (selectedFleet != null
                            && selectedFleet.getKey().equals(fleet.getKey())) {
                        view.setBackgroundColor(0xff0c6476);
                    } else {
                        view.setBackgroundColor(0xff000000);
                    }
                }
            }

            return view;
        }
    }
}
