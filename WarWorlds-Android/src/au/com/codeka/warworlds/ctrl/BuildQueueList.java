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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.TimeInHours;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.BuildRequest;
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
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarImageManager;

public class BuildQueueList extends FrameLayout implements MyEmpire.RefreshAllCompleteHandler,
                                                           StarManager.StarFetchedHandler {
    private Context mContext;
    private BuildQueueActionListener mActionListener;
    private BuildQueueListAdapter mBuildQueueListAdapter;
    private Colony mColony;
    private List<String> mStarKeys;
    private Handler mHandler;
    private ProgressUpdater mProgressUpdater;

    public BuildQueueList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View child = inflate(context, R.layout.buildqueue_list_ctrl, null);
        this.addView(child);

        mBuildQueueListAdapter = new BuildQueueListAdapter();
        ListView buildQueueList = (ListView) findViewById(R.id.build_queue_list);
        buildQueueList.setAdapter(mBuildQueueListAdapter);

        buildQueueList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BuildQueueListAdapter.ItemEntry entry = (BuildQueueListAdapter.ItemEntry) mBuildQueueListAdapter.getItem(position);
                if (mActionListener != null && entry.buildRequest != null) {
                    mActionListener.onBuildClick(mBuildQueueListAdapter.getStarForBuildRequest(entry.buildRequest), entry.buildRequest);
                }
            }
        });

        mHandler = new Handler();
    }

    public void setBuildQueueActionListener(BuildQueueActionListener listener) {
        mActionListener = listener;
    }

    /**
     * If one of the stars we care about updates, we'll want to refresh ourselves, too.
     */
    @Override
    public void onStarFetched(Star s) {
        for (String starKey : mStarKeys) {
            if (s.getKey().equals(starKey)) {
                EmpireManager.getInstance().refreshEmpire();
                return;
            }
        }
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
        for (BuildRequest request : star.getBuildRequests()) {
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

    /**
     * When the empire is updated, make sure we display the latest build queue.
     */
    @Override
    public void onRefreshAllComplete(MyEmpire empire) {
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

            refresh(empire.getImportantStar(mColony.getStarKey()),
                    mColony, empire.getAllBuildRequests());
        } else {
            Map<String, Colony> colonies = new TreeMap<String, Colony>();
            for (Colony colony : empire.getAllColonies()) {
                colonies.put(colony.getKey(), colony);
            }

            refresh(empire.getImportantStars(), colonies, empire.getAllBuildRequests());
        }
    }

    @Override
    public void onAttachedToWindow() {
        EmpireManager.getInstance().getEmpire().addRefreshAllCompleteHandler(this);
        StarManager.getInstance().addStarUpdatedListener(null, this);

        mProgressUpdater = new ProgressUpdater();
        mHandler.postDelayed(mProgressUpdater, 5000);
    }

    @Override
    public void onDetachedFromWindow() {
        EmpireManager.getInstance().getEmpire().removeRefreshAllCompleteHandler(this);
        StarManager.getInstance().removeStarUpdatedListener(this);

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
                if (!colony.getStarKey().equals(lastStarKey) || colony.getPlanetIndex() != lastPlanetIndex) {
                    ItemEntry entry = new ItemEntry();
                    entry.star = mStars.get(colony.getStarKey());
                    entry.planet = entry.star.getPlanets()[colony.getPlanetIndex() - 1];
                    mEntries.add(entry);
                    lastStarKey = colony.getStarKey();
                    lastPlanetIndex = colony.getPlanetIndex();
                }

                ItemEntry entry = new ItemEntry();
                entry.buildRequest = buildRequest;
                mEntries.add(entry);
            }

            notifyDataSetChanged();
        }

        public Star getStarForBuildRequest(BuildRequest buildRequest) {
            Colony colony = mColonies.get(buildRequest.getColonyKey());
            return mStars.get(colony.getStarKey());
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mEntries == null)
                return 0;

            return mEntries.get(position).buildRequest == null ? 1 : 2;
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

        private void refreshEntryProgress(ItemEntry entry) {
            Duration remainingDuration = entry.buildRequest.getRemainingTime();
            if (remainingDuration.equals(Duration.ZERO)) {
                entry.progressText.setText(String.format(Locale.ENGLISH, "%d %%, not enough resources to complete.",
                                           (int) entry.buildRequest.getPercentComplete()));
            } else if (remainingDuration.getStandardMinutes() > 0) {
                entry.progressText.setText(String.format(Locale.ENGLISH, "%d %%, %s left",
                        (int) entry.buildRequest.getPercentComplete(),
                        TimeInHours.format(remainingDuration)));
            } else {
                entry.progressText.setText(String.format(Locale.ENGLISH, "%d %%, almost done",
                        (int) entry.buildRequest.getPercentComplete(),
                        TimeInHours.format(remainingDuration)));
            }

            entry.progressBar.setProgress((int) entry.buildRequest.getPercentComplete());
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

                DesignManager dm = DesignManager.getInstance(entry.buildRequest.getBuildKind());
                Design design = dm.getDesign(entry.buildRequest.getDesignID());

                icon.setImageDrawable(new SpriteDrawable(design.getSprite()));

                if (entry.buildRequest.getCount() == 1) {
                    row1.setText(design.getDisplayName());
                } else {
                    row1.setText(String.format("%s (× %d)",
                                               design.getDisplayName(),
                                               entry.buildRequest.getCount()));
                }

                row3.setVisibility(View.GONE);
                entry.progressBar.setVisibility(View.VISIBLE);
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
            mHandler.postDelayed(this, 5000);
        }
    }

    public interface BuildQueueActionListener {
        void onBuildClick(Star star, BuildRequest buildRequest);
    }
}
