package au.com.codeka.warworlds.game.build;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import au.com.codeka.RomanNumeralFormatter;
import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.solarsystem.OwnedPlanetFragment;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
public class BuildActivity extends BaseActivity {
  private Star star;
  private List<Colony> colonies;
  private ViewPager viewPager;
  private ColonyPagerAdapter colonyPagerAdapter;
  private Colony initialColony;

  public Star getStar() {
    return star;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.build);

    colonyPagerAdapter = new ColonyPagerAdapter(getSupportFragmentManager());
    viewPager = findViewById(R.id.pager);
    viewPager.setAdapter(colonyPagerAdapter);

    if (savedInstanceState != null) {
      Star s = null;

      try {
        byte[] bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Star");
        Messages.Star star_pb = Messages.Star.parseFrom(bytes);
        s = new Star();
        s.fromProtocolBuffer(star_pb);

        bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.CurrentColony");
        Messages.Colony colony_pb = Messages.Colony.parseFrom(bytes);
        initialColony = new Colony();
        initialColony.fromProtocolBuffer(colony_pb);
      } catch (InvalidProtocolBufferException e) {
      }

      updateStar(s);
    }
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();
    ServerGreeter.waitForHello(this, (success, greeting) -> {
      Bundle extras = getIntent().getExtras();
      String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
      byte[] colonyBytes = extras.getByteArray("au.com.codeka.warworlds.Colony");
      try {
        Messages.Colony colony_pb = Messages.Colony.parseFrom(colonyBytes);
        initialColony = new Colony();
        initialColony.fromProtocolBuffer(colony_pb);
      } catch (InvalidProtocolBufferException e) {
      }

      StarManager.eventBus.register(eventHandler);
      Star star = StarManager.i.getStar(Integer.parseInt(starKey));
      if (star != null) {
        updateStar(star);
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    StarManager.eventBus.unregister(eventHandler);
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    if (star != null) {
      Messages.Star.Builder star_pb = Messages.Star.newBuilder();
      star.toProtocolBuffer(star_pb);
      state.putByteArray("au.com.codeka.warworlds.Star", star_pb.build().toByteArray());
    }

    Colony currentColony = colonies.get(viewPager.getCurrentItem());
    if (currentColony != null) {
      Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
      currentColony.toProtocolBuffer(colony_pb);
      state.putByteArray("au.com.codeka.warworlds.CurrentColony", colony_pb.build().toByteArray());
    }
  }

  private void refreshColonyDetails(Colony colony) {
    ImageView planetIcon = findViewById(R.id.planet_icon);
    Planet planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
    Sprite planetSprite = PlanetImageManager.getInstance().getSprite(planet);
    planetIcon.setImageDrawable(new SpriteDrawable(planetSprite));

    TextView planetName = findViewById(R.id.planet_name);
    planetName.setText(String.format(Locale.ENGLISH, "%s %s", star.getName(),
        RomanNumeralFormatter.format(colony.getPlanetIndex())));

    TextView buildQueueDescription = findViewById(R.id.build_queue_description);
    int buildQueueLength = 0;
    for (BaseBuildRequest br : star.getBuildRequests()) {
      if (br.getColonyKey().equals(colony.getKey())) {
        buildQueueLength++;
      }
    }
    if (buildQueueLength == 0) {
      buildQueueDescription.setText("Build queue: idle");
    } else {
      buildQueueDescription.setText(String.format(Locale.ENGLISH,
          "Build queue: %d", buildQueueLength));
    }
  }

  public Object eventHandler = new Object() {
    @EventHandler
    public void onStarUpdated(Star s) {
      if (star == null || star.getKey().equals(s.getKey())) {
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

    if (initialColony != null) {
      int colonyIndex = 0;
      for (Colony colony : colonies) {
        if (colony.getKey().equals(initialColony.getKey())) {
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

  public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
    public ColonyPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      Fragment fragment = new BuildFragment();
      Bundle args = new Bundle();
      args.putString("au.com.codeka.warworlds.ColonyKey", colonies.get(i).getKey());
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
      if (colonies != null && colonies.size() > position) {
        refreshColonyDetails(colonies.get(position));
      }
    }
  }

  public static class BaseTabFragment extends Fragment {
    private String mColonyKey;

    protected Star getStar() {
      return ((BuildActivity) getActivity()).star;
    }

    protected Colony getColony() {
      if (mColonyKey == null) {
        Bundle args = getArguments();
        mColonyKey = args.getString("au.com.codeka.warworlds.ColonyKey");
      }

      Star star = getStar();
      if (star.getColonies() == null) {
        return null;
      }

      for (BaseColony baseColony : star.getColonies()) {
        if (baseColony.getKey().equals(mColonyKey)) {
          return (Colony) baseColony;
        }
      }

      return null;
    }
  }
}
