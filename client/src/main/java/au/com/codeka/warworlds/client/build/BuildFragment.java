package au.com.codeka.warworlds.client.build;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.TabbedBaseFragment;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.client.world.ImageHelper;
import au.com.codeka.warworlds.client.world.StarManager;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
public class BuildFragment extends BaseFragment {
  private static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private List<Colony> colonies;
  private Colony initialColony;
  private ColonyPagerAdapter colonyPagerAdapter;

  private ViewPager viewPager;
  private ImageView planetIcon;
  private TextView planetName;
  private TextView buildQueueDescription;

  public static Bundle createArguments(long starId, int planetIndex) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starId);
    args.putInt(PLANET_INDEX_KEY, planetIndex);
    return args;
  }

  @Override
  public int getViewResourceId() {
    return R.layout.frag_build;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    colonyPagerAdapter = new ColonyPagerAdapter(getFragmentManager());
    viewPager = (ViewPager) view.findViewById(R.id.pager);
    viewPager.setAdapter(colonyPagerAdapter);
    planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
    planetName = (TextView) view.findViewById(R.id.planet_name);
    buildQueueDescription = (TextView) view.findViewById(R.id.build_queue_description);
  }

  @Override
  public void onResume(){
    super.onResume();

    Star star = StarManager.i.getStar(getArguments().getLong(STAR_ID_KEY));
    App.i.getEventBus().register(eventHandler);
    updateStar(star);
  }

  @Override
  public void onPause() {
    super.onPause();
    App.i.getEventBus().unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star != null && star.id.equals(s.id)) {
        updateStar(s);
      }
    }
  };

  private void updateStar(Star s) {
    boolean dataSetChanged = (star == null);

    star = s;
    colonies = new ArrayList<>();
    Empire myEmpire = EmpireManager.i.getMyEmpire();
    for (Planet planet : star.planets) {
      if (planet.colony != null && planet.colony.empire_id != null
          && myEmpire != null && planet.colony.empire_id.equals(myEmpire.id)) {
        colonies.add(planet.colony);
      }
    }

    if (initialColony != null) {
      int colonyIndex = 0;
      for (Colony colony : colonies) {
        if (colony.id.equals(initialColony.id)) {
          break;
        }
        colonyIndex++;
      }

      viewPager.setCurrentItem(colonyIndex);
      initialColony = null;
    }

    if (dataSetChanged) {
      colonyPagerAdapter.notifyDataSetChanged();
    }
  }

  private void refreshColonyDetails(Colony colony) {
    Planet planet = null;
    for (Planet p : star.planets) {
      if (p.colony != null && p.colony.id.equals(colony.id)) {
        planet = p;
      }
    }
    Preconditions.checkNotNull(planet);

    Picasso.with(getContext())
        .load(ImageHelper.getPlanetImageUrl(getContext(), star, planet.index, 64, 64))
        .into(planetIcon);
    planetName.setText(String.format(Locale.US, "%s %s", star.name,
        RomanNumeralFormatter.format(planet.index + 1)));

    int buildQueueLength = 0;
    //for (BaseBuildRequest br : star.getBuildRequests()) {
    //  if (br.getColonyKey().equals(colony.getKey())) {
    //    buildQueueLength++;
    //  }
    //}
    if (buildQueueLength == 0) {
      buildQueueDescription.setText("Build queue: idle");
    } else {
      buildQueueDescription.setText(String.format(Locale.ENGLISH,
          "Build queue: %d", buildQueueLength));
    }
  }

  public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
    public ColonyPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      Fragment fragment = new TabFragment();
      int planetIndex = -1;
      for (Planet planet : star.planets) {
        if (planet.colony != null && planet.colony.id.equals(colonies.get(i).id)) {
          planetIndex = planet.index;
          break;
        }
      }
      fragment.setArguments(createArguments(star.id, planetIndex));
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
      if (colonies != null && colonies.size() > position) {
        refreshColonyDetails(colonies.get(position));
      }
    }
  }

  public static class TabFragment extends TabbedBaseFragment {
    @Override
    protected void createTabs() {
      Bundle args = getArguments();
      getTabManager().addTab(getContext(),
          new TabInfo(this, "Buildings", BuildingsFragment.class, args));
      getTabManager().addTab(getContext(),
          new TabInfo(this, "Ships", ShipsFragment.class, args));
      getTabManager().addTab(getContext(),
          new TabInfo(this, "Queue", QueueFragment.class, args));
    }
  }

  public static class BaseTabFragment extends BaseFragment {
    private Star star;
    private Integer planetIndex;

    protected Star getStar() {
      if (star == null) {
        star = StarManager.i.getStar(getArguments().getLong(STAR_ID_KEY));
      }
      return star;
    }

    protected Colony getColony() {
      if (planetIndex == null) {
        planetIndex = getArguments().getInt(PLANET_INDEX_KEY);
      }

      Star star = getStar();
      if (star.planets == null) {
        return null;
      }
      return star.planets.get(planetIndex).colony;
    }
  }
}
