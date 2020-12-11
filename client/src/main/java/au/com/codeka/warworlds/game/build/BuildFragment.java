package au.com.codeka.warworlds.game.build;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
public class BuildFragment extends BaseFragment {
  private Star star;
  private List<Colony> colonies;
  private ViewPager viewPager;
  private ColonyPagerAdapter colonyPagerAdapter;
  private boolean firstRefresh = true;

  private BuildFragmentArgs args;

  public Star getStar() {
    return star;
  }

  @Nullable
  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
          @Nullable Bundle savedInstanceState) {
    viewPager = new ViewPager(requireContext());
    viewPager.setId(R.id.pager);
    return viewPager;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    args = BuildFragmentArgs.fromBundle(requireArguments());
    viewPager.setBackgroundColor(view.getResources().getColor(R.color.default_background));

    colonyPagerAdapter = new ColonyPagerAdapter(getChildFragmentManager());
    viewPager.setAdapter(colonyPagerAdapter);
  }

  @Override
  public void onStart() {
    super.onStart();
    ServerGreeter.waitForHello(requireActivity(), (success, greeting) -> {
      StarManager.eventBus.register(eventHandler);
      Star star = StarManager.i.getStar(args.getStarID());
      if (star != null) {
        updateStar(star);
      }
    });
  }

  @Override
  public void onStop() {
    super.onStop();
    StarManager.eventBus.unregister(eventHandler);
  }

  private void refreshColonyDetails(Colony colony) {
    ActionBar actionBar = requireMainActivity().requireSupportActionBar();
    actionBar.setTitle(
        String.format(
            "%s %s", star.getName(), RomanNumeralFormatter.format(colony.getPlanetIndex())));

    int buildQueueLength = 0;
    for (BaseBuildRequest br : star.getBuildRequests()) {
      if (br.getColonyKey().equals(colony.getKey())) {
        buildQueueLength++;
      }
    }
    if (buildQueueLength == 0) {
      actionBar.setSubtitle("Build queue: idle");
    } else {
      actionBar.setSubtitle(String.format(Locale.ENGLISH, "Build queue: %d", buildQueueLength));
    }

    Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
    Sprite planetSprite = PlanetImageManager.getInstance().getSprite(planet);
    actionBar.setIcon(new SpriteDrawable(planetSprite));
  }

  public Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (s.getID() == args.getStarID()) {
        updateStar(s);
      }
    }
  };

  private void updateStar(Star s) {
    boolean dataSetChanged = (star == null);

    star = s;
    colonies = new ArrayList<>();
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    for (BaseColony c : star.getColonies()) {
      if (c.getEmpireKey() != null && c.getEmpireKey().equals(myEmpire.getKey())) {
        colonies.add((Colony) c);
      }
    }
    Collections.sort(colonies, (lhs, rhs) -> lhs.getPlanetIndex() - rhs.getPlanetIndex());

    if (firstRefresh) {
      firstRefresh = false;
      int colonyIndex = 0;
      for (Colony colony : colonies) {
        if (colony.getPlanetIndex() == args.getPlanetIndex()) {
          break;
        }
        colonyIndex++;
      }

      viewPager.setCurrentItem(colonyIndex);
    }

    if (dataSetChanged) {
      colonyPagerAdapter.notifyDataSetChanged();
    }
  }

  public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
    public ColonyPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      Fragment fragment = new TabContainerFragment();
      fragment.setArguments(
          new BuildFragmentArgs.Builder(star.getID(), colonies.get(i).getPlanetIndex())
              .build().toBundle());
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

  public static abstract class BaseTabFragment extends BaseFragment {
    private BuildFragmentArgs args;

    /** Called when it's time to refresh a star, could be the first refresh as well. */
    protected abstract void refresh(Star star, Colony colony);

    protected void refresh(Star star) {
      int planetIndex = args.getPlanetIndex();

      for (BaseColony colony : star.getColonies()) {
        if (colony.getPlanetIndex() == planetIndex) {
          refresh(star, (Colony) colony);
          return;
        }
      }
    }

    @Override
    public void onResume() {
      super.onResume();

      args = BuildFragmentArgs.fromBundle(requireArguments());
      StarManager.eventBus.register(eventHandler);

      Star star = StarManager.i.getStar(args.getStarID());
      if (star != null) {
        refresh(star);
      }
    }

    @Override
    public void onPause() {
      super.onPause();
      StarManager.eventBus.unregister(eventHandler);
    }

    private final Object eventHandler = new Object() {
      @EventHandler
      public void onStarUpdated(Star s) {
        if (s.getID() == args.getStarID()) {
          refresh(s);
        }
      }
    };
  }
}
