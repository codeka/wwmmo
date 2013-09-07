package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
import au.com.codeka.common.design.Design;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.model.BuildRequest;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarType;

public class BuildQueueList extends FrameLayout
                            implements StarManager.StarFetchedHandler {
    private Context mContext;
    private BuildQueueActionListener mActionListener;
    private BuildQueueListAdapter mBuildQueueListAdapter;
    private String mColonyKey;
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
        ArrayList<BuildRequest> buildRequests = new ArrayList<BuildRequest>(star.build_requests);
        refresh(star, colony, buildRequests);
    }

    public void refresh(final Star star, final Colony colony, List<BuildRequest> allBuildRequests) {
        Map<String, Star> stars = new TreeMap<String, Star>();
        stars.put(star.key, star);

        Map<String, Colony> colonies = new TreeMap<String, Colony>();
        colonies.put(colony.key, colony);

        List<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
        for (BuildRequest request : allBuildRequests) {
            if (request.colony_key.equals(colony.key)) {
                buildRequests.add(request);
            }
        }

        mColonyKey = colony.key;
        refresh(stars, buildRequests);
    }

    public void refresh(final List<BuildRequest> buildRequests) {
        TreeSet<String> starKeys = new TreeSet<String>();
        for (BuildRequest buildRequest : buildRequests) {
            if (!starKeys.contains(buildRequest.star_key)) {
                starKeys.add(buildRequest.star_key);
            }
        }

        StarManager.i.requestStarSummaries(starKeys, new StarManager.StarSummariesFetchedHandler() {
            @Override
            public void onStarSummariesFetched(Collection<Star> stars) {
                TreeMap<String, Star> summaries = new TreeMap<String, Star>();
                for (Star star : stars) {
                    summaries.put(star.key, star);
                }
                refresh(summaries, buildRequests);
            }
        });
    }

    public void refresh(final Map<String, Star> stars,
                        final List<BuildRequest> buildRequests) {
        // save the list of star keys we're interested in here
        mStarKeys = new ArrayList<String>();
        for (Star star : stars.values()) {
            mStarKeys.add(star.key);
        }

        mBuildQueueListAdapter.setBuildQueue(stars, buildRequests);
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

        Design design = DesignManager.i.getDesign(DesignKind.fromBuildKind(mSelectedBuildRequest.build_kind),
                                                  mSelectedBuildRequest.design_id);

        icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        if (mSelectedBuildRequest.count == 1) {
            buildingName.setText(design.getDisplayName());
        } else {
            buildingName.setText(String.format(Locale.ENGLISH, "%d × %s",
                    mSelectedBuildRequest.count, design.getDisplayName(mSelectedBuildRequest.count > 1)));
        }

        mBuildQueueListAdapter.refreshEntryProgress(mSelectedBuildRequest, progressBar, progressText);
    }

    @Override
    public void onStarFetched(Star s) {
        if (mStarKeys == null) {
            return;
        }

        boolean ourStar = false;
        for (String starKey : mStarKeys) {
            if (starKey.equals(s.key)) {
                ourStar = true;
            }
        }
        if (!ourStar) {
            return;
        }

        mBuildQueueListAdapter.onStarRefreshed(s);
    }

    @Override
    public void onAttachedToWindow() {
        StarManager.i.addStarUpdatedListener(null, this);

        mProgressUpdater = new ProgressUpdater();
        mHandler.postDelayed(mProgressUpdater, 5000);
    }

    @Override
    public void onDetachedFromWindow() {
        StarManager.i.removeStarUpdatedListener(this);

        mHandler.removeCallbacks(mProgressUpdater);
        mProgressUpdater = null;
    }

    /**
     * This adapter is used to populate the list of buildings that are currently in progress.
     */
    private class BuildQueueListAdapter extends BaseAdapter {
        private List<BuildRequest> mBuildRequests;
        private Map<String, Star> mStarSummaries;
        private List<ItemEntry> mEntries;

        public void setBuildQueue(Map<String, Star> stars,
                                  List<BuildRequest> buildRequests) {
            mBuildRequests = new ArrayList<BuildRequest>(buildRequests);
            mStarSummaries = stars;

            Collections.sort(mBuildRequests, new Comparator<BuildRequest>() {
                @Override
                public int compare(BuildRequest lhs, BuildRequest rhs) {
                    // sort by star, then by design, then by count
                    if (!lhs.colony_key.equals(rhs.colony_key)) {
                        if (!lhs.star_key.equals(rhs.star_key)) {
                            Star lhsStar = mStarSummaries.get(lhs.star_key);
                            Star rhsStar = mStarSummaries.get(rhs.star_key);

                            if (lhsStar == null) {
                                return -1;
                            } else if (rhsStar == null) {
                                return 1;
                            }

                            return lhsStar.name.compareTo(rhsStar.name);
                        } else {
                            return lhs.planet_index - rhs.planet_index;
                        }
                    } else {
                        return lhs.start_time.compareTo(rhs.start_time);
                    }
                }
            });

            mEntries = new ArrayList<ItemEntry>();
            String lastStarKey = "";
            int lastPlanetIndex = -1;
            for (BuildRequest buildRequest : mBuildRequests) {
                Star star = mStarSummaries.get(buildRequest.star_key);
                if (star == null) {
                    continue;
                }

                if (!buildRequest.star_key.equals(lastStarKey) || buildRequest.planet_index != lastPlanetIndex) {
                    if (mShowStars) {
                        ItemEntry entry = new ItemEntry();
                        entry.star = star;
                        entry.planet = (Planet) star.planets.get(buildRequest.planet_index - 1);
                        mEntries.add(entry);
                    }
                    lastStarKey = buildRequest.star_key;
                    lastPlanetIndex = buildRequest.planet_index;
                }

                ItemEntry entry = new ItemEntry();
                entry.buildRequest = buildRequest;

                if (buildRequest.existing_building_key != null) {
                    entry.existingBuildingKey = buildRequest.existing_building_key;
                    entry.existingBuildingLevel = buildRequest.existing_building_level;
                }

                mEntries.add(entry);
            }

            notifyDataSetChanged();
        }

        /**
         * Called when a given star refreshes, we'll refresh just the build requests in that
         * star.
         */
        public void onStarRefreshed(Star s) {
            ArrayList<BuildRequest> newBuildRequests = new ArrayList<BuildRequest>();

            // copy old build requests that are not for this star over
            for (BuildRequest br : mBuildRequests) {
                if (!br.star_key.equals(s.key)) {
                    newBuildRequests.add(br);
                }
            }

            // copy build requests from the new star over
            for (BuildRequest br : s.build_requests) {
                // only add the build request if it's for a colony we're displaying
                if (mColonyKey == null || br.colony_key.equals(mColonyKey)) {
                    newBuildRequests.add((BuildRequest) br);
                }
            }

            setBuildQueue(mStarSummaries, newBuildRequests);
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
            return mStarSummaries.get(buildRequest.star_key);
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
                    buildRequest.existing_building_key == null ? "Building" : "Upgrading");

            Duration remainingDuration = Model.getRemainingTime(buildRequest);
            String msg;
            if (remainingDuration.equals(Duration.ZERO)) {
                if (Model.getPercentComplete(buildRequest) > 99.0) {
                    msg = String.format(Locale.ENGLISH, "%s %d %%, almost done",
                            prefix, (int) Model.getPercentComplete(buildRequest));
                } else {
                    msg = String.format(Locale.ENGLISH, "%s %d %%, not enough resources to complete.",
                            prefix, (int) Model.getPercentComplete(buildRequest));
                }
            } else if (remainingDuration.getStandardMinutes() > 0) {
                msg = String.format(Locale.ENGLISH, "%s %d %%, %s left",
                                    prefix, (int) Model.getPercentComplete(buildRequest),
                                    TimeInHours.format(remainingDuration));
            } else {
                msg = String.format(Locale.ENGLISH, "%s %d %%, almost done",
                                    prefix, (int) Model.getPercentComplete(buildRequest));
            }
            progressText.setText(Html.fromHtml(msg));

            progressBar.setProgress((int) Model.getPercentComplete(buildRequest));
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

                int imageSize = (int)(entry.star.size * StarType.get(entry.star).getImageScale() * 2);
                if (entry.starDrawable == null) {
                    Sprite sprite = StarImageManager.getInstance().getSprite(entry.star, imageSize, true);
                    entry.starDrawable = new SpriteDrawable(sprite);
                }
                if (entry.starDrawable != null) {
                    starIcon.setImageDrawable(entry.starDrawable);
                }

                if (entry.planetDrawable == null) {
                    Sprite sprite = PlanetImageManager.getInstance().getSprite(entry.star, entry.planet);
                    entry.planetDrawable = new SpriteDrawable(sprite);
                }
                if (entry.planetDrawable != null) {
                    planetIcon.setImageDrawable(entry.planetDrawable);
                }

                name.setText(String.format(Locale.ENGLISH, "%s %s", entry.star.name,
                             RomanNumeralFormatter.format(entry.planet.index)));
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

                Design design = DesignManager.i.getDesign(DesignKind.fromBuildKind(entry.buildRequest.build_kind),
                                                          entry.buildRequest.design_id);

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                if (entry.existingBuildingKey != null) {
                    level.setText(Integer.toString(entry.existingBuildingLevel));
                } else {
                    level.setVisibility(View.GONE);
                    levelLabel.setVisibility(View.GONE);
                }

                if (entry.buildRequest.count == 1) {
                    row1.setText(design.getDisplayName());
                } else {
                    row1.setText(String.format("%d × %s",
                            entry.buildRequest.count, design.getDisplayName(entry.buildRequest.count > 1)));
                }

                row3.setVisibility(View.GONE);
                entry.progressBar.setVisibility(View.VISIBLE);

                if (mSelectedBuildRequest != null && mSelectedBuildRequest.key.equals(entry.buildRequest.key)) {
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
            public String existingBuildingKey;
            public int existingBuildingLevel;
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
