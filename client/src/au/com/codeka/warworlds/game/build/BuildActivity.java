package au.com.codeka.warworlds.game.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.TabFragmentFragment;
import au.com.codeka.warworlds.TabManager;
import au.com.codeka.warworlds.ctrl.BuildQueueList;
import au.com.codeka.warworlds.ctrl.BuildingsList;
import au.com.codeka.warworlds.ctrl.FleetListRow;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.NotesDialog;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * When you click "Build" shows you the list of buildings/ships that are/can be built by your
 * colony. You can swipe left/right to switch between your colonies in this star.
 */
public class BuildActivity extends BaseActivity {
    private Star mStar;
    private List<Colony> mColonies;
    private ViewPager mViewPager;
    private ColonyPagerAdapter mColonyPagerAdapter;
    private Colony mInitialColony;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.build);

        mColonyPagerAdapter = new ColonyPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mColonyPagerAdapter);

        if (savedInstanceState != null) {
            Star s = null;

            try {
                byte[] bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Star");
                Messages.Star star_pb = Messages.Star.parseFrom(bytes);
                s = new Star();
                s.fromProtocolBuffer(star_pb);

                bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.CurrentColony");
                Messages.Colony colony_pb = Messages.Colony.parseFrom(bytes);
                mInitialColony = new Colony();
                mInitialColony.fromProtocolBuffer(colony_pb);
            } catch (InvalidProtocolBufferException e) {
            }

            updateStar(s);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
                byte[] colonyBytes = extras.getByteArray("au.com.codeka.warworlds.Colony");
                try {
                    Messages.Colony colony_pb = Messages.Colony.parseFrom(colonyBytes);
                    mInitialColony = new Colony();
                    mInitialColony.fromProtocolBuffer(colony_pb);
                } catch (InvalidProtocolBufferException e) {
                }

                StarManager.eventBus.register(mEventHandler);
                Star star = StarManager.i.getStar(Integer.parseInt(starKey));
                if (star != null) {
                    updateStar(star);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        StarManager.eventBus.unregister(mEventHandler);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mStar != null) {
            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            mStar.toProtocolBuffer(star_pb);
            state.putByteArray("au.com.codeka.warworlds.Star", star_pb.build().toByteArray());
        }

        Colony currentColony = mColonies.get(mViewPager.getCurrentItem());
        if (currentColony != null) {
            Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
            currentColony.toProtocolBuffer(colony_pb);
            state.putByteArray("au.com.codeka.warworlds.CurrentColony", colony_pb.build().toByteArray());
        }
    }

    private void refreshColonyDetails(Colony colony) {
        ImageView planetIcon = (ImageView) findViewById(R.id.planet_icon);
        Planet planet = (Planet) mStar.getPlanets()[colony.getPlanetIndex() - 1];
        Sprite planetSprite = PlanetImageManager.getInstance().getSprite(planet);
        planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));

        TextView planetName = (TextView) findViewById(R.id.planet_name);
        planetName.setText(String.format(Locale.ENGLISH, "%s %s",
                mStar.getName(), RomanNumeralFormatter.format(colony.getPlanetIndex())));

        TextView buildQueueDescription = (TextView) findViewById(R.id.build_queue_description);
        int buildQueueLength = 0;
        for (BaseBuildRequest br : mStar.getBuildRequests()) {
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

    public Object mEventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            if (mStar == null || mStar.getKey().equals(star.getKey())) {
                updateStar(star);
            }
        }
    };

    private void updateStar(Star star) {
        boolean dataSetChanged = (mStar == null);

        mStar = star;
        mColonies = new ArrayList<Colony>();
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        for (BaseColony c : mStar.getColonies()) {
            if (c.getEmpireKey() != null && c.getEmpireKey().equals(myEmpire.getKey())) {
                mColonies.add((Colony) c);
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

        if (dataSetChanged) {
            mColonyPagerAdapter.notifyDataSetChanged();
        }
    }

    public static class BuildFragment extends TabFragmentFragment {
        @Override
        protected void createTabs() {
            BuildActivity activity = (BuildActivity) getActivity();
            Bundle args = getArguments();

            getTabManager().addTab(activity, new TabInfo(this, "Buildings", BuildingsFragment.class, args));
            getTabManager().addTab(activity, new TabInfo(this, "Ships", ShipsFragment.class, args));
            getTabManager().addTab(activity, new TabInfo(this, "Queue", QueueFragment.class, args));
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
            args.putString("au.com.codeka.warworlds.ColonyKey", mColonies.get(i).getKey());
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
            if (mColonies != null && mColonies.size() > position) {
                refreshColonyDetails(mColonies.get(position));
            }
        }
    }

    public static class BaseTabFragment extends Fragment {
        private String mColonyKey;

        protected Colony getColony() {
            if (mColonyKey == null) {
                Bundle args = getArguments();
                mColonyKey = args.getString("au.com.codeka.warworlds.ColonyKey");
            }

            Star star = ((BuildActivity) getActivity()).mStar;
            if (star.getColonies() == null) {
                return null;
            }

            for (BaseColony baseColony : star.getColonies()) {
                if (baseColony.getKey().equals(mColonyKey)) {
                    return (Colony) baseColony;
                }
            }

            return null;
        }
    }

    public static class BuildingsFragment extends BaseTabFragment {
        private BuildingsList mBuildingsList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.build_buildings_tab, container, false);
            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();

            mBuildingsList = (BuildingsList) v.findViewById(R.id.building_list);
            if (colony != null) {
                mBuildingsList.setColony(star, colony);
            }

            mBuildingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BuildingsList.Entry entry = mBuildingsList.getItem(position);
                    if (entry.design != null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.design, star, colony);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    } else if (entry.building != null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.building, star, colony);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    }
                }
            });

            return v;
        }
    }

    public static class ShipsFragment extends BaseTabFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.build_ships_tab, container, false);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();

            ArrayList<Fleet> fleets = new ArrayList<Fleet>();
            for (BaseFleet baseFleet : star.getFleets()) {
                if (baseFleet.getEmpireKey() != null &&
                        baseFleet.getEmpireKey().equals(EmpireManager.i.getEmpire().getKey())) {
                    fleets.add((Fleet) baseFleet);
                }
            }

            ArrayList<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
            for (BaseBuildRequest baseBuildRequest : star.getBuildRequests()) {
                if (baseBuildRequest.getEmpireKey().equals(EmpireManager.i.getEmpire().getKey()) &&
                        baseBuildRequest.getDesignKind() == DesignKind.SHIP) {
                    buildRequests.add((BuildRequest) baseBuildRequest);
                }
            }

            final ShipListAdapter adapter = new ShipListAdapter();
            adapter.setShips(DesignManager.i.getDesigns(DesignKind.SHIP), fleets, buildRequests);

            final ListView availableDesignsList = (ListView) v.findViewById(R.id.ship_list);
            availableDesignsList.setAdapter(adapter);
            availableDesignsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ShipListAdapter.ItemEntry entry = (ShipListAdapter.ItemEntry) adapter.getItem(position);
                    if (entry.fleet == null && entry.buildRequest == null) {
                        BuildConfirmDialog dialog = new BuildConfirmDialog();
                        dialog.setup(entry.design, star, colony);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    } else if (entry.fleet != null && entry.buildRequest == null) {
                        ShipUpgradeDialog dialog = new ShipUpgradeDialog();
                        dialog.setup(star, colony, entry.fleet);
                        dialog.show(getActivity().getSupportFragmentManager(), "");
                    }
                }
            });

            availableDesignsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                    final ShipListAdapter.ItemEntry entry = (ShipListAdapter.ItemEntry) adapter.getItem(position);

                    NotesDialog dialog = new NotesDialog();
                    dialog.setup(entry.fleet == null ? entry.buildRequest.getNotes()
                                                     : entry.fleet.getNotes(), new NotesDialog.NotesChangedHandler() {
                        @Override
                        public void onNotesChanged(String notes) {
                            if (entry.fleet != null) {
                                entry.fleet.setNotes(notes);
                            } else if (entry.buildRequest != null) {
                                entry.buildRequest.setNotes(notes);
                            }
                            adapter.notifyDataSetChanged();

                            if (entry.fleet != null) {
                                FleetManager.i.updateNotes(entry.fleet);
                            } else {
                                BuildManager.getInstance().updateNotes(entry.buildRequest.getKey(), notes);
                            }
                        }
                    });

                    dialog.show(getActivity().getSupportFragmentManager(), "");
                    return true;
                }
            });

            return v;
        }

        /** This adapter is used to populate the list of ship designs in our view. */
        private class ShipListAdapter extends BaseAdapter {
            private List<ItemEntry> mEntries;

            private static final int HEADING_TYPE = 0;
            private static final int EXISTING_SHIP_TYPE = 1;
            private static final int NEW_SHIP_TYPE = 2;

            public void setShips(Map<String, Design> designs, ArrayList<Fleet> fleets, ArrayList<BuildRequest> buildRequests) {
                mEntries = new ArrayList<ItemEntry>();

                mEntries.add(new ItemEntry("New Ships"));
                for (Design design : designs.values()) {
                    mEntries.add(new ItemEntry((ShipDesign) design));
                }

                mEntries.add(new ItemEntry("Existing Ships"));
                for (Fleet fleet : fleets) {
                    if (fleet.getState() != Fleet.State.IDLE) {
                        continue;
                    }
                    ItemEntry entry = new ItemEntry(fleet);
                    for (BuildRequest buildRequest : buildRequests) {
                        if (buildRequest.getExistingFleetID() != null &&
                                ((int) buildRequest.getExistingFleetID()) == Integer.parseInt(fleet.getKey())) {
                            entry.buildRequest = buildRequest;
                        }
                    }
                    mEntries.add(entry);
                }
                for (BuildRequest buildRequest : buildRequests) {
                    if (buildRequest.getExistingFleetID() != null) {
                        continue;
                    }
                    mEntries.add(new ItemEntry(buildRequest));
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
            public boolean isEnabled(int position) {
                if (getItemViewType(position) == HEADING_TYPE) {
                    return false;
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
            public int getItemViewType(int position) {
                if (mEntries == null)
                    return 0;

                if (mEntries.get(position).heading != null)
                    return HEADING_TYPE;
                if (mEntries.get(position).design != null)
                    return NEW_SHIP_TYPE;
                return EXISTING_SHIP_TYPE;
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
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    if (entry.heading != null) {
                        view = new TextView(getActivity());
                    } else {
                        view = inflater.inflate(R.layout.buildings_design, parent, false);
                    }
                }

                if (entry.heading != null) {
                    TextView tv = (TextView) view;
                    tv.setTypeface(Typeface.DEFAULT_BOLD);
                    tv.setText(entry.heading);
                } else if (entry.fleet != null || entry.buildRequest != null) {
                    // existing fleet/upgrading fleet
                    ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                    LinearLayout row1 = (LinearLayout) view.findViewById(R.id.building_row1);
                    TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                    TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                    TextView level = (TextView) view.findViewById(R.id.building_level);
                    TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);
                    ProgressBar progress = (ProgressBar) view.findViewById(R.id.building_progress);
                    TextView notes = (TextView) view.findViewById(R.id.notes);

                    Fleet fleet = entry.fleet;
                    BuildRequest buildRequest = entry.buildRequest;
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP,
                            (fleet != null ? fleet.getDesignID() : buildRequest.getDesignID()));

                    icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                    int numUpgrades = 0;//design.getUpgrades().size();

                    if (numUpgrades == 0 || fleet == null) {
                        level.setVisibility(View.GONE);
                        levelLabel.setVisibility(View.GONE);
                    } else {
                        // TODO
                        level.setText("?");
                        level.setVisibility(View.VISIBLE);
                        levelLabel.setVisibility(View.VISIBLE);
                    }

                    row1.removeAllViews();
                    FleetListRow.populateFleetNameRow(getActivity(), row1, fleet, design);
                    if (buildRequest != null) {
                        String verb = (fleet == null ? "Building" : "Upgrading");
                        row2.setText(Html.fromHtml(String.format(Locale.ENGLISH,
                                "<font color=\"#0c6476\">%s:</font> %d %%, %s left",
                                verb, (int) buildRequest.getPercentComplete(),
                                TimeFormatter.create().format(buildRequest.getRemainingTime()))));

                        row3.setVisibility(View.GONE);
                        progress.setVisibility(View.VISIBLE);
                        progress.setProgress((int) buildRequest.getPercentComplete());
                    } else {
                        String upgrades = "";
                        for (ShipDesign.Upgrade upgrade : design.getUpgrades()) {
                            if (fleet != null && !fleet.hasUpgrade(upgrade.getID())) {
                                if (upgrades.length() > 0) {
                                    upgrades += ", ";
                                }
                                upgrades += upgrade.getDisplayName();
                            }
                        }

                        progress.setVisibility(View.GONE);
                        if (upgrades.length() == 0) {
                            row2.setText(Html.fromHtml(String.format(Locale.ENGLISH, "Upgrades: <i>none</i>")));
                        } else {
                            row2.setText(String.format(Locale.ENGLISH, "Upgrades: " + upgrades));
                        }

                        String requiredHtml = design.getDependenciesHtml(getColony());
                        row3.setVisibility(View.VISIBLE);
                        row3.setText(Html.fromHtml(requiredHtml));
                    }

                    if (fleet != null && fleet.getNotes() != null) {
                        notes.setText(fleet.getNotes());
                        notes.setVisibility(View.VISIBLE);
                    } else if (buildRequest != null && buildRequest.getNotes() != null) {
                        notes.setText(buildRequest.getNotes());
                        notes.setVisibility(View.VISIBLE);
                    } else {
                        notes.setText("");
                        notes.setVisibility(View.GONE);
                    }
                } else {
                    // new fleet
                    ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                    LinearLayout row1 = (LinearLayout) view.findViewById(R.id.building_row1);
                    TextView row2 = (TextView) view.findViewById(R.id.building_row2);
                    TextView row3 = (TextView) view.findViewById(R.id.building_row3);

                    view.findViewById(R.id.building_progress).setVisibility(View.GONE);
                    view.findViewById(R.id.building_level).setVisibility(View.GONE);
                    view.findViewById(R.id.building_level_label).setVisibility(View.GONE);
                    view.findViewById(R.id.notes).setVisibility(View.GONE);

                    ShipDesign design = entry.design;

                    icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                    row1.removeAllViews();
                    FleetListRow.populateFleetNameRow(getActivity(), row1, null, design);
                    String requiredHtml = design.getDependenciesHtml(getColony());
                    row2.setText(Html.fromHtml(requiredHtml));

                    row3.setVisibility(View.GONE);
                }

                return view;
            }

            public class ItemEntry {
                public ShipDesign design;
                public Fleet fleet;
                public BuildRequest buildRequest;
                public String heading;

                public ItemEntry(ShipDesign design) {
                    this.design = design;
                }
                public ItemEntry(BuildRequest buildRequest) {
                    this.buildRequest = buildRequest;
                }
                public ItemEntry(Fleet fleet) {
                    this.fleet = fleet;
                }
                public ItemEntry(String heading) {
                    this.heading = heading;
                }
            }
        }
    }

    public static class QueueFragment extends BaseTabFragment implements TabManager.Reloadable {
        private BuildQueueList mBuildQueueList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.build_queue_tab, container, false);

            final Star star = ((BuildActivity) getActivity()).mStar;
            final Colony colony = getColony();
            if (star == null)
                return inflater.inflate(R.layout.build_loading_tab, container, false);

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
