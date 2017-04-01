package au.com.codeka.warworlds.client.game.build;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.squareup.picasso.Picasso;
import com.transitionseverywhere.TransitionManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.activity.TabbedBaseFragment;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.opengl.DimensionResolver;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.TimeFormatter;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireStorage;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.Simulation;
import au.com.codeka.warworlds.common.sim.StarModifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Shows buildings and ships on a planet. You can swipe left/right to switch between your colonies
 * in this star.
 */
public class BuildFragment extends BaseFragment {
  private static final Log log = new Log("BuildFragment");

  private static final String STAR_ID_KEY = "StarID";
  private static final String PLANET_INDEX_KEY = "PlanetIndex";

  private Star star;
  private List<Colony> colonies;
  private Colony initialColony;
  private ColonyPagerAdapter colonyPagerAdapter;

  /** The {@link Design} we're currently planning to build. */
  @Nullable private Design currentDesign;

  private ViewPager viewPager;
  private ImageView planetIcon;
  private TextView planetName;
  private TextView buildQueueDescription;

  private ViewGroup bottomPane;
  private ImageView buildIcon;
  private TextView buildName;
  private TextView buildDescription;
  private TextView buildTime;
  private TextView buildMinerals;
  private ViewGroup buildCountContainer;

  private SeekBar buildCountSeek;
  private EditText buildCount;

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

    colonyPagerAdapter = new ColonyPagerAdapter(this);
    viewPager = (ViewPager) view.findViewById(R.id.pager);
    viewPager.setAdapter(colonyPagerAdapter);
    planetIcon = (ImageView) view.findViewById(R.id.planet_icon);
    planetName = (TextView) view.findViewById(R.id.planet_name);
    buildQueueDescription = (TextView) view.findViewById(R.id.build_queue_description);

    bottomPane = (ViewGroup) view.findViewById(R.id.bottom_pane);
    buildIcon = (ImageView) view.findViewById(R.id.build_icon);
    buildName = (TextView) view.findViewById(R.id.build_name);
    buildDescription = (TextView) view.findViewById(R.id.build_description);
    buildCountContainer = (ViewGroup) view.findViewById(R.id.build_count_container);
    buildTime = (TextView) view.findViewById(R.id.build_timetobuild);
    buildMinerals = (TextView) view.findViewById(R.id.build_mineralstobuild);

    buildCountSeek = (SeekBar) view.findViewById(R.id.build_count_seek);
    buildCount = (EditText) view.findViewById(R.id.build_count_edit);
    buildCountSeek.setMax(1000);
    buildCountSeek.setOnSeekBarChangeListener(buildCountSeekBarChangeListener);

    view.findViewById(R.id.build_button).setOnClickListener(v -> build());
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

  /** Show the "build" popup sheet for the given {@link Design}. */
  public void showBuildSheet(Design design) {
    currentDesign = design;

    buildCount.setText("1");
    buildCountSeek.setProgress(1);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

    BuildHelper.setDesignIcon(design, buildIcon);
    buildName.setText(design.display_name);
    buildDescription.setText(Html.fromHtml(design.description));

    if (design.design_kind == Design.DesignKind.SHIP) {
      // You can only build more than ship at a time (not buildings).
      buildCountContainer.setVisibility(View.VISIBLE);
    } else {
      buildCountContainer.setVisibility(View.GONE);
    }
  }

  public void hideBuildSheet() {
    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.getLayoutParams().height = (int) new DimensionResolver(getContext()).dp2px(30);
    buildName.setText("");
    buildIcon.setImageDrawable(null);
  }

