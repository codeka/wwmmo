package au.com.codeka.warworlds.client.game.solarsystem;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.store.StarCursor;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Base fragment for the solar system, which contains the drawer, and lets us switch between stars.
 * Each star is represented by a {@link SolarSystemFragment}.
 */
public class SolarSystemContainerFragment extends BaseFragment {
  private static final Log log = new Log("SolarSystemContainerFra");

  private DrawerLayout drawerLayout;
  private View drawer;
  private ActionBarDrawerToggle drawerToggle;
  private Star star;
  private SearchListAdapter searchListAdapter;
  private boolean waitingForStarToShow;

  private View view;

  // We keep the last 5 stars you've visited in an LRU cache so we can display them at the top
  // of the search list (note we actually keep 6 but ignore the most recent one, which is always
  // "this star").
  private static final ArrayList<Star> lastStars = new ArrayList<>();
  private static final int LAST_STARS_MAX_SIZE = 6;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_solarsystem_container;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    this.view = view;
    drawerLayout = view.findViewById(R.id.drawer_layout);
    drawer = view.findViewById(R.id.drawer);

    ListView searchList = view.findViewById(R.id.search_result);
    searchListAdapter = new SearchListAdapter(
        LayoutInflater.from(getContext()));
    searchList.setAdapter(searchListAdapter);

    searchList.setOnItemClickListener((parent, v, position, id) -> {
      Star s = (Star) searchListAdapter.getItem(position);
      if (s != null) {
        showStar(s, null);
      }
    });

    drawerToggle =
        new ActionBarDrawerToggle(
            getFragmentActivity(),
            drawerLayout,
            R.string.drawer_open,
            R.string.drawer_close) {
          @Override
          public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            refreshTitle();
          }

          @Override
          public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            refreshTitle();
            searchListAdapter.setCursor(StarManager.i.getMyStars());
          }
        };
    drawerLayout.setDrawerListener(drawerToggle);

    final EditText searchBox = view.findViewById(R.id.search_text);
    searchBox.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        performSearch(s.toString());
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });

    ImageButton searchBtn = view.findViewById(R.id.search_button);
    searchBtn.setOnClickListener(v -> performSearch(searchBox.getText().toString()));
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    drawerToggle.syncState();

    Bundle args = getArguments();
    if (args.getLong(SolarSystemFragment.STAR_ID_KEY) != 0) {
      Star star = StarManager.i.getStar(args.getLong(SolarSystemFragment.STAR_ID_KEY));
      showStar(star, args);
    }
  }
/*
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }
*/
  @Override
  public void onResume() {
    super.onResume();
    App.i.getEventBus().register(eventHandler);

    log.debug("SolarSystemContainerFragment.onResume");

    ActionBar actionBar = checkNotNull(getFragmentActivity().getSupportActionBar());
    actionBar.show();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);
  }
