package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.game.BuildAccelerateDialog;
import au.com.codeka.warworlds.game.BuildStopConfirmDialog;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * When you click "Build" shows you the list of buildings/ships that are/can be built by your
 * colony. You can swipe left/right to switch between your colonies in this star.
 */
public class BuildActivity extends BaseActivity implements StarManager.StarFetchedHandler {
    private Star mStar;
    private List<Colony> mColonies;
    private ViewPager mViewPager;
    private ColonyPagerAdapter mColonyPagerAdapter;
    private Colony mInitialColony;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_build);

        mColonyPagerAdapter = new ColonyPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mColonyPagerAdapter); 
    }

    @Override
    public void onResume() {
        super.onResume();
        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
                mInitialColony = (Colony) extras.getParcelable("au.com.codeka.warworlds.Colony");

                StarManager.getInstance().requestStar(BuildActivity.this, starKey, false, BuildActivity.this);
                StarManager.getInstance().addStarUpdatedListener(starKey, BuildActivity.this);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    /**
     * Called when our star is refreshed/updated. We want to reload the current tab.
     */
    @Override
    public void onStarFetched(Star s) {
        if (mStar == null || mStar.getKey().equals(s.getKey())) {
            mStar = s;
            mColonies = new ArrayList<Colony>();
            MyEmpire myEmpire = EmpireManager.getInstance().getEmpire();
            for (Colony c : mStar.getColonies()) {
                if (c.getEmpireKey() != null && c.getEmpireKey().equals(myEmpire.getKey())) {
                    mColonies.add(c);
                }
            }
            Collections.sort(mColonies, new Comparator<Colony>() {
                @Override
                public int compare(Colony lhs, Colony rhs) {
                    return lhs.getPlanetIndex() - rhs.getPlanetIndex();
                }
            });

            if (mInitialColony != null) {
                int colonyIndex = 0;
                for (Colony colony : mColonies) {
                    if (colony.getKey().equals(mInitialColony.getKey())) {
                        break;
                    }
                    colonyIndex ++;
                }

                mViewPager.setCurrentItem(colonyIndex);
                mInitialColony = null;
            }

            mColonyPagerAdapter.notifyDataSetChanged();
        }
    }

    private void refreshColonyDetails(Colony colony) {
        ImageView planetIcon = (ImageView) findViewById(R.id.planet_icon);
        Planet planet = mStar.getPlanets()[colony.getPlanetIndex() - 1];
        Sprite planetSprite = PlanetImageManager.getInstance().getSprite(this, planet);
        planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));

        TextView planetName = (TextView) findViewById(R.id.planet_name);
        planetName.setText(String.format(Locale.ENGLISH, "%s %s",
                mStar.getName(), RomanNumeralFormatter.format(colony.getPlanetIndex())));

        TextView buildQueueDescription = (TextView) findViewById(R.id.build_queue_description);
        int buildQueueLength = 0;
        for (BuildRequest br : mStar.getBuildRequests()) {
            if (br.getColonyKey().equals(colony.getKey())) {
                buildQueueLength ++;
            }
        }
        if (buildQueueLength == 0) {
            buildQueueDescription.setText("Build queue: idle");
        } else {
            buildQueueDescription.setText(String.format(Locale.ENGLISH, "Build queue: %d", buildQueueLength));
        }
    }

    private String getDependenciesList(Colony colony, Design design) {
        return getDependenciesList(colony, design, 1);
    }

    /**
     * Returns the dependencies of the given design a string for display to
     * the user. Dependencies that we don't meet will be coloured red.
     */
    private String getDependenciesList(Colony colony, Design design, int level) {
        String required = "Required: ";
        List<Design.Dependency> dependencies;
        if (level == 1 || design.getDesignKind() != Design.DesignKind.BUILDING) {
            dependencies = design.getDependencies();
        } else {
            BuildingDesign bd = (BuildingDesign) design;
            BuildingDesign.Upgrade upgrade = bd.getUpgrades().get(level - 1);
            dependencies = upgrade.getDependencies();
        }

        if (dependencies == null || dependencies.size() == 0) {
            required += "none";
        } else {
            int n = 0;
            for (Design.Dependency dep : dependencies) {
                if (n > 0) {
                    required += ", ";
                }

                boolean dependencyMet = false;
                for (Building b : colony.getBuildings()) {
                    if (b.getDesign().getID().equals(dep.getDesignID())) {
                        // TODO: check level
                        dependencyMet = true;
                    }
                }

                Design dependentDesign = BuildingDesignManager.getInstance().getDesign(dep.getDesignID());
                required += "<font color=\""+(dependencyMet ? "green" : "red")+"\">";
                required += dependentDesign.getDisplayName();
                required += "</font>";
            }
        }

        return required;
    }

    public static class BuildFragment extends TabFragmentFragment
                                      implements StarManager.StarFetchedHandler {
        @Override
        protected void createTabs() {
            BuildActivity activity = (BuildActivity) getActivity();
            Bundle args = getArguments();

            getTabManager().addTab(activity, new TabInfo(this, "Buildings", BuildingsFragment.class, args));
            getTabManager().addTab(activity, new TabInfo(this, "Ships", ShipsFragment.class, args));
            getTabManager().addTab(activity, new TabInfo(this, "Queue", QueueFragment.class, args));
        }

        @Override
        public void onStarFetched(Star s) {
            getTabManager().reloadTab();
        }

        @Override
        public void onResume() {
            super.onResume();
            StarManager.getInstance().addStarUpdatedListener(null, this);
        }

        @Override
        public void onPause() {
            super.onPause();
            StarManager.getInstance().removeStarUpdatedListener(this);
        }
    }

    public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
        public ColonyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new BuildFragment();
            Bundle args = new Bundle();
            args.putParcelable("au.com.codeka.warworlds.Colony", mColonies.get(i));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            if (mColonies == null) {
                return 0;
            }

            return mColonies.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Colony " + (position + 1);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            refreshColonyDetails(mColonies.get(position));
        }
    }

    public static class BaseTabFragment extends Fragment {
        private Colony mColony;

        protected Colony getColony() {
            if (mColony == null) {
                Bundle args = getArguments();
                mColony = (Colony) args.getParcelable("au.com.codeka.warworlds.Colony");
            }
            return mColony;
        }
    }

    public static class BuildingsFragment extends BaseTabFragment {
        private BuildingListAdapter mBuildingListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_buildings_tab, container, false);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();

            mBuildingListAdapter = new BuildingListAdapter();
            if (colony != null) {
                mBuildingListAdapter.setColony(star, colony);
            }

            ListView buildingsList = (ListView) v.findViewById(R.id.building_list);
            buildingsList.setAdapter(mBuildingListAdapter);

            buildingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Entry entry = (Entry) mBuildingListAdapter.getItem(position);
                    int buildQueueSize = 0;
                    BuildActivity activity = (BuildActivity) getActivity();
                    for (BuildRequest br : activity.mStar.getBuildRequests()) {
                        if (br.getColonyKey().equals(colony.getKey())) {
                            buildQueueSize ++;
                        }
                    }

                    if (entry.design != null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.design, colony, buildQueueSize);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    } else if (entry.building != null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.building, colony, buildQueueSize);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    }
                }
            });

            return v;
        }

        /**
         * This adapter is used to populate a list of buildings in a list view.
         */
        private class BuildingListAdapter extends BaseAdapter {
            private ArrayList<Entry> mEntries;

            private static final int HEADING_TYPE = 0;
            private static final int EXISTING_BUILDING_TYPE = 1;
            private static final int NEW_BUILDING_TYPE = 2;

            public void setColony(Star star, Colony colony) {
                List<Building> buildings = colony.getBuildings();
                if (buildings == null) {
                    buildings = new ArrayList<Building>();
                }

                mEntries = new ArrayList<Entry>();
                for (Building b : buildings) {
                    Entry entry = new Entry();
                    entry.building = b;
                    // if the building is being upgraded (i.e. if there's a build request that
                    // references this building) then add the build request as well
                    for (BuildRequest br : star.getBuildRequests()) {
                        if (br.getExistingBuildingKey() != null && br.getExistingBuildingKey().equals(b.getKey())) {
                            entry.buildRequest = br;
                        }
                    }
                    mEntries.add(entry);
                }

                for (BuildRequest br : star.getBuildRequests()) {
                    if (br.getColonyKey().equals(colony.getKey()) &&
                        br.getBuildKind().equals(BuildRequest.BuildKind.BUILDING) &&
                        br.getExistingBuildingKey() == null) {
                        Entry entry = new Entry();
                        entry.buildRequest = br;
                        mEntries.add(entry);
                    }
                }

                Collections.sort(mEntries, new Comparator<Entry>() {
                    @Override
                    public int compare(Entry lhs, Entry rhs) {
                        String a = (lhs.building != null ? lhs.building.getDesignName() : lhs.buildRequest.getDesignID());
                        String b = (rhs.building != null ? rhs.building.getDesignName() : rhs.buildRequest.getDesignID());
                        return a.compareTo(b);
                    }
                });

                Entry title = new Entry();
                title.title = "Existing Buildings";
                mEntries.add(0, title);

                title = new Entry();
                title.title = "Available Buildings";
                mEntries.add(title);

                for (Design d : BuildingDesignManager.getInstance().getDesigns().values()) {
                    BuildingDesign bd = (BuildingDesign) d;
                    if (bd.getMaxPerColony() > 0) {
                        int numExisting = 0;
                        for (Entry e : mEntries) {
                            if (e.building != null) {
                                if (e.building.getDesignName().equals(bd.getID())) {
                                    numExisting ++;
                                }
                            } else if (e.buildRequest != null) {
                                if (e.buildRequest.getDesignID().equals(bd.getID())) {
                                    numExisting ++;
                                }
                            }
                        }
                        if (numExisting >= bd.getMaxPerColony()) {
                            continue;
                        }
                    }
                    if (bd.getMaxPerEmpire() > 0) {
                        int numExisting = BuildManager.getInstance().getTotalBuildingsInEmpire(bd.getID());
                        for (BuildRequest br : BuildManager.getInstance().getBuildRequests()) {
                            if (br.getDesignID().equals(bd.getID())) {
                                numExisting ++;
                            }
                        }
                        if (numExisting >= bd.getMaxPerEmpire()) {
                            continue;
                        }
                    }
                    Entry entry = new Entry();
                    entry.design = bd;
                    mEntries.add(entry);
                }

                notifyDataSetChanged();
            }

            /**
             * We have three types of items, the "headings", the list of existing buildings
             * and the list of building designs.
             */
            @Override
            public int getViewTypeCount() {
                return 3;
            }

            @Override
            public int getItemViewType(int position) {
                if (mEntries == null)
                    return 0;

                if (mEntries.get(position).title != null)
                    return HEADING_TYPE;
                if (mEntries.get(position).design != null)
                    return NEW_BUILDING_TYPE;
                return EXISTING_BUILDING_TYPE;
            }

            @Override
            public boolean isEnabled(int position) {
                if (getItemViewType(position) == HEADING_TYPE) {
                    return false;
                }

                // also, if it's an existing building that's at the max level it can't be
                // upgraded any more, so also disabled.
                Entry entry = mEntries.get(position);
                if (entry.building != null) {
                    int maxUpgrades = entry.building.getDesign().getUpgrades().size();
                    if (entry.building.getLevel() > maxUpgrades) {
                        return false;
                    }
                }

                return true;
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
                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);

                    int viewType = getItemViewType(position);
                    if (viewType == HEADING_TYPE) {
                        view = new TextView(getActivity());
                    } else {
                        view = inflater.inflate(R.layout.solarsystem_buildings_design, parent, false);
                    }
                }

                Entry entry = mEntries.get(position);
                if (entry.title != null) {
                    TextView tv = (TextView) view;
                    tv.setText(entry.title);
                } else if (entry.building != null || entry.buildRequest != null) {
                    // existing building/upgrading building
                    ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                    TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                    TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                    TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                    TextView level = (TextView) view.findViewById(R.id.building_level);
                    TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);
                    ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);

                    Building building = entry.building;
                    BuildRequest buildRequest = entry.buildRequest;
                    BuildingDesign design = (building != null
                            ? building.getDesign()
                            : BuildingDesignManager.getInstance().getDesign(buildRequest.getDesignID()));

                    icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

                    int numUpgrades = design.getUpgrades().size();

                    if (numUpgrades == 0 || building == null) {
                        level.setVisibility(View.GONE);
                        levelLabel.setVisibility(View.GONE);
                    } else {
                        level.setText(Integer.toString(building.getLevel()));
                        level.setVisibility(View.VISIBLE);
                        levelLabel.setVisibility(View.VISIBLE);
                    }

                    row1.setText(design.getDisplayName());
                    if (buildRequest != null) {
                        String verb = (building == null ? "Building" : "Upgrading");
                        row2.setText(Html.fromHtml(String.format(Locale.ENGLISH,
                                "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
                                verb, (int) buildRequest.getPercentComplete(),
                                 TimeInHours.format(buildRequest.getRemainingTime()))));

                        row3.setVisibility(View.GONE);
                        progress.setVisibility(View.VISIBLE);
                        progress.setProgress((int) buildRequest.getPercentComplete());
                    } else {
                        if (numUpgrades < building.getLevel()) {
                            row2.setText("No more upgrades");
                            row3.setVisibility(View.GONE);
                            progress.setVisibility(View.GONE);
                        } else {
                            progress.setVisibility(View.GONE);
                            row2.setText(String.format(Locale.ENGLISH,
                                    "Upgrade: %.2f hours",
                                    (float) design.getBuildCost().getTimeInSeconds() / 3600.0f));

                            String required = ((BuildActivity) getActivity()).getDependenciesList(
                                    getColony(), design, building.getLevel());
                            row3.setVisibility(View.VISIBLE);
                            row3.setText(Html.fromHtml(required));
                        }
                    }
                } else {
                    // new building
                    ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                    TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                    TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                    TextView row3 = (TextView) view.findViewById(R.id.building_row3);

                    view.findViewById(R.id.building_progress).setVisibility(View.GONE);
                    view.findViewById(R.id.building_level).setVisibility(View.GONE);
                    view.findViewById(R.id.building_level_label).setVisibility(View.GONE);

                    BuildingDesign design = mEntries.get(position).design;

                    icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

                    row1.setText(design.getDisplayName());
                    row2.setText(String.format("%.2f hours",
                            (float) design.getBuildCost().getTimeInSeconds() / 3600.0f));

                    String required = ((BuildActivity) getActivity()).getDependenciesList(getColony(), design);
                    row3.setText(required);
                }

                return view;
            }
        }

        private static class Entry {
            public String title;
            public BuildRequest buildRequest;
            public Building building;
            public BuildingDesign design;
        }
    }

    public static class ShipsFragment extends BaseTabFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_ships_tab, container, false);

            final Colony colony = getColony();

            final ShipDesignListAdapter adapter = new ShipDesignListAdapter();
            adapter.setDesigns(ShipDesignManager.getInstance().getDesigns());

            ListView availableDesignsList = (ListView) v.findViewById(R.id.ship_list);
            availableDesignsList.setAdapter(adapter);
            availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int buildQueueSize = 0;
                    BuildActivity activity = (BuildActivity) getActivity();
                    for (BuildRequest br : activity.mStar.getBuildRequests()) {
                        if (br.getColonyKey().equals(colony.getKey())) {
                            buildQueueSize ++;
                        }
                    }
                    ShipDesign design = (ShipDesign) adapter.getItem(position);
                    if (design == null) {
                        // not sure why this would ever happen?
                        return;
                    }

                    BuildConfirmDialog dialog = new BuildConfirmDialog();
                    dialog.setup(design, colony, buildQueueSize);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            return v;
        }

        /**
         * This adapter is used to populate the list of ship designs in our view.
         */
        private class ShipDesignListAdapter extends BaseAdapter {
            private List<ShipDesign> mDesigns;

            public void setDesigns(Map<String, Design> designs) {
                mDesigns = new ArrayList<ShipDesign>();
                for (Design d : designs.values()) {
                    mDesigns.add((ShipDesign) d);
                }
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                if (mDesigns == null)
                    return 0;
                return mDesigns.size();
            }

            @Override
            public Object getItem(int position) {
                if (mDesigns == null)
                    return null;
                return mDesigns.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, parent, false);
                }

                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                view.findViewById(R.id.building_progress).setVisibility(View.GONE);
                view.findViewById(R.id.building_level).setVisibility(View.GONE);
                view.findViewById(R.id.building_level_label).setVisibility(View.GONE);

                ShipDesign design = mDesigns.get(position);

                icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

                row1.setText(design.getDisplayName());
                row2.setText(String.format("%.2f hours",
                        (float) design.getBuildCost().getTimeInSeconds() / 3600.0f));

                String required = ((BuildActivity) getActivity()).getDependenciesList(getColony(), design);
                row3.setText(Html.fromHtml(required));

                return view;
            }
        }
    }

    public static class QueueFragment extends BaseTabFragment implements TabManager.Reloadable {
        private BuildQueueList mBuildQueueList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_queue_tab, container, false);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();
            if (star == null)
                return inflater.inflate(R.layout.solarsystem_build_loading_tab, container, false);

            mBuildQueueList = (BuildQueueList) v.findViewById(R.id.build_queue);
            mBuildQueueList.setShowStars(false);
            mBuildQueueList.refresh(star, colony);

            mBuildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
                @Override
                public void onAccelerateClick(StarSummary star, BuildRequest buildRequest) {
                    BuildAccelerateDialog dialog = new BuildAccelerateDialog();
                    dialog.setBuildRequest(star, buildRequest);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }

                @Override
                public void onStopClick(StarSummary star, BuildRequest buildRequest) {
                    BuildStopConfirmDialog dialog = new BuildStopConfirmDialog();
                    dialog.setBuildRequest(star, buildRequest);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            return v;
        }

        @Override
        public void reloadTab() {
            if (mBuildQueueList != null) {
                mBuildQueueList.refreshSelection();
            }
        }

    }
}
