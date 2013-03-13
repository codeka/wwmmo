package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.Duration;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Design;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

public class BuildQueueList extends FrameLayout implements MyEmpire.RefreshAllCompleteHandler {
    private Context mContext;
    private BuildQueueActionListener mActionListener;
    private BuildQueueListAdapter mBuildQueueListAdapter;
    private Colony mColony;
    private List<String> mStarKeys;
    private Handler mHandler;
    private ProgressUpdater mProgressUpdater;
    private BuildRequest mSelectedBuildRequest;
    private boolean mShowStars;

    public BuildQueueList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mShowStars = true;

        View child = inflate(context, R.layout.buildqueue_list_ctrl, null);
        this.addView(child);

        mBuildQueueListAdapter = new BuildQueueListAdapter();
        ListView buildQueueList = (ListView) findViewById(R.id.build_queue_list);
        buildQueueList.setAdapter(mBuildQueueListAdapter);

        buildQueueList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BuildQueueListAdapter.ItemEntry entry = (BuildQueueListAdapter.ItemEntry) mBuildQueueListAdapter.getItem(position);
                if (entry.buildRequest != null) {
                    mSelectedBuildRequest = entry.buildRequest;
                    mBuildQueueListAdapter.notifyDataSetChanged();
                    refreshSelection();
                }
            }
        });

        Button stopBtn = (Button) findViewById(R.id.stop_btn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null && mSelectedBuildRequest != null) {
                    mActionListener.onStopClick(mBuildQueueListAdapter.getStarForBuildRequest(mSelectedBuildRequest), mSelectedBuildRequest);
                }
            }
        });

        Button accelerateBtn = (Button) findViewById(R.id.accelerate_btn);
        accelerateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null && mSelectedBuildRequest != null) {
                    mActionListener.onAccelerateClick(mBuildQueueListAdapter.getStarForBuildRequest(mSelectedBuildRequest), mSelectedBuildRequest);
                }
            }
        });

        refreshSelection();

        mHandler = new Handler();
    }

    public void setShowStars(boolean showStars) {
        mShowStars = showStars;
        if (mBuildQueueListAdapter != null) {
            mBuildQueueListAdapter.notifyDataSetChanged();
        }
    }

    public void setBuildQueueActionListener(BuildQueueActionListener listener) {
        mActionListener = listener;
    }

    public void refresh(final Star star, final Colony colony) {
        refresh(star, colony, star.getBuildRequests());
    }

    public void refresh(final Star star, final Colony colony, List<BuildRequest> allBuildRequests) {
        Map<String, Star> stars = new TreeMap<String, Star>();
        stars.put(star.getKey(), star);

        Map<String, Colony> colonies = new TreeMap<String, Colony>();
        colonies.put(colony.getKey(), colony);

        List<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
        for (BuildRequest request : allBuildRequests) {
            if (request.getColonyKey().equals(colony.getKey())) {
                buildRequests.add(request);
            }
        }

        mColony = colony;
        refresh(stars, colonies, buildRequests);
    }

    public void refresh(final Map<String, Star> stars,
                        final Map<String, Colony> colonies,
                        final List<BuildRequest> buildRequests) {
        // save the list of star keys we're interested in here
        mStarKeys = new ArrayList<String>();
        for (Star star : stars.values()) {
            mStarKeys.add(star.getKey());
        }

        mBuildQueueListAdapter.setBuildQueue(stars, colonies, buildRequests);
    }

    public void refreshSelection() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.bottom_pane).findViewById(R.id.building_progress);
        TextView progressText = (TextView) findViewById(R.id.bottom_pane).findViewById(R.id.progress_text);
        ImageView icon = (ImageView) findViewById(R.id.bottom_pane).findViewById(R.id.building_icon);
        TextView buildingName = (TextView) findViewById(R.id.bottom_pane).findViewById(R.id.building_name);

        if (mSelectedBuildRequest == null) {
            findViewById(R.id.stop_btn).setEnabled(false);
            findViewById(R.id.accelerate_btn).setEnabled(false);
            buildingName.setVisibility(View.GONE);
            icon.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.stop_btn).setEnabled(true);
        findViewById(R.id.accelerate_btn).setEnabled(true);
        buildingName.setVisibility(View.VISIBLE);
        icon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        DesignManager dm = DesignManager.getInstance(mSelectedBuildRequest.getBuildKind());
        Design design = dm.getDesign(mSelectedBuildRequest.getDesignID());

        icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

        if (mSelectedBuildRequest.getCount() == 1) {
            buildingName.setText(design.getDisplayName());
        } else {
            buildingName.setText(String.format(Locale.ENGLISH, "%d × %s",
                    mSelectedBuildRequest.getCount(), design.getDisplayName(mSelectedBuildRequest.getCount() > 1)));
        }

        mBuildQueueListAdapter.refreshEntryProgress(mSelectedBuildRequest, progressBar, progressText);
    }

    /**
     * When the empire is updated, make sure we display the latest build queue.
     */
    @Override
    public void onRefreshAllComplete(MyEmpire empire) {
        String selectedBuildRequestKey = null;
        if (mSelectedBuildRequest != null) {
            selectedBuildRequestKey = mSelectedBuildRequest.getKey();
        }

        if (mColony != null) {
            String colonyKey = mColony.getKey();
            mColony = null;

            for (int i = 0; i < empire.getAllColonies().size(); i++) {
                Colony colony = empire.getAllColonies().get(i);
                if (colony.getKey().equals(colonyKey)) {
                    mColony = colony;
                    break;
                }
            }

            if (mColony != null) {
                refresh(empire.getImportantStar(mColony.getStarKey()),
                        mColony, empire.getAllBuildRequests());
            }
        } else {
            Map<String, Colony> colonies = new TreeMap<String, Colony>();
            for (Colony colony : empire.getAllColonies()) {
                colonies.put(colony.getKey(), colony);
            }

            refresh(empire.getImportantStars(), colonies, empire.getAllBuildRequests());
        }

        mSelectedBuildRequest = null;
        if (selectedBuildRequestKey != null) {
            for (BuildRequest buildRequest : empire.getAllBuildRequests()) {
                if (buildRequest.getKey().equals(selectedBuildRequestKey)) {
                    mSelectedBuildRequest = buildRequest;
                    break;
                }
            }
        }
        refreshSelection();
    }

    @Override
    public void onAttachedToWindow() {
        EmpireManager.getInstance().getEmpire().addRefreshAllCompleteHandler(this);

        mProgressUpdater = new ProgressUpdater();
        mHandler.postDelayed(mProgressUpdater, 5000);
    }

    @Override
    public void onDetachedFromWindow() {
        EmpireManager.getInstance().getEmpire().removeRefreshAllCompleteHandler(this);

        mHandler.removeCallbacks(mProgressUpdater);
        mProgressUpdater = null;
    }

    /**
     * This adapter is used to populate the list of buildings that are currently in progress.
     */
    private class BuildQueueListAdapter extends BaseAdapter {
        private List<BuildRequest> mBuildRequests;
        private Map<String, Star> mStars;
        private Map<String, Colony> mColonies;
        private List<ItemEntry> mEntries;

        public void setBuildQueue(Map<String, Star> stars,
                                  Map<String, Colony> colonies,
                                  List<BuildRequest> buildRequests) {
            mBuildRequests = new ArrayList<BuildRequest>(buildRequests);
            mStars = stars;
            mColonies = colonies;

            Collections.sort(mBuildRequests, new Comparator<BuildRequest>() {
                @Override
                public int compare(BuildRequest lhs, BuildRequest rhs) {
                    // sort by star, then by design, then by count
                    if (!lhs.getColonyKey().equals(rhs.getColonyKey())) {
                        Colony lhsColony = mColonies.get(lhs.getColonyKey());
                        Colony rhsColony = mColonies.get(rhs.getColonyKey());

                        if (lhsColony == null) {
                            return -1;
                        } else if (rhsColony == null) {
                            return 1;
                        }

                        if (!lhsColony.getStarKey().equals(rhsColony.getStarKey())) {
                            Star lhsStar = mStars.get(lhsColony.getStarKey());
                            Star rhsStar = mStars.get(rhsColony.getStarKey());

                            if (lhsStar == null) {
                                return -1;
                            } else if (rhsStar == null) {
                                return 1;
                            }

                            return lhsStar.getName().compareTo(rhsStar.getName());
                        } else {
                            return lhsColony.getPlanetIndex() - rhsColony.getPlanetIndex();
                        }
                    } else {
                        return lhs.getStartTime().compareTo(rhs.getStartTime());
                    }
                }
            });

            mEntries = new ArrayList<ItemEntry>();
            String lastStarKey = "";
            int lastPlanetIndex = -1;
            for (BuildRequest buildRequest : mBuildRequests) {
                Colony colony = mColonies.get(buildRequest.getColonyKey());
                if (colony == null) {
                    continue;
                }
                Star star = mStars.get(colony.getStarKey());
                if (star == null) {
                    continue;
                }

                if (!colony.getStarKey().equals(lastStarKey) || colony.getPlanetIndex() != lastPlanetIndex) {
                    if (mShowStars) {
                        ItemEntry entry = new ItemEntry();
                        entry.star = star;
                        entry.planet = star.getPlanets()[colony.getPlanetIndex() - 1];
                        mEntries.add(entry);
                    }
                    lastStarKey = colony.getStarKey();
                    lastPlanetIndex = colony.getPlanetIndex();
                }

                ItemEntry entry = new ItemEntry();
                entry.buildRequest = buildRequest;

                if (buildRequest.getExistingBuildingKey() != null) {
                    for (Building building : colony.getBuildings()) {
                        if (building.getKey().equals(buildRequest.getExistingBuildingKey())) {
                            entry.existingBuilding = building;
                        }
                    }
                }

                mEntries.add(entry);
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

            return mEntries.get(position).buildRequest == null ? 0 : 1;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mEntries.get(position).buildRequest == null) {
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
        public Object getItem(int position) {
            if (mEntries == null)
                return null;
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void refreshProgress() {
            if (mEntries == null) {
                return;
            }

            for (ItemEntry entry : mEntries) {
                if (entry.buildRequest != null) {
                    refreshEntryProgress(entry);
                }
            }
        }

        public Star getStarForBuildRequest(BuildRequest buildRequest) {
            Colony colony = mColonies.get(buildRequest.getColonyKey());
            return mStars.get(colony.getStarKey());
        }

        public void refreshEntryProgress(ItemEntry entry) {
            if (entry.progressBar == null || entry.progressText == null) {
                return;
            }
            if (entry.progressBar.getTag() != entry || entry.progressText.getTag() != entry) {
                return;
            }

            refreshEntryProgress(entry.buildRequest, entry.progressBar, entry.progressText);
        }

        public void refreshEntryProgress(BuildRequest buildRequest,
                                         ProgressBar progressBar,
                                         TextView progressText) {
            String prefix = String.format(Locale.ENGLISH, "<font color=\"#0c6476\">%s:</font> ",
                    buildRequest.getExistingBuildingKey() == null ? "Building" : "Upgrading");

            Duration remainingDuration = buildRequest.getRemainingTime();
            String msg;
            if (remainingDuration.equals(Duration.ZERO)) {
                if (buildRequest.getPercentComplete() > 99.0) {
                    msg = String.format(Locale.ENGLISH, "%s %d %%, almost done",
                            prefix, (int) buildRequest.getPercentComplete());
                } else {
                    msg = String.format(Locale.ENGLISH, "%s %d %%, not enough resources to complete.",
                            prefix, (int) buildRequest.getPercentComplete());
                }
            } else if (remainingDuration.getStandardMinutes() > 0) {
                msg = String.format(Locale.ENGLISH, "%s %d %%, %s left",
                                    prefix, (int) buildRequest.getPercentComplete(),
                                    TimeInHours.format(remainingDuration));
            } else {
                msg = String.format(Locale.ENGLISH, "%s %d %%, almost done",
                                    prefix, (int) buildRequest.getPercentComplete());
            }
            progressText.setText(Html.fromHtml(msg));

            progressBar.setProgress((int) buildRequest.getPercentComplete());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemEntry entry = mEntries.get(position);
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (entry.buildRequest == null) {
                    view = inflater.inflate(R.layout.buildqueue_list_colony_row, null);
                } else {
                    view = inflater.inflate(R.layout.solarsystem_buildings_design, null);
                }
            }

            if (entry.buildRequest == null) {
                ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
                ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
                TextView name = (TextView) view.findViewById(R.id.star_name);

                int imageSize = (int)(entry.star.getSize() * entry.star.getStarType().getImageScale() * 2);
                if (entry.starDrawable == null) {
                    Sprite sprite = StarImageManager.getInstance().getSprite(mContext, entry.star, imageSize);
                    entry.starDrawable = new SpriteDrawable(sprite);
                }
                if (entry.starDrawable != null) {
                    starIcon.setImageDrawable(entry.starDrawable);
                }

                if (entry.planetDrawable == null) {
                    Sprite sprite = PlanetImageManager.getInstance().getSprite(mContext, entry.planet);
                    entry.planetDrawable = new SpriteDrawable(sprite);
                }
                if (entry.planetDrawable != null) {
                    planetIcon.setImageDrawable(entry.planetDrawable);
                }

                name.setText(String.format(Locale.ENGLISH, "%s %s", entry.star.getName(),
                             RomanNumeralFormatter.format(entry.planet.getIndex())));
            } else {
                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                TextView row1 = (TextView) view.findViewById(R.id.building_row1);
                entry.progressText = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                entry.progressBar = (ProgressBar) view.findViewById(R.id.building_progress);
                TextView level = (TextView) view.findViewById(R.id.building_level);
                TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);

                // we use these to detect when the view gets recycled in our refresh handler.
                entry.progressText.setTag(entry);
                entry.progressBar.setTag(entry);

                DesignManager dm = DesignManager.getInstance(entry.buildRequest.getBuildKind());
                Design design = dm.getDesign(entry.buildRequest.getDesignID());

                icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

                if (entry.existingBuilding != null) {
                    level.setText(Integer.toString(entry.existingBuilding.getLevel()));
                } else {
                    level.setVisibility(View.GONE);
                    levelLabel.setVisibility(View.GONE);
                }

                if (entry.buildRequest.getCount() == 1) {
                    row1.setText(design.getDisplayName());
                } else {
                    row1.setText(String.format("%d × %s)",
                            entry.buildRequest.getCount(), design.getDisplayName(entry.buildRequest.getCount() > 1)));
                }

                row3.setVisibility(View.GONE);
                entry.progressBar.setVisibility(View.VISIBLE);

                if (mSelectedBuildRequest != null && mSelectedBuildRequest.getKey().equals(entry.buildRequest.getKey())) {
                    view.setBackgroundColor(0xff0c6476);
                } else {
                    view.setBackgroundColor(0xff000000);
                }

                refreshEntryProgress(entry);
            }

            return view;
        }

        private class ItemEntry {
            public Star star;
            public Planet planet;
            public SpriteDrawable starDrawable;
            public SpriteDrawable planetDrawable;
            public BuildRequest buildRequest;
            public Building existingBuilding;
            public ProgressBar progressBar;
            public TextView progressText;
        }
    }

    /**
     * This is called every five seconds, we'll refresh the current progress values.
     */
    private class ProgressUpdater implements Runnable {
        @Override
        public void run() {
            mBuildQueueListAdapter.refreshProgress();
            if (mSelectedBuildRequest != null) {
                refreshSelection();
            }
            mHandler.postDelayed(this, 5000);
        }
    }

    public interface BuildQueueActionListener {
        void onAccelerateClick(Star star, BuildRequest buildRequest);
        void onStopClick(Star star, BuildRequest buildRequest);
    }
}
