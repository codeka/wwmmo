package au.com.codeka.warworlds.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
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
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;

public class ColonyList extends FrameLayout {
    private Context mContext;
    private Map<String, Star> mStars;
    private Colony mSelectedColony;
    private boolean mIsInitialized;
    private ColonyListAdapter mColonyListAdapter;
    private ColonyActionHandler mColonyActionListener;

    // collection of stars we're currently simulating
    private static TreeSet<String> sSimulatingStars = new TreeSet<String>();

    public ColonyList(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        View child = inflate(context, R.layout.colony_list_ctrl, null);
        this.addView(child);
    }

    public void refresh(final List<Colony> colonies, Map<String, Star> stars) {
        mStars = stars;

        initialize();

        // if we had a colony selected, make sure we still have the same
        // colony selected after we refresh
        if (mSelectedColony != null) {
            Colony selectedColony = mSelectedColony;
            mSelectedColony = null;

            for (Colony c : colonies) {
                if (c.getKey().equals(selectedColony.getKey())) {
                    mSelectedColony = c;
                    break;
                }
            }
        }

        refreshSelectedColony();
        mColonyListAdapter.setColonies(stars, colonies);
    }

    public void setOnColonyActionListener(ColonyActionHandler listener) {
        mColonyActionListener = listener;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        ImageManager.eventBus.register(mEventHandler);
        StarManager.eventBus.register(mEventHandler);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ImageManager.eventBus.unregister(mEventHandler);
        StarManager.eventBus.unregister(mEventHandler);
    }

