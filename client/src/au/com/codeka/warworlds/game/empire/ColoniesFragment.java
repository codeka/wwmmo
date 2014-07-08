package au.com.codeka.warworlds.game.empire;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.EmpireStarsFetcher;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSimulationQueue;

/**
 * ColoniesFragment shows a list of stars, which you can expand to see colonies that
 * belong to those stars.
 */
public class ColoniesFragment extends StarsFragment {
    private StarsListAdapter mAdapter;
    private EmpireStarsFetcher mFetcher;

    public ColoniesFragment() {
        mFetcher = new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Colonies, null);
        // fetch the first few stars
        mFetcher.getStars(0, 20);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empire_colonies_tab, null);
        ExpandableListView starsList = (ExpandableListView) v.findViewById(R.id.stars);
        mAdapter = new StarsListAdapter(inflater);
        starsList.setAdapter(mAdapter);
/*
        colonyList.setOnColonyActionListener(new ColonyList.ColonyActionHandler() {
            @Override
            public void onViewColony(Star star, Colony colony) {
                BasePlanet planet = star.getPlanets()[colony.getPlanetIndex() - 1];
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
        });
*/
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        StarManager.eventBus.register(mEventHandler);
        mFetcher.eventBus.register(mEventHandler);
    }

    @Override
    public void onStop() {
        super.onStop();
        StarManager.eventBus.unregister(mEventHandler);
        mFetcher.eventBus.unregister(mEventHandler);
    }

    private Object mEventHandler = new Object() {
        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onStarUpdated(Star star) {
            mAdapter.notifyDataSetChanged();
        }

        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onStarsFetched(EmpireStarsFetcher.StarsFetchedEvent event) {
            mAdapter.notifyDataSetChanged();
        }
    };

    public class StarsListAdapter extends BaseExpandableListAdapter {
        private LayoutInflater mInflater;

        // should never have > 40 visible at once...
        private LruCache<Integer, Star> mStarCache = new LruCache<Integer, Star>(40);
        private MyEmpire mEmpire;

        public StarsListAdapter(LayoutInflater inflater) {
            mInflater = inflater;
            mEmpire = EmpireManager.i.getEmpire();
        }

        // TODO: ?
        @Override
        public boolean hasStableIds() {
            return false;
        }
     
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public int getGroupCount() {
            return mFetcher.getNumStars();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return getStar(groupPosition);
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            Star star = getStar(groupPosition);
            if (star == null) {
                return 0;
            }

            // TODO: cache?
            int numColonies = 0;
            for (int i = 0; i < star.getColonies().size(); i++) {
                Integer empireID = ((Colony) star.getColonies().get(i)).getEmpireID();
                if (empireID != null && empireID == mEmpire.getID()) {
                    numColonies ++;
                }
            }

            return numColonies;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            Star star = getStar(groupPosition);
            if (star == null) {
                return null;
            }

            // TODO: cache?
            int colonyIndex = 0;
            for (int i = 0; i < star.getColonies().size(); i++) {
                Integer empireID = ((Colony) star.getColonies().get(i)).getEmpireID();
                if (empireID != null && empireID == mEmpire.getID()) {
                    if (colonyIndex == childPosition) {
                        return star.getColonies().get(i);
                    }
                    colonyIndex ++;
                }
            }

            // Shouldn't get here...
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.colony_list_star_row, null);
            }

            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            TextView starName = (TextView) view.findViewById(R.id.star_name);
            TextView starType = (TextView) view.findViewById(R.id.star_type);
            TextView starGoodsDelta = (TextView) view.findViewById(R.id.star_goods_delta);
            TextView starGoodsTotal = (TextView) view.findViewById(R.id.star_goods_total);
            TextView starMineralsDelta = (TextView) view.findViewById(R.id.star_minerals_delta);
            TextView starMineralsTotal = (TextView) view.findViewById(R.id.star_minerals_total);

            Star star = getStar(groupPosition);
            if (star == null) {
                starIcon.setImageBitmap(null);
                starName.setText("");
                starType.setText("");
                starGoodsDelta.setText("");
                starGoodsTotal.setText("???");
                starMineralsDelta.setText("");
                starMineralsTotal.setText("???");
            } else {
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
                    if (empirePresence.getDeltaGoodsPerHour() == 0.0f
                            && empirePresence.getDeltaMineralsPerHour() == 0.0f) {
                        // this is so unlikely, it's probably because we didn't simulate.
                        needSimulation = true;
                    }
                }

                if (needSimulation || empirePresence == null) {
                    // if the star hasn't been simulated for > 5 minutes, schedule a simulation now and
                    // just display ??? for the various parameters
                    starGoodsDelta.setText("");
                    starGoodsTotal.setText("???");
                    starMineralsDelta.setText("");
                    starMineralsTotal.setText("???");
                    StarSimulationQueue.i.simulate(star);
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
            return view;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.colony_list_colony_row, null);
            }

            ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
            TextView colonyName = (TextView) view.findViewById(R.id.colony_name);
            TextView colonySummary = (TextView) view.findViewById(R.id.colony_summary);

            planetIcon.setImageBitmap(null);
            colonyName.setText("");
            colonySummary.setText("");

            Star star = getStar(groupPosition);
            if (star != null) {
                Colony colony = (Colony) getChild(groupPosition, childPosition);
                Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
                if (planet != null) {
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
            }

            return view;
        }

        private Star getStar(int index) {
            Star star = mStarCache.get(index);
            if (star == null) {
                star = mFetcher.getStar(index);
                if (star != null) {
                    mStarCache.put(index, star);
                }
            }
            return star;
        }
    }
}
