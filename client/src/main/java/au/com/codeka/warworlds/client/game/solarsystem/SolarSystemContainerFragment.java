package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.store.StarCursor;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Star;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base fragment for the solar system, which contains the drawer, and lets us switch between stars.
 * Each star is represented by a {@link SolarSystemFragment}.
 */
public class SolarSystemContainerFragment extends BaseFragment {
  private DrawerLayout drawerLayout;
  private View drawer;
  private ActionBarDrawerToggle drawerToggle;
  private Star star;
  private SearchListAdapter searchListAdapter;
  private boolean waitingForStarToShow;

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
    drawerLayout = (DrawerLayout) view.findViewById(R.id.drawer_layout);
    drawer = view.findViewById(R.id.drawer);

    ListView searchList = (ListView) view.findViewById(R.id.search_result);
    searchListAdapter = new SearchListAdapter(getLayoutInflater(savedInstanceState));
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
            getFragmentActivity().supportInvalidateOptionsMenu();
          }

          @Override
          public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            refreshTitle();
            getFragmentActivity().supportInvalidateOptionsMenu();
            searchListAdapter.onShow();
          }
        };
    drawerLayout.setDrawerListener(drawerToggle);

    final EditText searchBox = (EditText) view.findViewById(R.id.search_text);
    searchBox.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        performSearch(searchBox.getText().toString());
        return true;
      }
      return false;
    });

    ImageButton searchBtn = (ImageButton) view.findViewById(R.id.search_button);
    searchBtn.setOnClickListener(v -> performSearch(searchBox.getText().toString()));
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    drawerToggle.syncState();

    Bundle args = getArguments();
    if (args.getLong(SolarSystemFragment.STAR_ID_KEY) != 0) {
      Star star = StarManager.i.getStar(args.getLong(SolarSystemFragment.STAR_ID_KEY));
      showStar(star, args);
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public void onResume() {
    super.onResume();
    getFragmentActivity().getSupportActionBar().show();
  }

//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    // Inflate the menu items for use in the action bar
//    MenuInflater inflater = getMenuInflater();
//    inflater.inflate(R.menu.solarsystem_menu, menu);
 //   return super.onCreateOptionsMenu(menu);
 // }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

/*    switch (item.getItemId()) {
      case R.id.menu_locate:
        Intent intent = new Intent(this, StarfieldActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (star != null) {
          intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
          intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
          intent.putExtra("au.com.codeka.warworlds.OffsetX", star.getOffsetX());
          intent.putExtra("au.com.codeka.warworlds.OffsetY", star.getOffsetY());
          intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
        }
        startActivity(intent);
        break;
    }
*/
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPause() {
    super.onPause();
    getFragmentActivity().getSupportActionBar().hide();
//    StarManager.eventBus.unregister(eventHandler);
  }

  private void performSearch(String search) {
//    searchListAdapter.setEmpireStarsFetcher(
//        new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Everything, search));
  }

  public void showStar(Star star, @Nullable Bundle args) {
    this.star = star;
    refreshTitle();

    Fragment fragment;
    //if (star.classification == Star.CLASSIFICATION.WORMHOLE) {
    //  fragment = new WormholeFragment();
    //} else {
      fragment = new SolarSystemFragment();
    //}
    if (args == null) {
      args = SolarSystemFragment.createArguments(star.id);
    }
    fragment.setArguments(args);

    getFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();

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

//      if (searchListAdapter.getStarsFetcher() == null || searchListAdapter.getStarsFetcher()
 //         .hasStarID(star.getID())) {
 //       searchListAdapter.notifyDataSetChanged();
 //     }
    }
  };

  private static class SearchListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private StarCursor cursor;

    public SearchListAdapter(LayoutInflater inflater) {
      this.inflater = inflater;
    }

    /**
     * This should be called whenever the drawer is opened.
     */
    public void onShow() {
      if (cursor == null) {
        cursor = StarManager.i.getMyStars();
      }
    }

  /*  public void setEmpireStarsFetcher(EmpireStarsFetcher fetcher) {
      if (this.fetcher != null) {
        this.fetcher.eventBus.unregister(eventHandler);
      }
      this.fetcher = fetcher;
      this.fetcher.eventBus.register(eventHandler);
      this.fetcher.getStars(0, 20);
      notifyDataSetChanged();
    }*/

  //  public EmpireStarsFetcher getStarsFetcher() {
 //     return fetcher;
  //  }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public int getItemViewType(int position) {
      if (position == lastStars.size() - 1) {
        return 1;
      } else if (cursor != null && cursor.getSize() == 0) {
        return 2;
      } else {
        return 0;
      }
    }

    @Override
    public int getCount() {
      int count = lastStars.size() - 1;
      if (cursor != null) {
        if (cursor.getSize() == 0) {
          count += 2;
        } else {
          count += cursor.getSize() + 1; // +1 for the spacer view
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
      } else if (cursor.getSize() == 0) {
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
        } /*else if (cursor != null && cursor.getSize() == 0 && position >= lastStars.size()) {
          // if we don't have any stars yet, show a loading spinner
          view = inflater.inflate(R.layout.solarsystem_starlist_loading, parent, false);
        } */else {
          view = inflater.inflate(R.layout.solarsystem_starlist_row, parent, false);
        }
      }

//      if (position == lastStars.size() - 1 || (fetcher != null && fetcher.getNumStars() == 0
//          && position >= lastStars.size())) {
//        return view;
//      }

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

        if (storage != null) {
          starGoodsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
              storage.goods_delta_per_hour < 0 ? "-" : "+",
              Math.abs(Math.round(storage.goods_delta_per_hour))));
          if (storage.goods_delta_per_hour < 0) {
            starGoodsDelta.setTextColor(Color.RED);
          } else {
            starGoodsDelta.setTextColor(Color.GREEN);
          }
          starGoodsTotal.setText(String
              .format(Locale.ENGLISH, "%d / %d", Math.round(storage.total_goods),
                  Math.round(storage.max_goods)));

          starMineralsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
              storage.minerals_delta_per_hour < 0 ? "-" : "+",
              Math.abs(Math.round(storage.minerals_delta_per_hour))));
          if (storage.minerals_delta_per_hour < 0) {
            starMineralsDelta.setTextColor(Color.RED);
          } else {
            starMineralsDelta.setTextColor(Color.GREEN);
          }
          starMineralsTotal.setText(String
              .format(Locale.ENGLISH, "%d / %d", Math.round(storage.total_minerals),
                  Math.round(storage.max_minerals)));
        }
      }
      return view;
    }

    private Object eventHandler = new Object() {
//      @EventHandler
//      public void onEmpireStarsFetched(EmpireStarsFetcher.StarsFetchedEvent event) {
//        notifyDataSetChanged();
 //     }
    };
  }
}
