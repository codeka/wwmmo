package au.com.codeka.warworlds.game.empire;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveActivity;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.game.empire.EmpireActivity.EmpireActivityResult;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.Star;


public class FleetsFragment extends BaseFragment {
    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        //if (sStars == null || sCurrentEmpire == null) {
            return getLoadingView(inflator);
        //}
/*
        ArrayList<BaseFleet> fleets = new ArrayList<BaseFleet>();
        for (Star s : sStars.values()) {
            for (BaseFleet f : s.getFleets()) {
                if (f.getEmpireKey() != null && f.getEmpireKey().equals(sCurrentEmpire.getKey())) {
                    fleets.add(f);
                }
            }
        }

        View v = inflator.inflate(R.layout.empire_fleets_tab, null);
        FleetList fleetList = (FleetList) v.findViewById(R.id.fleet_list);
        fleetList.refresh(fleets, sStars);

        EmpireActivity activity = (EmpireActivity) getActivity();
        if (activity.mFirstStarsRefresh && activity.mExtras != null) {
            String fleetKey = activity.mExtras.getString("au.com.codeka.warworlds.FleetKey");
            if (fleetKey != null) {
                fleetList.selectFleet(fleetKey, true);
            }
        }

        fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetView(Star star, Fleet fleet) {
                Intent intent = new Intent();
                intent.putExtra("au.com.codeka.warworlds.Result", EmpireActivityResult.NavigateToFleet.getValue());
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                intent.putExtra("au.com.codeka.warworlds.StarOffsetX", star.getOffsetX());
                intent.putExtra("au.com.codeka.warworlds.StarOffsetY", star.getOffsetY());
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                intent.putExtra("au.com.codeka.warworlds.FleetKey", fleet.getKey());
                getActivity().setResult(RESULT_OK, intent);
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

        return v;*/
    }
}
