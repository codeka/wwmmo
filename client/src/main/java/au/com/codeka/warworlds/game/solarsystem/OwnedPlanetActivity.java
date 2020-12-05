package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.WelcomeFragment;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class OwnedPlanetActivity extends BaseActivity {
  private Star star;
  private List<Colony> colonies;
  private ViewPager viewPager;
  private ColonyPagerAdapter colonyPagerAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    viewPager = new ViewPager(this);
    viewPager.setId(R.id.pager);
    setContentView(viewPager);

    colonyPagerAdapter = new ColonyPagerAdapter(getSupportFragmentManager());
    viewPager.setAdapter(colonyPagerAdapter);
  }

  public Star getStar() {
    return star;
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    ServerGreeter.waitForHello(this, (success, greeting) -> {
      if (!success) {
        startActivity(new Intent(this, WelcomeFragment.class));
      } else {
        String starKey = getIntent().getExtras().getString("au.com.codeka.warworlds.StarKey");
        StarManager.eventBus.register(eventHandler);
        Star star = StarManager.i.getStar(Integer.parseInt(starKey));
        if (star != null) {
          refresh(star);
        }
      }
    });
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarFetched(Star s) {
      if (star != null && !star.getKey().equals(s.getKey())) {
        return;
      }

      refresh(s);
    }
  };

  private void refresh(Star star) {
    boolean dataSetChanged = (star == null);
    this.star = star;

    colonies = new ArrayList<>();
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    for (BaseColony c : star.getColonies()) {
      if (c.getEmpireKey() != null && c.getEmpireKey().equals(myEmpire.getKey())) {
        colonies.add((Colony) c);
      }
    }
    Collections.sort(colonies, (lhs, rhs) -> lhs.getPlanetIndex() - rhs.getPlanetIndex());
    colonyPagerAdapter.notifyDataSetChanged();

    int initialPlanetIndex = colonies.get(0).getPlanetIndex();
    if (getIntent().getExtras() != null) {
      initialPlanetIndex = getIntent().getExtras().getInt("au.com.codeka.warworlds.PlanetIndex");
    }

    int colonyIndex = 0;
    for (Colony colony : colonies) {
      if (colony.getPlanetIndex() == initialPlanetIndex) {
        break;
      }
      colonyIndex++;
    }

    if (dataSetChanged) {
      viewPager.setCurrentItem(colonyIndex);
    }
  }

  public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
    public ColonyPagerAdapter(FragmentManager fm) {
      super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public Fragment getItem(int i) {
      Fragment fragment = new OwnedPlanetFragment();
      Bundle args = new Bundle();
      args.putInt("au.com.codeka.warworlds.PlanetIndex", colonies.get(i).getPlanetIndex());
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public int getCount() {
      if (colonies == null) {
        return 0;
      }

      return colonies.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return "Colony " + (position + 1);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);
//      if (colonies != null && colonies.size() > position) {
//        refreshColonyDetails(colonies.get(position));
//      }
    }
  }

}
