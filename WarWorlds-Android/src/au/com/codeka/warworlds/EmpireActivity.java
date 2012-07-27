package au.com.codeka.warworlds;

import java.util.HashSet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sCurrentEmpire = null;
        EmpireManager.getInstance().getEmpire().refreshAllDetails(new MyEmpire.RefreshAllCompleteHandler() {
            @Override
            public void onRefreshAllComplete(MyEmpire empire) {
                sCurrentEmpire = empire;
                reloadTab();
            }
        });

        addTab("Overview", OverviewFragment.class, null);
        addTab("Colonies", ColoniesFragment.class, null);
        addTab("Fleets", FleetsFragment.class, null);
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
                    // TODO: end this activity, go back to the starfield one and
                    // navigate to the given colony
                    //getActivity().navigateToPlanet(star, planet, true);
                    //dismiss();
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

            return v;
        }
    }
}
