package au.com.codeka.warworlds.game;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.ctrl.ColonyList;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireRank;
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

        getTabManager().addTab(mContext, new TabInfo("Overview", OverviewFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Colonies", ColoniesFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Build", BuildQueueFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Fleets", FleetsFragment.class, null));

        mExtras = getIntent().getExtras();
        if (mExtras != null) {
            String fleetKey = mExtras.getString("au.com.codeka.warworlds.FleetKey");
            if (fleetKey != null) {
                getTabHost().setCurrentTabByTag("Fleets");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(mContext, WarWorldsActivity.class));
                    return;
                }

                EmpireManager.getInstance().getEmpire().refreshAllDetails(new MyEmpire.RefreshAllCompleteHandler() {
                    @Override
                    public void onRefreshAllComplete(MyEmpire empire) {
                        sCurrentEmpire = empire;
                        getTabManager().reloadTab();
                        mFirstRefresh = false;
                    }
                });
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

            final View v = inflator.inflate(R.layout.empire_overview_tab, null);
            final RankListAdapter rankListAdapter = new RankListAdapter();

            MyEmpire empire = EmpireManager.getInstance().getEmpire();

            TextView empireName = (TextView) v.findViewById(R.id.empire_name);
            ImageView empireIcon = (ImageView) v.findViewById(R.id.empire_icon);

            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(empire.getShield(getActivity()));


            final ProgressBar progress = (ProgressBar) v.findViewById(R.id.progress_bar);
            final ListView rankList = (ListView) v.findViewById(R.id.empire_rankings);
            progress.setVisibility(View.VISIBLE);
            rankList.setVisibility(View.GONE);
            rankList.setAdapter(rankListAdapter);

            MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
            int myRank = myEmpire.getRank().getRank();
            int minRank = myRank - 2;
            if (minRank < 1) {
                minRank = 1;
            }
            EmpireManager.getInstance().fetchEmpiresByRank(getActivity(), minRank, minRank + 4,
                    new EmpireManager.EmpiresFetchedHandler() {
                        @Override
                        public void onEmpiresFetched(List<Empire> empires) {
                            rankListAdapter.setEmpires(empires);
                            rankList.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.GONE);
                        }
                    });
            return v;
        }

        private class RankListAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public void setEmpires(List<Empire> empires) {
                mEntries = new ArrayList<ItemEntry>();

                Collections.sort(empires, new Comparator<Empire>() {
                    @Override
                    public int compare(Empire lhs, Empire rhs) {
                        if (lhs.getRank() == null || rhs.getRank() == null) {
                            // should never happen, but just in case...
                            return lhs.getDisplayName().compareTo(rhs.getDisplayName());
                        }
                        int lhsRank = lhs.getRank().getRank();
                        int rhsRank = rhs.getRank().getRank();
                        return lhsRank - rhsRank;
                    }
                });

                int lastRank = 0;
                for (Empire empire : empires) {
                    if (lastRank != 0 && empire.getRank() != null &&
                            empire.getRank().getRank() != lastRank + 1) {
                        mEntries.add(new ItemEntry(null));
                    }
                    lastRank = empire.getRank().getRank();
                    mEntries.add(new ItemEntry(empire));
                }

                notifyDataSetChanged();
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                if (mEntries == null)
                    return 0;

                if (mEntries.get(position).empire == null)
                    return 1;
                return 0;
            }

            @Override
            public boolean isEnabled(int position) {
                // TODO: tapping empire does something?
                return false;
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
                ItemEntry entry = mEntries.get(position);
                View view = convertView;

                Activity activity = getActivity();

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    if (entry.empire == null) {
                        view = new View(activity);
                        view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10));
                    } else {
                        view = inflater.inflate(R.layout.empire_overview_rank_row, null);
                    }
                }
                if (entry.empire == null) {
                    return view;
                }

                TextView rankView = (TextView) view.findViewById(R.id.rank);
                ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                TextView totalStars = (TextView) view.findViewById(R.id.total_stars);
                TextView totalColonies = (TextView) view.findViewById(R.id.total_colonies);
                TextView totalShips = (TextView) view.findViewById(R.id.total_ships);
                TextView totalBuildings = (TextView) view.findViewById(R.id.total_buildings);

                DecimalFormat formatter = new DecimalFormat("#,##0");
                EmpireRank rank = entry.empire.getRank();
                rankView.setText(formatter.format(rank.getRank()));
                empireName.setText(entry.empire.getDisplayName());
                empireIcon.setImageBitmap(entry.empire.getShield(activity));
                totalStars.setText(Html.fromHtml(String.format("Stars: <b>%s</b>",
                        formatter.format(rank.getTotalStars()))));
                totalColonies.setText(Html.fromHtml(String.format("Colonies: <b>%s</b>",
                        formatter.format(rank.getTotalColonies()))));
                totalShips.setText(Html.fromHtml(String.format("Ships: <b>%s</b>",
                        formatter.format(rank.getTotalShips()))));
                totalBuildings.setText(Html.fromHtml(String.format("Buildings: <b>%s</b>",
                        formatter.format(rank.getTotalBuildings()))));

                return view;
            }

            public class ItemEntry {
                public Empire empire;

                public ItemEntry(Empire empire) {
                    this.empire = empire;
                }
            }
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
                public void onCollectTaxes() {
                    EmpireManager.getInstance().getEmpire().collectTaxes(context);
                }
            });

            return v;
        }
    }

    public static class BuildQueueFragment extends BaseFragment {
        public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
            if (sCurrentEmpire == null) {
                return getLoadingView(inflator);
            }

            View v = inflator.inflate(R.layout.empire_buildqueue_tab, null);

            Map<String, Colony> colonies = new TreeMap<String, Colony>();
            for (Colony colony : sCurrentEmpire.getAllColonies()) {
                colonies.put(colony.getKey(), colony);
            }
            BuildQueueList buildQueueList = (BuildQueueList) v.findViewById(R.id.build_queue);
            buildQueueList.refresh(sCurrentEmpire.getImportantStars(),
                                   colonies,
                                   sCurrentEmpire.getAllBuildRequests());

            buildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
                @Override
                public void onAccelerateClick(Star star, BuildRequest buildRequest) {
                    BuildAccelerateDialog dialog = new BuildAccelerateDialog();
                    dialog.setBuildRequest(star, buildRequest);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }

                @Override
                public void onStopClick(Star star, BuildRequest buildRequest) {
                    BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
                    dialog.setBuildRequest(star, buildRequest);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
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
            if (activity.mFirstRefresh && activity.mExtras != null) {
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
                public void onFleetMerge(Fleet fleet, List<Fleet> potentialFleets) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FleetMergeDialog dialog = new FleetMergeDialog();
                    dialog.setup(fleet, potentialFleets);
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