/*
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
*/
  @Override
  public void onPause() {
    super.onPause();

    log.debug("SolarSystemContainerFragment.onPause");
    ActionBar actionBar = checkNotNull(getFragmentActivity().getSupportActionBar());
    actionBar.hide();

    App.i.getEventBus().unregister(eventHandler);
  }

  private void performSearch(String search) {
    searchListAdapter.setCursor(StarManager.i.searchMyStars(search));
  }

  public void showStar(Star star, @Nullable Bundle args) {
    this.star = star;
    refreshTitle();

    // TODO: current fragment? hide!
    ((ViewGroup) view.findViewById(R.id.content)).removeAllViews();

    BaseFragment fragment;
    //if (star.classification == Star.CLASSIFICATION.WORMHOLE) {
    //  fragment = new WormholeFragment();
    //} else {
      fragment = new SolarSystemFragment();
    //}
    if (args == null) {
      args = SolarSystemFragment.createArguments(star.id);
    }
    fragment.setArguments(args);

    fragment.onCreate(null);
    View childView =
        fragment.onCreateView(LayoutInflater.from(getContext()), (ViewGroup) view, null);
    ((ViewGroup) view.findViewById(R.id.content)).addView(childView);
    fragment.onStart();
    fragment.onResume();

    drawerLayout.closeDrawer(drawer);
    synchronized (lastStars) {
      for (int i = 0; i < lastStars.size(); i++) {
        if (lastStars.get(i).id.equals(star.id)) {
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
    ActionBar actionBar = checkNotNull(getFragmentActivity().getSupportActionBar());
    log.debug("Refreshing title. isDrawerOpen=%s star=%s actionBar.isShowing=%s",
        drawerLayout.isDrawerOpen(drawer) ? "true" : "false",
        star == null ? "??" : star.name,
        actionBar.isShowing() ? "true" : "false");

    if (drawerLayout.isDrawerOpen(drawer)) {
      actionBar.setTitle("Star Search");
    } else if (star == null) {
      actionBar.setTitle("Star Name");
    } else {
      actionBar.setTitle(star.name);
    }
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (s.id.equals(star.id)) {
        star = s;
        if (waitingForStarToShow) {
          waitingForStarToShow = false;
          showStar(star, null);
        } else {
          ActionBar actionBar = checkNotNull(getFragmentActivity().getSupportActionBar());
          actionBar.setTitle(star.name);
        }
      }
    }
  };

  private static class SearchListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private StarCursor cursor;

    private final int VIEW_TYPE_STAR = 0;
    private final int VIEW_TYPE_SEPARATOR = 1;
    private final int NUM_VIEW_TYPES = 2;

    public SearchListAdapter(LayoutInflater inflater) {
      this.inflater = inflater;
    }

    /** Sets the {@link StarCursor} that we'll use to display stars. */
    public void setCursor(StarCursor cursor) {
      this.cursor = checkNotNull(cursor);
      notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
      return NUM_VIEW_TYPES;
    }

    @Override
    public int getItemViewType(int position) {
      if (position == lastStars.size() - 1) {
        return VIEW_TYPE_SEPARATOR;
      } else {
        return VIEW_TYPE_STAR;
      }
    }

    @Override
    public int getCount() {
      int count = lastStars.size() - 1;
      if (cursor != null) {
        count += cursor.getSize() + 1; // +1 for the spacer view
      }
      return count;
    }

    @Override
    public Object getItem(int position) {
      if (position < lastStars.size() - 1) {
        return lastStars.get(position + 1);
      } else if (position == lastStars.size() - 1) {
        return null;
      } else {
        return cursor.getValue(position - lastStars.size());
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
          view.setLayoutParams(
              new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
        } else {
          view = inflater.inflate(R.layout.solarsystem_starlist_row, parent, false);
        }
      }

      if (position == lastStars.size() - 1) {
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
        Picasso.with(view.getContext())
            .load(ImageHelper.getStarImageUrl(inflater.getContext(), star, 36, 36))
            .into(starIcon);
        starName.setText(star.name);
        starType.setText(star.classification.toString());

        Empire myEmpire = EmpireManager.i.getMyEmpire();
        EmpireStorage storage = null;
        for (int i = 0; i < star.empire_stores.size(); i++) {
          if (star.empire_stores.get(i).empire_id != null
              && star.empire_stores.get(i).empire_id.equals(myEmpire.id)) {
            storage = star.empire_stores.get(i);
            break;
          }
        }

        if (storage == null) {
          starGoodsDelta.setText("");
          starGoodsTotal.setText("");
          starMineralsDelta.setText("");
          starMineralsTotal.setText("");
        } else {
          starGoodsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
              storage.goods_delta_per_hour < 0 ? "-" : "+",
              Math.abs(Math.round(storage.goods_delta_per_hour))));
          if (storage.goods_delta_per_hour < 0) {
            starGoodsDelta.setTextColor(Color.RED);
          } else {
            starGoodsDelta.setTextColor(Color.GREEN);
          }
          starGoodsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
              Math.round(storage.total_goods),
              Math.round(storage.max_goods)));

          starMineralsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
              storage.minerals_delta_per_hour < 0 ? "-" : "+",
              Math.abs(Math.round(storage.minerals_delta_per_hour))));
          if (storage.minerals_delta_per_hour < 0) {
            starMineralsDelta.setTextColor(Color.RED);
          } else {
            starMineralsDelta.setTextColor(Color.GREEN);
          }
          starMineralsTotal.setText(String.format(Locale.ENGLISH, "%d / %d",
              Math.round(storage.total_minerals),
              Math.round(storage.max_minerals)));
        }
      }
      return view;
    }
  }
}
