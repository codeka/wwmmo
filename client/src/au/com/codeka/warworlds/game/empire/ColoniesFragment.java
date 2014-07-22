package au.com.codeka.warworlds.game.empire;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
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
    private StarsListAdapter adapter;
    private EmpireStarsFetcher fetcher;

    public ColoniesFragment() {
        fetcher = new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Colonies, null);
        // fetch the first few stars
        fetcher.getStars(0, 20);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.empire_colonies_tab, container, false);
        ExpandableListView starsList = (ExpandableListView) v.findViewById(R.id.stars);
        adapter = new ColonyStarsListAdapter(inflater, fetcher);
        starsList.setAdapter(adapter);

        starsList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                Star star = (Star) adapter.getGroup(groupPosition);
                Colony colony = (Colony) adapter.getChild(groupPosition, childPosition);
                Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
                // end this activity, go back to the starfield and navigate to the given colony

                Intent intent = new Intent();
                intent.putExtra("au.com.codeka.warworlds.Result",
                        EmpireActivity.EmpireActivityResult.NavigateToPlanet.getValue());
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                intent.putExtra("au.com.codeka.warworlds.StarOffsetX", star.getOffsetX());
                intent.putExtra("au.com.codeka.warworlds.StarOffsetY", star.getOffsetY());
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
                intent.putExtra("au.com.codeka.warworlds.PlanetIndex", planet.getIndex());
                getActivity().setResult(EmpireActivity.RESULT_OK, intent);
                getActivity().finish();

                return false;
            }
        });

        final EditText searchBox = (EditText) v.findViewById(R.id.search_text);
        searchBox.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(searchBox.getText().toString());
                    return true;
                }
                return false;
            }
        });

        ImageButton searchBtn = (ImageButton) v.findViewById(R.id.search_button);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch(searchBox.getText().toString());
            }
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        StarManager.eventBus.register(eventHandler);
    }

    @Override
    public void onStop() {
        super.onStop();
        StarManager.eventBus.unregister(eventHandler);
    }

    private void performSearch(String search) {
        fetcher = new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Colonies, search);
        adapter.updateFetcher(fetcher);
    }

    private Object eventHandler = new Object() {
        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onStarUpdated(Star star) {
            adapter.notifyDataSetChanged();
        }
    };

    public static class ColonyStarsListAdapter extends StarsListAdapter {
        private LayoutInflater inflater;
        private MyEmpire empire;

        public ColonyStarsListAdapter(LayoutInflater inflater, EmpireStarsFetcher fetcher) {
            super(fetcher);
            this.inflater = inflater;
            empire = EmpireManager.i.getEmpire();
        }

        @Override
        public int getNumChildren(Star star) {
            int numColonies = 0;
            for (int i = 0; i < star.getColonies().size(); i++) {
                Integer empireID = ((Colony) star.getColonies().get(i)).getEmpireID();
                if (empireID != null && empireID == empire.getID()) {
                    numColonies ++;
                }
            }

            return numColonies;
        }

        @Override
        public Object getChild(Star star, int index) {
            int colonyIndex = 0;
            for (int i = 0; i < star.getColonies().size(); i++) {
                Integer empireID = ((Colony) star.getColonies().get(i)).getEmpireID();
                if (empireID != null && empireID == empire.getID()) {
                    if (colonyIndex == index) {
                        return star.getColonies().get(i);
                    }
                    colonyIndex ++;
                }
            }

            // Shouldn't get here...
            return null;
        }

        @Override
        public View getStarView(Star star, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.empire_colony_list_star_row, parent, false);
            }

            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            TextView starName = (TextView) view.findViewById(R.id.star_name);
            TextView starType = (TextView) view.findViewById(R.id.star_type);
            TextView starGoodsDelta = (TextView) view.findViewById(R.id.star_goods_delta);
            TextView starGoodsTotal = (TextView) view.findViewById(R.id.star_goods_total);
            TextView starMineralsDelta = (TextView) view.findViewById(R.id.star_minerals_delta);
            TextView starMineralsTotal = (TextView) view.findViewById(R.id.star_minerals_total);

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

                if (StarSimulationQueue.needsSimulation(star) || empirePresence == null) {
                    // if the star hasn't been simulated for > 5 minutes, schedule a simulation
                    // now and just display ??? for the various parameters
                    starGoodsDelta.setText("");
                    starGoodsTotal.setText("???");
                    starMineralsDelta.setText("");
                    starMineralsTotal.setText("???");
                    StarSimulationQueue.i.simulate(star, false);
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
        public View getChildView(Star star, int index, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.empire_colony_list_colony_row, parent, false);
            }

            ImageView planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
            TextView colonyName = (TextView) view.findViewById(R.id.colony_name);
            TextView colonySummary = (TextView) view.findViewById(R.id.colony_summary);

            planetIcon.setImageBitmap(null);
            colonyName.setText("");
            colonySummary.setText("");

            if (star != null) {
                Colony colony = (Colony) getChild(star, index);
                Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
                if (planet != null) {
                    Sprite sprite = PlanetImageManager.getInstance().getSprite(planet);
                    planetIcon.setImageDrawable(new SpriteDrawable(sprite));

                    colonyName.setText(String.format("%s %s", star.getName(),
                            RomanNumeralFormatter.format(planet.getIndex())));

                    if (star.getLastSimulation().compareTo(DateTime.now(DateTimeZone.UTC)
                            .minusMinutes(5)) < 0) {
                        // if the star hasn't been simulated for > 5 minutes, just display ???
                        // for the various parameters (a simulation will be scheduled)
                        colonySummary.setText("Pop: ?");
                    } else {
                        colonySummary.setText(String.format("Pop: %d",
                                (int) colony.getPopulation()));
                    }
                }
            }

            return view;
        }
    }
}
