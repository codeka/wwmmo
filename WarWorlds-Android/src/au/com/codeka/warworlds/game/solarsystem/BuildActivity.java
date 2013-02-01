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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.TabFragmentActivity;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.game.BuildAccelerateConfirmDialog;
import au.com.codeka.warworlds.game.BuildStopConfirmDialog;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.BuildingDesign;
import au.com.codeka.warworlds.model.BuildingDesignManager;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * When you click "Build" shows you the list of buildings/ships that are/can be built by your
 * colony.
 */
public class BuildActivity extends TabFragmentActivity implements StarManager.StarFetchedHandler {
    private Context mContext = this;
    private Star mStar;
    private Colony mColony;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getTabManager().addTab(mContext, new TabInfo("Buildings", BuildingsFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Ships", ShipsFragment.class, null));
        getTabManager().addTab(mContext, new TabInfo("Queue", QueueFragment.class, null));
    }

    @Override
    public void onResume() {
        Bundle extras = this.getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
        mColony = (Colony) extras.getParcelable("au.com.codeka.warworlds.Colony");

        StarManager.getInstance().requestStar(mContext, starKey, false, this);
        StarManager.getInstance().addStarUpdatedListener(starKey, this);

        super.onResume();
    }

    @Override
    public void onPause() {
        StarManager.getInstance().removeStarUpdatedListener(this);

        super.onPause();
    }

    /**
     * Called when our star is refreshed/updated. We want to reload the current tab.
     */
    @Override
    public void onStarFetched(Star s) {
        if (mStar == null || mStar.getKey().equals(s.getKey())) {
            mStar = s;
            if (mColony != null) {
                for (Colony colony : mStar.getColonies()) {
                    if (colony.getKey().equals(mColony.getKey())) {
                        mColony = colony;
                    }
                }
            }

            getTabManager().reloadTab();
        }
    }

    private String getDependenciesList(Design design) {
        return getDependenciesList(design, 1);
    }

    /**
     * Returns the dependencies of the given design a string for display to
     * the user. Dependencies that we don't meet will be coloured red.
     */
    private String getDependenciesList(Design design, int level) {
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
                for (Building b : mColony.getBuildings()) {
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

    public static class BuildingsFragment extends Fragment {
        private BuildingListAdapter mBuildingListAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_buildings_tab, null);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = ((BuildActivity) getActivity()).mColony;

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
                        view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
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

                            String required = ((BuildActivity) getActivity()).getDependenciesList(design, building.getLevel());
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

                    String required = ((BuildActivity) getActivity()).getDependenciesList(design);
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

    public static class ShipsFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_ships_tab, null);

            final Colony colony = ((BuildActivity) getActivity()).mColony;

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
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
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

                String required = ((BuildActivity) getActivity()).getDependenciesList(design);
                row3.setText(Html.fromHtml(required));

                return view;
            }
        }
    }

    public static class QueueFragment extends Fragment implements TabManager.Reloadable {
        private BuildQueueList mBuildQueueList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.solarsystem_build_queue_tab, null);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = ((BuildActivity) getActivity()).mColony;
            if (star == null)
                return inflater.inflate(R.layout.solarsystem_build_loading_tab, null);

            mBuildQueueList = (BuildQueueList) v.findViewById(R.id.build_queue);
            mBuildQueueList.setShowStars(false);
            mBuildQueueList.refresh(star, colony);

            mBuildQueueList.setBuildQueueActionListener(new BuildQueueList.BuildQueueActionListener() {
                @Override
                public void onAccelerateClick(Star star, BuildRequest buildRequest) {
                    BuildAccelerateConfirmDialog dialog = new BuildAccelerateConfirmDialog();
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

        @Override
        public void reloadTab() {
            if (mBuildQueueList != null) {
                mBuildQueueList.refreshSelection();
            }
        }

    }
}
