package au.com.codeka.warworlds.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
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

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;

import java.util.ArrayList;
import java.util.Locale;

import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
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
 * Manages the content of the drawer.
 */
public class DrawerController {
  // We keep the last 5 stars you've visited in an LRU cache so we can display them at the top
  // of the search list (note we actually keep 6 but ignore the most recent one, which is always
  // "this star").
  private final ArrayList<Star> lastStars = new ArrayList<>();
  private final int LAST_STARS_MAX_SIZE = 6;

  private final NavController navController;
  private final DrawerLayout drawerLayout;
  private final SearchListAdapter searchListAdapter;

  private Integer waitingForStarID = null;

  public DrawerController(
      NavController navController, DrawerLayout drawerLayout, ViewGroup drawerContent) {
    this.navController = navController;
    this.drawerLayout = drawerLayout;

    final ListView searchList = drawerContent.findViewById(R.id.search_result);
    searchListAdapter = new SearchListAdapter(LayoutInflater.from(drawerContent.getContext()));
    searchList.setAdapter(searchListAdapter);

    searchList.setOnItemClickListener((parent, view, position, id) -> {
      Star star = (Star) searchListAdapter.getItem(position);
      if (star != null) {
        showStar(star.getID());
      }
    });


    final EditText searchBox = drawerContent.findViewById(R.id.search_text);
    searchBox.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        performSearch(searchBox.getText().toString());
        return true;
      }
      return false;
    });

    ImageButton searchBtn = drawerContent.findViewById(R.id.search_button);
    searchBtn.setOnClickListener(v -> performSearch(searchBox.getText().toString()));

    drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
      @Override
      public void onDrawerOpened(@NonNull View drawerView) {
        searchListAdapter.onShow();
      }
    });
  }

  public void start() {
    StarManager.eventBus.register(eventHandler);
  }

  public void stop() {
    StarManager.eventBus.unregister(eventHandler);
  }

  private void performSearch(String search) {
    searchListAdapter.setEmpireStarsFetcher(
        new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Everything, search));
  }

  public void showStar(Integer starID) {
    Star star = StarManager.i.getStar(starID);
    if (star == null) {
      // we need the star summary in order to know whether it's a wormhole or normal star.
      waitingForStarID = starID;
      return;
    }

    if (star.getStarType().getType() == BaseStar.Type.Wormhole) {
      // TODO
    } else {
      Bundle args = new Bundle();
      args.putInt("au.com.codeka.warworlds.StarID", starID);
      navController.navigate(R.id.solarSystemFragment, args);
    }

    drawerLayout.close();

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

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star star) {
      if (waitingForStarID != null && star.getID() == waitingForStarID) {
        waitingForStarID = null;
        showStar(star.getID());
      }

      if (searchListAdapter.getStarsFetcher() == null || searchListAdapter.getStarsFetcher()
          .hasStarID(star.getID())) {
        searchListAdapter.notifyDataSetChanged();
      }
    }
  };

  private class SearchListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private EmpireStarsFetcher fetcher;

    public SearchListAdapter(LayoutInflater inflater) {
      this.inflater = inflater;
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

    public void onShow() {
      if (fetcher == null) {
        setEmpireStarsFetcher(new EmpireStarsFetcher(EmpireStarsFetcher.Filter.Everything, null));
      }
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
          view.setLayoutParams(
              new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20));
        } else if (fetcher != null && fetcher.getNumStars() == 0 && position >= lastStars.size()) {
          // if we don't have any stars yet, show a loading spinner
          view = inflater.inflate(R.layout.solarsystem_starlist_loading, parent, false);
        } else {
          view = inflater.inflate(R.layout.solarsystem_starlist_row, parent, false);
        }
      }

      if (position == lastStars.size() - 1 || (fetcher != null && fetcher.getNumStars() == 0
          && position >= lastStars.size())) {
        return view;
      }

      Star star = (Star) getItem(position);
      ImageView starIcon = view.findViewById(R.id.star_icon);
      TextView starName = view.findViewById(R.id.star_name);
      TextView starType = view.findViewById(R.id.star_type);
      TextView starGoodsDelta = view.findViewById(R.id.star_goods_delta);
      TextView starGoodsTotal = view.findViewById(R.id.star_goods_total);
      TextView starMineralsDelta = view.findViewById(R.id.star_minerals_delta);
      TextView starMineralsTotal = view.findViewById(R.id.star_minerals_total);

      if (starIcon == null) {
        throw new RuntimeException(position + " " + view.toString());
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
        int imageSize = (int) (star.getSize() * star.getStarType().getImageScale() * 2);
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

        if (StarSimulationQueue.needsSimulation(star) || (
            empirePresence == null && star.getWormholeExtra() == null)) {
          // if the star hasn't been simulated for > 5 minutes, schedule a simulation
          // now and just display ??? for the various parameters
          starGoodsDelta.setText("");
          starGoodsTotal.setText("???");
          starMineralsDelta.setText("");
          starMineralsTotal.setText("???");
          StarSimulationQueue.i.simulate(star, false);
        } else if (empirePresence != null) {
          starGoodsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
              empirePresence.getDeltaGoodsPerHour() < 0 ? "-" : "+",
              Math.abs(Math.round(empirePresence.getDeltaGoodsPerHour()))));
          if (empirePresence.getDeltaGoodsPerHour() < 0) {
            starGoodsDelta.setTextColor(Color.RED);
          } else {
            starGoodsDelta.setTextColor(Color.GREEN);
          }
          starGoodsTotal.setText(String
              .format(Locale.ENGLISH, "%d / %d", Math.round(empirePresence.getTotalGoods()),
                  Math.round(empirePresence.getMaxGoods())));

          starMineralsDelta.setText(String.format(Locale.ENGLISH, "%s%d/hr",
              empirePresence.getDeltaMineralsPerHour() < 0 ? "-" : "+",
              Math.abs(Math.round(empirePresence.getDeltaMineralsPerHour()))));
          if (empirePresence.getDeltaMineralsPerHour() < 0) {
            starMineralsDelta.setTextColor(Color.RED);
          } else {
            starMineralsDelta.setTextColor(Color.GREEN);
          }
          starMineralsTotal.setText(String
              .format(Locale.ENGLISH, "%d / %d", Math.round(empirePresence.getTotalMinerals()),
                  Math.round(empirePresence.getMaxMinerals())));
        }
      }
      return view;
    }

    private final Object eventHandler = new Object() {
      @EventHandler
      public void onEmpireStarsFetched(EmpireStarsFetcher.StarsFetchedEvent event) {
        notifyDataSetChanged();
      }
    };
  }
}
