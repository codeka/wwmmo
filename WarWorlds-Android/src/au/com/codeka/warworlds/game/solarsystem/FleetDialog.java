package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;
import java.util.TreeMap;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.game.FleetMergeDialog;
import au.com.codeka.warworlds.game.FleetMoveDialog;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class FleetDialog extends DialogFragment
                         implements StarManager.StarFetchedHandler{
    private Star mStar;
    private View mView;

    public FleetDialog() {
    }

    public void setStar(Star star) {
        mStar = star;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fleet_dlg, null);

        StarManager.getInstance().addStarUpdatedListener(mStar.getKey(), this);

        final FleetList fleetList = (FleetList) mView.findViewById(R.id.fleet_list);

        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        stars.put(mStar.getKey(), mStar);
        fleetList.refresh(mStar.getFleets(), stars);

        fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
            @Override
            public void onFleetView(Star star, Fleet fleet) {
                // won't be called here...
            }

            @Override
            public void onFleetSplit(Star star, Fleet fleet) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FleetSplitDialog dialog = new FleetSplitDialog();
                dialog.setFleet(fleet);
                dialog.show(fm, "");
            }

            @Override
            public void onFleetMove(Star star, Fleet fleet) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FleetMoveDialog dialog = new FleetMoveDialog();
                dialog.setFleet(fleet);
                dialog.show(fm, "");
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
                EmpireManager.getInstance().getEmpire().updateFleetStance(
                        getActivity(), star, fleet, newStance);
            }
        });

        // no "View" button or infobar, because it doesn't make sense here...
        mView.findViewById(R.id.view_btn).setVisibility(View.GONE);
        mView.findViewById(R.id.infobar).setVisibility(View.GONE);

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setNeutralButton("Close", null);
        return b.create();
    }

    @Override
    public void onStop() {
        super.onStop();
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    @Override
    public void onStarFetched(Star s) {
        TreeMap<String, Star> stars = new TreeMap<String, Star>();
        stars.put(s.getKey(), s);

        final FleetList fleetList = (FleetList) mView.findViewById(R.id.fleet_list);
        fleetList.refresh(s.getFleets(), stars);
    }
}
