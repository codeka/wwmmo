package au.com.codeka.warworlds.game.solarsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.navigation.NavArgs;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/**
 * A fragment that hosts {@link OwnedPlanetFragment}, {@link EmptyPlanetFragment}, etc, and lets you
 * swipe between all the same kind planets on the star.
 */
public class PlanetPagerFragment extends BaseFragment {
  private Star star;
  private List<Colony> colonies;
  private ViewPager viewPager;
  private ColonyPagerAdapter colonyPagerAdapter;

  private PlanetPagerFragmentArgs args;

  public enum Kind {
    OwnedPlanets,
    EnemyPlanets, // TODO: separate "enemy" and "native"?
    EmptyPlanets,
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

    args = PlanetPagerFragmentArgs.fromBundle(requireArguments());
    colonyPagerAdapter = new ColonyPagerAdapter(getChildFragmentManager());
    viewPager.setAdapter(colonyPagerAdapter);
  }

  public Star getStar() {
    return star;
  }

  @Override
  public void onResume() {
    super.onResume();

    ServerGreeter.waitForHello(requireActivity(), (success, greeting) -> {
      if (!success) {
        // TODO: is this still a thing?
      } else {
        StarManager.eventBus.register(eventHandler);
        Star star = StarManager.i.getStar(args.getStarID());
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
    boolean dataSetChanged = (this.star == null);
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

    int initialPlanetIndex = args.getPlanetIndex();
    if (initialPlanetIndex < 0) {
      initialPlanetIndex = colonies.get(0).getPlanetIndex();
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
      Fragment fragment;
      Bundle bundle;

      switch(args.getKind()) {
        case OwnedPlanets:
          fragment = new OwnedPlanetFragment();
          bundle = new OwnedPlanetFragmentArgs.Builder(
                  star.getID(), colonies.get(i).getPlanetIndex())
              .build().toBundle();
          break;
        default:
          // TODO: implement?
          fragment = new OwnedPlanetFragment();
          bundle = null;
          break;
      }

      fragment.setArguments(bundle);
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
        requireMainActivity().requireSupportActionBar().setTitle(
            String.format("%s %s", star.getName(),
                RomanNumeralFormatter.format(colonies.get(position).getPlanetIndex())));
      }
    }
  }
}