  private void updateBuildTime() {
    App.i.getTaskRunner().runTask(() -> {
      // Add the build request to a temporary copy of the star, simulate it and figure out the
      // build time.
      Star.Builder starBuilder = star.newBuilder();
      Colony colony = checkNotNull(colonies.get(viewPager.getCurrentItem()));
      Design design = checkNotNull(currentDesign);

      int count = 1;
      if (design.design_kind == Design.DesignKind.SHIP) {
        count = Integer.parseInt(buildCount.getText().toString());
      }

      int planetIndex = 0;
      for (int i = 0; i < star.planets.size(); i++) {
        Planet planet = star.planets.get(i);
        if (planet.colony != null && planet.colony.id.equals(colony.id)) {
          planetIndex = i;
        }
      }

      Empire myEmpire = EmpireManager.i.getMyEmpire();
      new StarModifier(() -> 0).modifyStar(starBuilder,
          new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
              .colony_id(colony.id)
              .count(count)
              .design_type(design.type)
              // TODO: upgrades?
              .build());
      // find the build request with ID 0, that's our guy

      Star updatedStar = starBuilder.build();
      for (BuildRequest buildRequest : BuildHelper.getBuildRequests(updatedStar)) {
        if (buildRequest.id == 0) {
          App.i.getTaskRunner().runTask(() -> {
            buildTime.setText(BuildHelper.formatTimeRemaining(buildRequest));
            EmpireStorage newEmpireStorage = BuildHelper.getEmpireStorage(updatedStar, myEmpire.id);
            EmpireStorage oldEmpireStorage = BuildHelper.getEmpireStorage(star, myEmpire.id);
            if (newEmpireStorage != null && oldEmpireStorage != null) {
              float mineralsDelta = newEmpireStorage.minerals_delta_per_hour
                  - oldEmpireStorage.minerals_delta_per_hour;
              buildMinerals.setText(String.format(Locale.US, "%s%.1f/hr",
                  mineralsDelta < 0 ? "-" : "+", Math.abs(mineralsDelta)));
              buildMinerals.setTextColor(mineralsDelta < 0 ? Color.RED : Color.GREEN);
            } else {
              buildMinerals.setText("");
            }
          }, Threads.UI);
        }
      }
    }, Threads.BACKGROUND);
  }

  /** Start building the thing we currently have showing. */
  public void build() {
    if (currentDesign != null) {
      String str = buildCount.getText().toString();
      int count;
      try {
        count = Integer.parseInt(str);
      } catch (NumberFormatException e) {
        count = 1;
      }

      if (count <= 0) {
        return;
      }

      Colony colony = colonies.get(viewPager.getCurrentItem());
      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
          .colony_id(colony.id)
          .design_type(currentDesign.type)
          .count(count)
          .build());
    }

    getFragmentManager().popBackStack();
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
          && planet.colony.empire_id.equals(myEmpire.id)) {
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
    checkNotNull(planet);

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

  private final SeekBar.OnSeekBarChangeListener buildCountSeekBarChangeListener =
      new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean userInitiated) {
          if (!userInitiated) {
            return;
          }
          buildCount.setText(String.format(Locale.US, "%d", progress));
          updateBuildTime();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
      };

  public class ColonyPagerAdapter extends FragmentStatePagerAdapter {
    private BuildFragment buildFragment;

    public ColonyPagerAdapter(BuildFragment buildFragment) {
      super(buildFragment.getFragmentManager());
      this.buildFragment = buildFragment;
    }

    public BuildFragment getBuildFragment() {
      return buildFragment;
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);

      getTabManager().setOnTabChangeListener(s -> getBuildFragment().hideBuildSheet());
    }

    public BuildFragment getBuildFragment() {
      ViewPager viewPager = (ViewPager) getTabHost().getParent();
      ColonyPagerAdapter adapter = (ColonyPagerAdapter) viewPager.getAdapter();
      return adapter.getBuildFragment();
    }

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

    /** Gets a reference to the {@link BuildFragment} we're inside of. */
    protected BuildFragment getBuildFragment() {
      TabFragment tabFragment = (TabFragment) getParentFragment();
      return tabFragment.getBuildFragment();
    }
  }
}
