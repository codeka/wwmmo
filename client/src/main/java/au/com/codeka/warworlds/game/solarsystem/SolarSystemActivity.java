package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

import java.util.ArrayList;
import java.util.Locale;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.starfield.StarfieldFragment;
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
public class SolarSystemActivity extends BaseActivity {/*
  private DrawerLayout drawerLayout;
  private View drawer;
  private ActionBarDrawerToggle drawerToggle;
  private Integer starID;
  private Star star;
  private boolean waitingForStarToShow;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      starID = savedInstanceState.getInt("au.com.codeka.warworlds.StarID");
    }

    setContentView(R.layout.solarsystem_activity);
    setSupportActionBar(findViewById(R.id.toolbar));
    drawerLayout = findViewById(R.id.drawer_layout);
    drawer = findViewById(R.id.drawer);

    ListView searchList = findViewById(R.id.search_result);
    searchListAdapter = new SearchListAdapter(getLayoutInflater());
    searchList.setAdapter(searchListAdapter);

    searchList.setOnItemClickListener((parent, view, position, id) -> {
      Star star = (Star) searchListAdapter.getItem(position);
      if (star != null) {
        showStar(star.getID());
      }
    });

    drawerToggle =
        new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
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

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
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
    // Inflate the menu items for use in the action bar
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.solarsystem_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    switch (item.getItemId()) {
      case R.id.menu_locate:
        Intent intent = new Intent(this, StarfieldFragment.class);
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

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    StarManager.eventBus.register(eventHandler);

    ServerGreeter.waitForHello(this, (success, greeting) -> {
      if (!isResumed) {
        return;
      }

      if (starID == null) {
        Bundle extras = getIntent().getExtras();
        String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
        if (starKey != null) {
          showStar(Integer.parseInt(starKey));
        }
      } else {
        showStar(starID);
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
    searchListAdapter.setEmpireStarsFetcher(
        new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Everything, search));
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

    getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();

    drawerLayout.closeDrawer(drawer);
    synchronized (lastStars) {
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

*/
}
