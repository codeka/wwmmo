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
import au.com.codeka.common.Cash;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.EmpirePresence;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;
import au.com.codeka.common.sim.Simulation;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarType;

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
                if (c.key.equals(selectedColony.key)) {
                    mSelectedColony = c;
                    break;
                }
            }
        }

        refreshSelectedColony();
        mColonyListAdapter.setColonies(stars, colonies);
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onDetachedFromWindow() {
    }

    public void setOnColonyActionListener(ColonyActionHandler listener) {
        mColonyActionListener = listener;
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
                    Star star = mStars.get(mSelectedColony.star_key);
                    mColonyActionListener.onViewColony(star, mSelectedColony);
                }
            }
        });

        final Button collectBtn = (Button) findViewById(R.id.collect_btn);
        collectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mColonyActionListener.onCollectTaxes();
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
                if (entry.colony != null && entry.colony.key.equals(mSelectedColony.key)) {
                    mSelectedColony = entry.colony;
                }
            }

            Star star = mStars.get(mSelectedColony.star_key);
            Planet planet = (Planet) star.planets.get(mSelectedColony.planet_index - 1);
            planetDetailsView.setup(star, planet, mSelectedColony);
        }
    }

    public static void populateColonyListRow(Context context, View view, Colony colony, Star star) {
        Planet planet = (Planet) star.planets.get(colony.planet_index - 1);

        ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
        TextView colonyName = (TextView) view.findViewById(R.id.colony_name);
        TextView colonySummary = (TextView) view.findViewById(R.id.colony_summary);
        TextView uncollectedTaxes = (TextView) view.findViewById(R.id.colony_taxes);

        Sprite sprite = PlanetImageManager.getInstance().getSprite(star, planet);
        planetIcon.setImageDrawable(new SpriteDrawable(sprite));

        colonyName.setText(String.format("%s %s", star.name, RomanNumeralFormatter.format(planet.index)));

        if (Model.toDateTime(star.last_simulation).isBefore(DateTime.now(DateTimeZone.UTC).minusMinutes(5))) {
            // if the star hasn't been simulated for > 5 minutes, just display ??? for the
            // various parameters (a simulation will be scheduled)
            colonySummary.setText("Pop: ?");
            uncollectedTaxes.setText("Taxes: ?");
        } else {
            colonySummary.setText(String.format("Pop: %d", (int) (float) colony.population));
            uncollectedTaxes.setText(String.format("Taxes: %s", Cash.format(colony.uncollected_taxes)));
        }
    }

    private static void populateColonyListStarRow(Context context, View view, Star star) {
        ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
        TextView starName = (TextView) view.findViewById(R.id.star_name);
        TextView starTypeName = (TextView) view.findViewById(R.id.star_type);
        TextView starGoodsDelta = (TextView) view.findViewById(R.id.star_goods_delta);
        TextView starGoodsTotal = (TextView) view.findViewById(R.id.star_goods_total);
        TextView starMineralsDelta = (TextView) view.findViewById(R.id.star_minerals_delta);
        TextView starMineralsTotal = (TextView) view.findViewById(R.id.star_minerals_total);

        StarType starType = StarType.get(star);
        int imageSize = (int)(star.size * starType.getImageScale() * 2);
        Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, true);
        starIcon.setImageDrawable(new SpriteDrawable(sprite));

        starName.setText(star.name);
        starTypeName.setText(starType.getDisplayName());

        Empire myEmpire = EmpireManager.i.getEmpire();
        EmpirePresence empirePresence = null;
        for (EmpirePresence ep : star.empires) {
            if (ep.empire_key != null && ep.empire_key.equals(myEmpire.key)) {
                empirePresence = ep;
                break;
            }
        }

        boolean needSimulation = Model.toDateTime(star.last_simulation).isBefore(DateTime.now(DateTimeZone.UTC).minusMinutes(5));
        if (!needSimulation && empirePresence != null) {
            if (empirePresence.goods_delta_per_hour == 0.0f && empirePresence.minerals_delta_per_hour == 0.0f) {
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
                    empirePresence.goods_delta_per_hour < 0 ? "-" : "+",
                    Math.abs(Math.round(empirePresence.goods_delta_per_hour))));
            if (empirePresence.goods_delta_per_hour < 0) {
                starGoodsDelta.setTextColor(Color.RED);
            } else {
                starGoodsDelta.setTextColor(Color.GREEN);
            }
            starGoodsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
                    Math.round(empirePresence.total_goods),
                    Math.round(empirePresence.max_goods)));

            starMineralsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
                    empirePresence.minerals_delta_per_hour < 0 ? "-" : "+",
                    Math.abs(Math.round(empirePresence.minerals_delta_per_hour))));
            if (empirePresence.minerals_delta_per_hour < 0) {
                starMineralsDelta.setTextColor(Color.RED);
            } else {
                starMineralsDelta.setTextColor(Color.GREEN);
            }
            starMineralsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
                    Math.round(empirePresence.total_minerals),
                    Math.round(empirePresence.max_minerals)));
        }
    }

    private static void scheduleSimulate(final Star star) {
        synchronized(sSimulatingStars) {
            if (sSimulatingStars.contains(star.key)) {
                return;
            }
            sSimulatingStars.add(star.key);
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
                StarManager.i.fireStarUpdated(star);
                sSimulatingStars.remove(star.key);
            }
        }.execute();
    }

    /**
     * This adapter is used to populate the list of colonies that we're looking at.
     */
    private class ColonyListAdapter extends BaseAdapter {
        private ArrayList<ItemEntry> mEntries;
        private Map<String, Star> mStars;

        public ColonyListAdapter() {
            // whenever a new star/planet bitmap is generated, redraw the list
            StarImageManager.getInstance().addSpriteGeneratedListener(
                    new ImageManager.SpriteGeneratedListener() {
                @Override
                public void onSpriteGenerated(String key, Sprite sprite) {
                    notifyDataSetChanged();
                }
            });
            PlanetImageManager.getInstance().addSpriteGeneratedListener(
                    new ImageManager.SpriteGeneratedListener() {
                @Override
                public void onSpriteGenerated(String key, Sprite sprite) {
                    notifyDataSetChanged();
                }
            });
            StarManager.i.addStarUpdatedListener(null, new StarManager.StarFetchedHandler() {
                @Override
                public void onStarFetched(Star s) {
                    // if a star is updated, we'll want to refresh our colony list because the
                    // colony inside it might've changed too...
                    for (Colony starColony : s.colonies) {
                        for (int i = 0; i < mEntries.size(); i++) {
                            ItemEntry entry = mEntries.get(i);
                            if (entry.colony != null && entry.colony.key.equals(starColony.key)) {
                                entry.colony = (Colony) starColony;
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < mEntries.size(); i++) {
                        ItemEntry entry = mEntries.get(i);
                        if (entry.star != null && entry.star.key.equals(s.key)) {
                            entry.star = s;
                        }
                    }

                    notifyDataSetChanged();
                    refreshSelectedColony();
                }
            });
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
                    if (!lhs.star_key.equals(rhs.star_key)) {
                        Star lhsStar = mStars.get(lhs.star_key);
                        Star rhsStar = mStars.get(rhs.star_key);
                        return lhsStar.name.compareTo(rhsStar.name);
                    } else {
                        return lhs.planet_index - rhs.planet_index;
                    }
                }
            });

            Star lastStar = null;
            for (Colony colony : colonies) {
                if (lastStar == null || !lastStar.key.equals(colony.star_key)) {
                    lastStar = mStars.get(colony.star_key);
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
                Star star = mStars.get(colony.star_key);
                populateColonyListRow(mContext, view, colony, star);

                if (mSelectedColony != null && mSelectedColony.key.equals(colony.key)) {
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
        void onCollectTaxes();
    }
}
