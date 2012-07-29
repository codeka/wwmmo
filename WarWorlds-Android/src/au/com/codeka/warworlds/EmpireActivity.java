package au.com.codeka.warworlds;

import java.util.HashSet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import au.com.codeka.warworlds.ctrl.ColonyList;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.game.FleetMoveDialog;
import au.com.codeka.warworlds.game.FleetSplitDialog;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;

/**
 * This dialog shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireActivity extends TabFragmentActivity {

    private static MyEmpire sCurrentEmpire;

    public enum EmpireActivityResult {
        NavigateToPlanet(1);

        private int mValue;

        public static EmpireActivityResult fromValue(int value) {
            for (EmpireActivityResult res : values()) {
                if (res.mValue == value) {
                    return res;
                }
            }

            throw new IllegalArgumentException("value is not a valid EmpireActivityResult");
        }

        public int getValue() {
            return mValue;
        }

        EmpireActivityResult(int value) {
            mValue = value;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sCurrentEmpire = null;
        refresh();

        addTab("Overview", OverviewFragment.class, null);
        addTab("Colonies", ColoniesFragment.class, null);
        addTab("Fleets", FleetsFragment.class, null);
    }

    public void refresh() {
        EmpireManager.getInstance().getEmpire().refreshAllDetails(new MyEmpire.RefreshAllCompleteHandler() {
            @Override
            public void onRefreshAllComplete(MyEmpire empire) {
                sCurrentEmpire = empire;
                reloadTab();
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d = DialogManager.getInstance().onCreateDialog(this, id);
        if (d == null)
            d = super.onCreateDialog(id);
        return d;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d, Bundle args) {
        DialogManager.getInstance().onPrepareDialog(this, id, d, args);
        super.onPrepareDialog(id, d, args);
    }

    public static class BaseFragment extends Fragment {
        /**
         * Gets a view to display if we're still loading the empire details.
         */
        protected View getLoadingView(LayoutInflater inflator) {
            return inflator.inflate(R.layout.empire_loading_tab, null);
        }
    }

    public static class OverviewFragment extends BaseFragment {
        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            if (sCurrentEmpire == null) {
                return getLoadingView(inflator);
            }

            View v = inflator.inflate(R.layout.empire_overview_tab, null);
            HashSet<String> colonizedStarKeys = new HashSet<String>();
            for (Colony c : sCurrentEmpire.getAllColonies()) {
                colonizedStarKeys.add(c.getStarKey());
            }

            int totalShips = 0;
            for (Fleet f : sCurrentEmpire.getAllFleets()) {
                totalShips += f.getNumShips();
            }

            // Current value of R.id.empire_overview_format (in English):
            // %1$d stars and %2$d planets colonized
            // %3$d ships in %4$d fleets
            String fmt = getActivity().getString(R.string.empire_overview_format);
            final TextView overviewText = (TextView) v.findViewById(R.id.overview_text);
            String overview = String.format(fmt,
                    colonizedStarKeys.size(), sCurrentEmpire.getAllColonies().size(),
                    totalShips, sCurrentEmpire.getAllFleets().size());
            overviewText.setText(Html.fromHtml(overview));

            return v;
        }
    }

    public static class ColoniesFragment extends BaseFragment {
        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            if (sCurrentEmpire == null) {
                return getLoadingView(inflator);
            }

            View v = inflator.inflate(R.layout.empire_colonies_tab, null);
            ColonyList colonyList = (ColonyList) v.findViewById(R.id.colony_list);
            colonyList.refresh(sCurrentEmpire.getAllColonies(), sCurrentEmpire.getImportantStars());

            colonyList.setOnViewColonyListener(new ColonyList.ViewColonyHandler() {
                @Override
                public void onViewColony(Star star, Colony colony) {
                    Planet planet = star.getPlanets()[colony.getPlanetIndex() - 1];
                    // end this activity, go back to the starfield and navigate to the given colony

                    Intent intent = new Intent();
                    intent.putExtra("au.com.codeka.warworlds.Result", EmpireActivityResult.NavigateToPlanet.getValue());
                    intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                    intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                    intent.putExtra("au.com.codeka.warworlds.StarOffsetX", star.getOffsetX());
                    intent.putExtra("au.com.codeka.warworlds.StarOffsetY", star.getOffsetY());
                    intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                    intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planet.getIndex());
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                }
            });

            return v;
        }
    }

    public static class FleetsFragment extends BaseFragment {
        @Override
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            if (sCurrentEmpire == null) {
                return getLoadingView(inflator);
            }

            View v = inflator.inflate(R.layout.empire_fleets_tab, null);
            FleetList fleetList = (FleetList) v.findViewById(R.id.fleet_list);
            fleetList.refresh(getActivity(), sCurrentEmpire.getAllFleets(), sCurrentEmpire.getImportantStars());

            fleetList.setOnFleetActionListener(new FleetList.OnFleetActionListener() {
                @Override
                public void onFleetSplit(Star star, Fleet fleet) {
                    Bundle args = new Bundle();
                    args.putParcelable("au.com.codeka.warworlds.Fleet", fleet);
                    DialogManager.getInstance().show(getActivity(), FleetSplitDialog.class, args,
                            new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    ((EmpireActivity) getActivity()).refresh();
                                }
                            });
                }

                @Override
                public void onFleetMove(Star star, Fleet fleet) {
                    Bundle args = new Bundle();
                    args.putString("au.com.codeka.warworlds.StarKey", fleet.getStarKey());
                    args.putString("au.com.codeka.warworlds.FleetKey", fleet.getKey());
                    DialogManager.getInstance().show(getActivity(), FleetMoveDialog.class, args);
                }
            });

            return v;
        }
    }
}