    private void initialize() {
        if (mIsInitialized) {
            return;
        }
        mIsInitialized = true;

        mColonyListAdapter = new ColonyListAdapter();
        final ListView colonyList = (ListView) findViewById(R.id.colonies);
        colonyList.setAdapter(mColonyListAdapter);

        colonyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mSelectedColony = mColonyListAdapter.getColonyAtPosition(position);
                mColonyListAdapter.notifyDataSetChanged();
                refreshSelectedColony();
            }
        });

        final Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedColony != null && mColonyActionListener != null) {
                    Star star = mStars.get(mSelectedColony.getStarKey());
                    mColonyActionListener.onViewColony(star, mSelectedColony);
                }
            }
        });
    }

    private void refreshSelectedColony() {
        final PlanetDetailsView planetDetailsView = (PlanetDetailsView) findViewById(R.id.colony_info);

        if (mSelectedColony == null) {
            planetDetailsView.setup(null, null, null);
        } else {
            // the colony might've changed so update it first
            for(ColonyListAdapter.ItemEntry entry : mColonyListAdapter.getEntries()) {
                if (entry.colony != null && entry.colony.getKey().equals(mSelectedColony.getKey())) {
                    mSelectedColony = entry.colony;
                }
            }

            Star star = mStars.get(mSelectedColony.getStarKey());
            Planet planet = (Planet) star.getPlanets()[mSelectedColony.getPlanetIndex() - 1];
            planetDetailsView.setup(star, planet, mSelectedColony);
        }
    }

    public static void populateColonyListRow(Context context, View view, Colony colony, Star star) {
        Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];

        ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
        TextView colonyName = (TextView) view.findViewById(R.id.colony_name);
        TextView colonySummary = (TextView) view.findViewById(R.id.colony_summary);

        Sprite sprite = PlanetImageManager.getInstance().getSprite(planet);
        planetIcon.setImageDrawable(new SpriteDrawable(sprite));

        colonyName.setText(String.format("%s %s", star.getName(), RomanNumeralFormatter.format(planet.getIndex())));

        if (star.getLastSimulation().compareTo(DateTime.now(DateTimeZone.UTC).minusMinutes(5)) < 0) {
            // if the star hasn't been simulated for > 5 minutes, just display ??? for the
            // various parameters (a simulation will be scheduled)
            colonySummary.setText("Pop: ?");
        } else {
            colonySummary.setText(String.format("Pop: %d", (int) colony.getPopulation()));
        }
    }

    private static void populateColonyListStarRow(Context context, View view, Star star) {
        ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
        TextView starName = (TextView) view.findViewById(R.id.star_name);
        TextView starType = (TextView) view.findViewById(R.id.star_type);
        TextView starGoodsDelta = (TextView) view.findViewById(R.id.star_goods_delta);
        TextView starGoodsTotal = (TextView) view.findViewById(R.id.star_goods_total);
        TextView starMineralsDelta = (TextView) view.findViewById(R.id.star_minerals_delta);
        TextView starMineralsTotal = (TextView) view.findViewById(R.id.star_minerals_total);

        int imageSize = (int)(star.getSize() * star.getStarType().getImageScale() * 2);
        Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
        starIcon.setImageDrawable(new SpriteDrawable(sprite));

        starName.setText(star.getName());
        starType.setText(star.getStarType().getDisplayName());

        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        EmpirePresence empirePresence = null;
        for (BaseEmpirePresence baseEmpirePresence : star.getEmpirePresences()) {
            if (baseEmpirePresence.getEmpireKey().equals(myEmpire.getKey())) {
                empirePresence = (EmpirePresence) baseEmpirePresence;
                break;
            }
        }

        boolean needSimulation = star.getLastSimulation().compareTo(DateTime.now(DateTimeZone.UTC).minusMinutes(5)) < 0;
        if (!needSimulation && empirePresence != null) {
            if (empirePresence.getDeltaGoodsPerHour() == 0.0f && empirePresence.getDeltaMineralsPerHour() == 0.0f) {
                // this is so unlikely, it's probably because we didn't simulate.
                needSimulation = true;
            }
        }

        if (needSimulation) {
            // if the star hasn't been simulated for > 5 minutes, schedule a simulation now and
            // just display ??? for the various parameters
            starGoodsDelta.setText("");
            starGoodsTotal.setText("???");
            starMineralsDelta.setText("");
            starMineralsTotal.setText("???");
            scheduleSimulate(star);
        } else {
            starGoodsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
                    empirePresence.getDeltaGoodsPerHour() < 0 ? "-" : "+",
                    Math.abs(Math.round(empirePresence.getDeltaGoodsPerHour()))));
            if (empirePresence.getDeltaGoodsPerHour() < 0) {
                starGoodsDelta.setTextColor(Color.RED);
            } else {
                starGoodsDelta.setTextColor(Color.GREEN);
            }
            starGoodsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
                    Math.round(empirePresence.getTotalGoods()),
                    Math.round(empirePresence.getMaxGoods())));

            starMineralsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
                    empirePresence.getDeltaMineralsPerHour() < 0 ? "-" : "+",
                    Math.abs(Math.round(empirePresence.getDeltaMineralsPerHour()))));
            if (empirePresence.getDeltaMineralsPerHour() < 0) {
                starMineralsDelta.setTextColor(Color.RED);
            } else {
                starMineralsDelta.setTextColor(Color.GREEN);
            }
            starMineralsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
                    Math.round(empirePresence.getTotalMinerals()),
                    Math.round(empirePresence.getMaxMinerals())));
        }
    }

    private static void scheduleSimulate(final Star star) {
        synchronized(sSimulatingStars) {
            if (sSimulatingStars.contains(star.getKey())) {
                return;
            }
            sSimulatingStars.add(star.getKey());
        }

        new AsyncTask<Void, Void, Star>() {
            @Override
            protected Star doInBackground(Void... params) {
                Simulation sim = new Simulation();
                sim.simulate(star);
                return star;
            }

            @Override
            protected void onPostExecute(Star star) {
                StarManager.eventBus.publish(star);
                sSimulatingStars.remove(star.getKey());
            }
        }.execute();
    }

    private Object mEventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            // if a star is updated, we'll want to refresh our colony list because the
            // colony inside it might've changed too...
            mColonyListAdapter.onStarUpdated(star);
        }

        @EventHandler
        public void onSpriteGenerated(ImageManager.SpriteGeneratedEvent event) {
            mColonyListAdapter.notifyDataSetChanged();
        }
    };

    /**
     * This adapter is used to populate the list of colonies that we're looking at.
     */
    private class ColonyListAdapter extends BaseAdapter {
        private ArrayList<ItemEntry> mEntries;
        private Map<String, Star> mStars;

        public void onStarUpdated(Star star) {
            for (BaseColony starColony : star.getColonies()) {
                for (int i = 0; i < mEntries.size(); i++) {
                    ItemEntry entry = mEntries.get(i);
                    if (entry.colony != null && entry.colony.getKey().equals(starColony.getKey())) {
                        entry.colony = (Colony) starColony;
                        break;
                    }
                }
            }

            for (int i = 0; i < mEntries.size(); i++) {
                ItemEntry entry = mEntries.get(i);
                if (entry.star != null && entry.star.getKey().equals(star.getKey())) {
                    entry.star = star;
                }
            }

            notifyDataSetChanged();
            refreshSelectedColony();
        }

        /**
         * Sets the list of fleets that we'll be displaying.
         */
        public void setColonies(Map<String, Star> stars, List<Colony> colonies) {
            mEntries = new ArrayList<ItemEntry>();
            mStars = stars;

            Collections.sort(colonies, new Comparator<Colony>() {
                @Override
                public int compare(Colony lhs, Colony rhs) {
                    // sort by star, then by planet index
                    if (!lhs.getStarKey().equals(rhs.getStarKey())) {
                        Star lhsStar = mStars.get(lhs.getStarKey());
                        Star rhsStar = mStars.get(rhs.getStarKey());
                        return lhsStar.getName().compareTo(rhsStar.getName());
                    } else {
                        return lhs.getPlanetIndex() - rhs.getPlanetIndex();
                    }
                }
            });

            Star lastStar = null;
            for (Colony colony : colonies) {
                if (lastStar == null || !lastStar.getKey().equals(colony.getStarKey())) {
                    lastStar = mStars.get(colony.getStarKey());
                    mEntries.add(new ItemEntry(lastStar));
                }
                mEntries.add(new ItemEntry(colony));
            }

            notifyDataSetChanged();
        }

        public List<ItemEntry> getEntries() {
            return mEntries;
        }

        public Colony getColonyAtPosition(int position) {
            if (mEntries == null)
                return null;
            return mEntries.get(position).colony;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mEntries == null)
                return 0;

            return mEntries.get(position).star == null ? 1 : 0;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mEntries == null)
                return false;

            return (mEntries.get(position).star == null);
        }

        @Override
        public int getCount() {
            if (mEntries == null)
                return 0;
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
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
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                if (entry.star != null)
                    view = inflater.inflate(R.layout.colony_list_star_row, null);
                else
                    view = inflater.inflate(R.layout.colony_list_colony_row, null);
            }

            if (entry.star != null) {
                populateColonyListStarRow(mContext, view, entry.star);
            } else {
                Colony colony = entry.colony;
                Star star = mStars.get(colony.getStarKey());
                populateColonyListRow(mContext, view, colony, star);

                if (mSelectedColony != null && mSelectedColony.getKey().equals(colony.getKey())) {
                    view.setBackgroundColor(0xff0c6476);
                } else {
                    view.setBackgroundColor(0xff000000);
                }
            }

            return view;
        }

        public class ItemEntry {
            public Colony colony;
            public Star star;

            public ItemEntry(Star star) {
                this.star = star;
            }
            public ItemEntry(Colony colony) {
                this.colony = colony;
            }
        }
    }

    public interface ColonyActionHandler {
        void onViewColony(Star star, Colony colony);
    }
}
