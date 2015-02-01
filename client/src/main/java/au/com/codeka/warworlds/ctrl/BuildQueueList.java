package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
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
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.Design;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.NotesDialog;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

public class BuildQueueList extends FrameLayout {
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

        buildQueueList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                final BuildQueueListAdapter.ItemEntry entry = (BuildQueueListAdapter.ItemEntry) mBuildQueueListAdapter.getItem(position);

                NotesDialog dialog = new NotesDialog();
                dialog.setup(entry.buildRequest.getNotes(), new NotesDialog.NotesChangedHandler() {
                    @Override
                    public void onNotesChanged(String notes) {
                        entry.buildRequest.setNotes(notes);
                        mBuildQueueListAdapter.notifyDataSetChanged();

                        BuildManager.getInstance().updateNotes(entry.buildRequest.getKey(), notes);
                    }
                });

                FragmentActivity activity = (FragmentActivity) mContext;
                dialog.show(activity.getSupportFragmentManager(), "");
                return true;
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
        ArrayList<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
        for (BaseBuildRequest br : star.getBuildRequests()) {
            buildRequests.add((BuildRequest) br);
        }
        refresh(star, colony, buildRequests);
    }

    public void refresh(final Star star, final Colony colony, List<BuildRequest> allBuildRequests) {
        Map<String, Star> stars = new TreeMap<>();
        stars.put(star.getKey(), star);

        Map<String, Colony> colonies = new TreeMap<String, Colony>();
        colonies.put(colony.getKey(), colony);

        List<BuildRequest> buildRequests = new ArrayList<BuildRequest>();
        for (BuildRequest request : allBuildRequests) {
            if (request.getColonyKey().equals(colony.getKey())) {
                buildRequests.add(request);
            }
        }

        mColonyKey = colony.getKey();
        refresh(stars, buildRequests);
    }

    public void refresh(final List<BuildRequest> buildRequests) {
        TreeSet<String> starKeys = new TreeSet<String>();
        for (BuildRequest buildRequest : buildRequests) {
            if (!starKeys.contains(buildRequest.getStarKey())) {
                starKeys.add(buildRequest.getStarKey());
            }
        }
/*
        StarManager.getInstance().requestStarSummaries(starKeys, new StarManager.StarSummariesFetchedHandler() {
            @Override
            public void onStarSummariesFetched(Collection<StarSummary> stars) {
                TreeMap<String, StarSummary> summaries = new TreeMap<String, StarSummary>();
                for (StarSummary star : stars) {
                    summaries.put(star.getKey(), star);
                }
                refresh(summaries, buildRequests);
            }
        });*/
    }

    public void refresh(final Map<String, Star> stars,
                        final List<BuildRequest> buildRequests) {
        // save the list of star keys we're interested in here
        mStarKeys = new ArrayList<>();
        for (Star star : stars.values()) {
            mStarKeys.add(star.getKey());
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

        Design design = DesignManager.i.getDesign(mSelectedBuildRequest.getDesignKind(),
                                                  mSelectedBuildRequest.getDesignID());

        icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

        if (mSelectedBuildRequest.getCount() == 1) {
            buildingName.setText(design.getDisplayName());
        } else {
            buildingName.setText(String.format(Locale.ENGLISH, "%d × %s",
                    mSelectedBuildRequest.getCount(), design.getDisplayName(mSelectedBuildRequest.getCount() > 1)));
        }

        mBuildQueueListAdapter.refreshEntryProgress(mSelectedBuildRequest, progressBar, progressText);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        StarManager.eventBus.register(mEventHandler);

        mProgressUpdater = new ProgressUpdater();
        mHandler.postDelayed(mProgressUpdater, 5000);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        StarManager.eventBus.unregister(mEventHandler);

        mHandler.removeCallbacks(mProgressUpdater);
        mProgressUpdater = null;
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onStarFetcher(Star star) {
            if (mStarKeys == null) {
                return;
            }

            boolean ourStar = false;
            for (String starKey : mStarKeys) {
                if (starKey.equals(star.getKey())) {
                    ourStar = true;
                }
            }
            if (!ourStar) {
                return;
            }

            mBuildQueueListAdapter.onStarRefreshed(star);
        }
    };

    /**
     * This adapter is used to populate the list of buildings that are currently in progress.
     */
    private class BuildQueueListAdapter extends BaseAdapter {
        private List<BuildRequest> mBuildRequests;
        private Map<String, Star> mStarSummaries;
        private List<ItemEntry> mEntries;

        public void setBuildQueue(Map<String, Star> stars,
                                  List<BuildRequest> buildRequests) {
            mBuildRequests = new ArrayList<>(buildRequests);
            mStarSummaries = stars;

            Collections.sort(mBuildRequests, new Comparator<BuildRequest>() {
                @Override
                public int compare(BuildRequest lhs, BuildRequest rhs) {
                    // sort by star, then by design, then by count
                    if (!lhs.getColonyKey().equals(rhs.getColonyKey())) {
                        if (!lhs.getStarKey().equals(rhs.getStarKey())) {
                            Star lhsStar = mStarSummaries.get(lhs.getStarKey());
                            Star rhsStar = mStarSummaries.get(rhs.getStarKey());

                            if (lhsStar == null) {
                                return -1;
                            } else if (rhsStar == null) {
                                return 1;
                            }

                            return lhsStar.getName().compareTo(rhsStar.getName());
                        } else {
                            return lhs.getPlanetIndex() - rhs.getPlanetIndex();
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
                Star star = mStarSummaries.get(buildRequest.getStarKey());
                if (star == null) {
                    continue;
                }

                if (!buildRequest.getStarKey().equals(lastStarKey) || buildRequest.getPlanetIndex() != lastPlanetIndex) {
                    if (mShowStars) {
                        ItemEntry entry = new ItemEntry();
                        entry.star = star;
                        entry.planet = (Planet) star.getPlanets()[buildRequest.getPlanetIndex() - 1];
                        mEntries.add(entry);
                    }
                    lastStarKey = buildRequest.getStarKey();
                    lastPlanetIndex = buildRequest.getPlanetIndex();
                }

                ItemEntry entry = new ItemEntry();
                entry.buildRequest = buildRequest;

                if (buildRequest.getExistingBuildingKey() != null) {
                    entry.existingBuildingKey = buildRequest.getExistingBuildingKey();
                    entry.existingBuildingLevel = buildRequest.getExistingBuildingLevel();
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
                if (!br.getStarKey().equals(s.getKey())) {
                    newBuildRequests.add(br);
                }
            }

            // copy build requests from the new star over
            for (BaseBuildRequest br : s.getBuildRequests()) {
                // only add the build request if it's for a colony we're displaying
                if (mColonyKey == null || br.getColonyKey().equals(mColonyKey)) {
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
            return mStarSummaries.get(buildRequest.getStarKey());
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
                                    TimeFormatter.create().format(remainingDuration));
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
                    view = inflater.inflate(R.layout.buildings_design, null);
                }
            }

            if (entry.buildRequest == null) {
                ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
                ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
                TextView name = (TextView) view.findViewById(R.id.star_name);

                int imageSize = (int)(entry.star.getSize() * entry.star.getStarType().getImageScale() * 2);
                if (entry.starDrawable == null) {
                    Sprite sprite = StarImageManager.getInstance().getSprite(entry.star, imageSize, true);
                    entry.starDrawable = new SpriteDrawable(sprite);
                }
                if (entry.starDrawable != null) {
                    starIcon.setImageDrawable(entry.starDrawable);
                }

                if (entry.planetDrawable == null) {
                    Sprite sprite = PlanetImageManager.getInstance().getSprite(entry.planet);
                    entry.planetDrawable = new SpriteDrawable(sprite);
                }
                if (entry.planetDrawable != null) {
                    planetIcon.setImageDrawable(entry.planetDrawable);
                }

                name.setText(String.format(Locale.ENGLISH, "%s %s", entry.star.getName(),
                             RomanNumeralFormatter.format(entry.planet.getIndex())));
            } else {
                ImageView icon = (ImageView) view.findViewById(R.id.building_icon);
                LinearLayout row1 = (LinearLayout) view.findViewById(R.id.building_row1);
                entry.progressText = (TextView) view.findViewById(R.id.building_row2);
                TextView row3 = (TextView) view.findViewById(R.id.building_row3);
                entry.progressBar = (ProgressBar) view.findViewById(R.id.building_progress);
                TextView level = (TextView) view.findViewById(R.id.building_level);
                TextView levelLabel = (TextView) view.findViewById(R.id.building_level_label);
                TextView notes = (TextView) view.findViewById(R.id.notes);

                // we use these to detect when the view gets recycled in our refresh handler.
                entry.progressText.setTag(entry);
                entry.progressBar.setTag(entry);

                Design design = DesignManager.i.getDesign(entry.buildRequest.getDesignKind(),
                                                          entry.buildRequest.getDesignID());

                icon.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));

                if (entry.existingBuildingKey != null) {
                    level.setText(Integer.toString(entry.existingBuildingLevel));
                } else {
                    level.setVisibility(View.GONE);
                    levelLabel.setVisibility(View.GONE);
                }

                row1.removeAllViews();
                if (entry.buildRequest.getCount() == 1) {
                    addTextToRow(mContext, row1, design.getDisplayName());
                } else {
                    addTextToRow(mContext, row1, String.format("%d × %s",
                            entry.buildRequest.getCount(), design.getDisplayName(entry.buildRequest.getCount() > 1)));
                }

                row3.setVisibility(View.GONE);
                entry.progressBar.setVisibility(View.VISIBLE);

                if (entry.buildRequest.getNotes() != null) {
                    notes.setText(entry.buildRequest.getNotes());
                    notes.setVisibility(View.VISIBLE);
                } else {
                    notes.setText("");
                    notes.setVisibility(View.GONE);
                }

                if (mSelectedBuildRequest != null && mSelectedBuildRequest.getKey().equals(entry.buildRequest.getKey())) {
                    view.setBackgroundResource(R.color.list_item_selected);
                } else {
                    view.setBackgroundResource(android.R.color.transparent);
                }

                refreshEntryProgress(entry);
            }

            return view;
        }

        private void addTextToRow(Context context, LinearLayout row, CharSequence text) {
            TextView tv = new TextView(context);
            tv.setText(text);
            tv.setSingleLine(true);
            tv.setEllipsize(TruncateAt.END);
            row.addView(tv);
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
