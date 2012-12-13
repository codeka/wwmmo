package au.com.codeka.warworlds.game;

import java.util.HashSet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.ctrl.ColonyList;
import au.com.codeka.warworlds.ctrl.FleetList;
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

    Context mContext = this;
    Bundle mExtras = null;
    boolean mFirstRefresh = true;

    public enum EmpireActivityResult {
        NavigateToPlanet(1),
        NavigateToFleet(2);

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

        getTabManager().addTab(mContext, new TabInfo("Overview", OverviewFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Colonies", ColoniesFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Fleets", FleetsFragment.class, null));

        mExtras = getIntent().getExtras();
        if (mExtras != null) {
            String fleetKey = mExtras.getString("au.com.codeka.warworlds.FleetKey");
            if (fleetKey != null) {
                getTabHost().setCurrentTabByTag("Fleets");
            }
        }
    }

    public void refresh() {
        EmpireManager.getInstance().getEmpire().refreshAllDetails(new MyEmpire.RefreshAllCompleteHandler() {
            @Override
            public void onRefreshAllComplete(MyEmpire empire) {
                sCurrentEmpire = empire;
                getTabManager().reloadTab();
                mFirstRefresh = false;
            }
        });
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

            MyEmpire empire = EmpireManager.getInstance().getEmpire();

            TextView empireName = (TextView) v.findViewById(R.id.empire_name);
            ImageView empireIcon = (ImageView) v.findViewById(R.id.empire_icon);

            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(empire.getShield(getActivity()));

            HashSet<String> colonizedStarKeys = new HashSet<String>();
            for (Colony c : sCurrentEmpire.getAllColonies()) {
                colonizedStarKeys.add(c.getStarKey());
            }

            int totalShips = 0;
            for (Fleet f : sCurrentEmpire.getAllFleets()) {
                totalShips += f.getNumShips();
            }

            // Current value of R.id.empire_overview_format (in English):
            // %1$d stars and %2$d planets colonised
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

            final Context context = getActivity();

            View v = inflator.inflate(R.layout.empire_colonies_tab, null);
            ColonyList colonyList = (ColonyList) v.findViewById(R.id.colony_list);
            colonyList.refresh(sCurrentEmpire.getAllColonies(), sCurrentEmpire.getImportantStars());

            colonyList.setOnColonyActionListener(new ColonyList.ColonyActionHandler() {
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

                @Override
                public void onCollectTaxes(Star star, Colony colony) {
                    EmpireManager.getInstance().getEmpire().collectTaxes(context, star, colony);
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
            fleetList.refresh(sCurrentEmpire.getAllFleets(), sCurrentEmpire.getImportantStars());

            final Context context = getActivity();

            EmpireActivity activity = (EmpireActivity) getActivity();
            if (activity.mFirstRefresh) {
                String fleetKey = activity.mExtras.getString("au.com.codeka.warworlds.FleetKey");
                if (fleetKey != null) {
                    fleetList.selectFleet(fleetKey);
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
                    args.putParcelable("au.com.codeka.warworlds.Fleet", fleet);

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
                public void onFleetStanceModified(Star star, Fleet fleet, Fleet.Stance newStance) {
                    EmpireManager.getInstance().getEmpire().updateFleetStance(context, star,
                                                                              fleet, newStance);
                }
            });

            return v;
        }
    }
}
