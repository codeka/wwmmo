package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Locale;

import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.wormhole.WormholeFragment;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpirePresence;
import au.com.codeka.warworlds.model.EmpireStarsFetcher;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSimulationQueue;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends BaseActivity {
    private DrawerLayout drawerLayout;
    private View drawer;
    private ActionBarDrawerToggle drawerToggle;
    private Integer starID;
    private Star star;
    private SearchListAdapter searchListAdapter;
    private boolean waitingForStarToShow;

    // We keep the last 5 stars you've visited in an LRU cache so we can display them at the top
    // of the search list (note we actually keep 6 but ignore the most recent one, which is always
    // "this star").
    private static final ArrayList<Star> lastStars = new ArrayList<>();
    private static final int LAST_STARS_MAX_SIZE = 6;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            starID = savedInstanceState.getInt("au.com.codeka.warworlds.StarID");
        }

        setContentView(R.layout.solarsystem_activity);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer = findViewById(R.id.drawer);

        // We have to offset the drawerLayout a bit because the action bar will be covering it.
        final TypedArray styledAttributes = getTheme().obtainStyledAttributes(
            new int[] { android.R.attr.actionBarSize });
        int actionBarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        ((FrameLayout.LayoutParams) drawerLayout.getLayoutParams()).topMargin = actionBarHeight;

        ListView searchList = (ListView) findViewById(R.id.search_result);
        searchListAdapter = new SearchListAdapter(getLayoutInflater());
        searchList.setAdapter(searchListAdapter);

        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Star star = (Star) searchListAdapter.getItem(position);
                if (star != null) {
                    showStar(star.getID());
                }
            }
        });

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                refreshTitle();
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                refreshTitle();
                supportInvalidateOptionsMenu();
                searchListAdapter.onShow();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        final EditText searchBox = (EditText) findViewById(R.id.search_text);
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

        ImageButton searchBtn = (ImageButton) findViewById(R.id.search_button);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch(searchBox.getText().toString());
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /** We want an action bar, so we override this to return true. */
    @Override
    protected boolean wantsActionBar() {
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
          return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResumeFragments() {
        super.onResumeFragments();

        StarManager.eventBus.register(eventHandler);

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (starID == null) {
                    Bundle extras = getIntent().getExtras();
                    String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
                    if (starKey != null) {
                        showStar(Integer.parseInt(starKey));
                    }
                } else {
                    showStar(starID);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putInt("au.com.codeka.warworlds.StarID", starID);
    }

    @Override
    public void onPause() {
        super.onPause();
        StarManager.eventBus.unregister(eventHandler);
    }

    @Override
    public void onBackPressed() {
        Star star = StarManager.i.getStar(starID);
        Intent intent = new Intent();
        if (star != null) {
            intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
            intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
            intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
        }
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    private void performSearch(String search) {
        searchListAdapter.setEmpireStarsFetcher(new EmpireStarsFetcher(
                EmpireStarsFetcher.Filter.Everything, search));
    }

    public void showStar(Integer starID) {
        this.starID = starID;
        star = StarManager.i.getStar(starID);
        if (star == null) {
            // we need the star summary in order to know whether it's a wormhole or normal star.
            waitingForStarToShow = true;
            return;
        }
        refreshTitle();

        Fragment fragment;
        if (star.getStarType().getType() == BaseStar.Type.Wormhole) {
            fragment = new WormholeFragment();
        } else {
            fragment = new SolarSystemFragment();
        }
        Bundle args = new Bundle();
        args.putLong("au.com.codeka.warworlds.StarID", starID);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.content, fragment)
                                   .commit();

        drawerLayout.closeDrawer(drawer);
        synchronized(lastStars) {
            for (int i = 0; i < lastStars.size(); i++) {
                if (lastStars.get(i).getID() == star.getID()) {
                    lastStars.remove(i);
                    break;
                }
            }
            lastStars.add(0, star);
            while (lastStars.size() > LAST_STARS_MAX_SIZE) {
                lastStars.remove(lastStars.size() - 1);
            }
        }
        searchListAdapter.notifyDataSetChanged();
    }

    private void refreshTitle() {
        if (drawerLayout.isDrawerOpen(drawer)) {
            getSupportActionBar().setTitle("Star Search");
        } else if (star == null) {
            getSupportActionBar().setTitle("Star Name");
        } else {
            getSupportActionBar().setTitle(star.getName());
        }
    }

    private Object eventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            if (star.getID() == starID) {
                SolarSystemActivity.this.star = star;
                if (waitingForStarToShow) {
                    waitingForStarToShow = false;
                    showStar(starID);
                } else {
                    getSupportActionBar().setTitle(star.getName());
                }
            }

            if (searchListAdapter.getStarsFetcher() == null
                || searchListAdapter.getStarsFetcher().hasStarID(star.getID())) {
                searchListAdapter.notifyDataSetChanged();
            }
        }
    };

    private static class SearchListAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private EmpireStarsFetcher fetcher;

        public SearchListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        /** This should be called whenever the drawer is opened. */
        public void onShow() {
            if (fetcher == null) {
                setEmpireStarsFetcher(new EmpireStarsFetcher(
                        EmpireStarsFetcher.Filter.Everything, null));
            }
        }

        public void setEmpireStarsFetcher(EmpireStarsFetcher fetcher) {
            if (this.fetcher != null) {
                this.fetcher.eventBus.unregister(eventHandler);
            }
            this.fetcher = fetcher;
            this.fetcher.eventBus.register(eventHandler);
            this.fetcher.getStars(0, 20);
            notifyDataSetChanged();
        }

        public EmpireStarsFetcher getStarsFetcher() {
            return fetcher;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == lastStars.size() - 1) {
                return 1;
            } else if (fetcher != null && fetcher.getNumStars() == 0) {
                return 2;
            } else {
                return 0;
            }
        }

        @Override
        public int getCount() {
            int count = lastStars.size() - 1;
            if (fetcher != null) {
                if (fetcher.getNumStars() == 0) {
                    count += 2;
                } else {
                    count += fetcher.getNumStars() + 1; // +1 for the spacer view
                }
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            if (position < lastStars.size()) {
                return lastStars.get(position + 1);
            } else if (position == lastStars.size() - 1) {
                return null;
            } else if (fetcher.getNumStars() == 0) {
                return null;
            } else {
                return fetcher.getStar(position - lastStars.size());
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                if (position == lastStars.size() - 1) {
                    // it's just a spacer
                    view = new View(inflater.getContext());
                    view.setLayoutParams(new AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 20));
                } else if (fetcher != null && fetcher.getNumStars() == 0
                        && position >= lastStars.size()) {
                    // if we don't have any stars yet, show a loading spinner
                    view = inflater.inflate(R.layout.solarsystem_starlist_loading, parent, false);
                } else {
                    view = inflater.inflate(R.layout.solarsystem_starlist_row, parent, false);
                }
            }

            if (position == lastStars.size() -1
                    || (fetcher != null && fetcher.getNumStars() == 0
                        && position >= lastStars.size())) {
                return view;
            }

            Star star = (Star) getItem(position);
            ImageView starIcon = (ImageView) view.findViewById(R.id.star_icon);
            TextView starName = (TextView) view.findViewById(R.id.star_name);
            TextView starType = (TextView) view.findViewById(R.id.star_type);
            TextView starGoodsDelta = (TextView) view.findViewById(R.id.star_goods_delta);
            TextView starGoodsTotal = (TextView) view.findViewById(R.id.star_goods_total);
            TextView starMineralsDelta = (TextView) view.findViewById(R.id.star_minerals_delta);
            TextView starMineralsTotal = (TextView) view.findViewById(R.id.star_minerals_total);

            if (starIcon == null) {
                throw new RuntimeException(Integer.toString(position) + " " + view.toString());
            }

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
                    StarSimulationQueue.i.simulate((Star) star, false);
                } else if (empirePresence != null) {
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

        private Object eventHandler = new Object() {
            @EventHandler
            public void onEmpireStarsFetched(EmpireStarsFetcher.StarsFetchedEvent event) {
                notifyDataSetChanged();
            }
        };
    }
}
