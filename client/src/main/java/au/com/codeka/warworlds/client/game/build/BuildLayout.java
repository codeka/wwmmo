package au.com.codeka.warworlds.client.game.build;

import android.content.Context;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.client.opengl.DimensionResolver;
import au.com.codeka.warworlds.client.util.RomanNumeralFormatter;
import au.com.codeka.warworlds.common.proto.BuildRequest;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Colony;
import au.com.codeka.warworlds.common.proto.Design;
import au.com.codeka.warworlds.common.proto.Fleet;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.sim.DesignHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Layout for {@link BuildScreen}.
 */
public class BuildLayout extends RelativeLayout {
  private final ColonyPagerAdapter colonyPagerAdapter;
  private final ViewPager viewPager;
  private final ImageView planetIcon;
  private final TextView planetName;
  private final TextView buildQueueDescription;
  private final ViewGroup bottomPane;
  @Nullable private BottomPaneContentView bottomPaneContentView;

  private Star star;
  private List<Colony> colonies;

  public BuildLayout(Context context, Star star, List<Colony> colonies, int initialIndex) {
    super(context);
    this.star = star;
    this.colonies = colonies;
    inflate(context, R.layout.build, this);

    colonyPagerAdapter = new ColonyPagerAdapter(context, star, colonies, this);
    viewPager = findViewById(R.id.pager);
    viewPager.setAdapter(colonyPagerAdapter);
    viewPager.setCurrentItem(initialIndex);
    planetIcon = findViewById(R.id.planet_icon);
    planetName = findViewById(R.id.planet_name);
    buildQueueDescription = findViewById(R.id.build_queue_description);
    bottomPane = findViewById(R.id.bottom_pane);
    viewPager.addOnPageChangeListener(pageChangeListener);
  }

  /** Called when the star is updated. We'll need to refresh our current view. */
  public void refresh(Star star, List<Colony> colonies) {
    this.star = star;
    this.colonies = colonies;
    colonyPagerAdapter.refresh(star, colonies);
    if (bottomPaneContentView != null) {
      bottomPaneContentView.refresh(star);
    }
  }

  /** Show the "build" popup sheet for the given {@link Design}. */
  public void showBuildSheet(Design design) {
    final Colony colony = checkNotNull(colonies.get(viewPager.getCurrentItem()));

    bottomPaneContentView = new BuildBottomPane(
        getContext(), star, colony, design, (designType, count) -> {
            StarManager.i.updateStar(star, new StarModification.Builder()
                .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
                .colony_id(colony.id)
                .design_type(designType)
                .count(count));
            hideBottomSheet();
          });

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    bottomPane.removeAllViews();
    bottomPane.addView((View) bottomPaneContentView);
  }

  /**
   * Show the "upgrade" sheet for upgrading the given {@link Building}. If the building is already
   * at max level, then nothing will happen.
   */
  public void showUpgradeSheet(Building building) {
    final Colony colony = checkNotNull(colonies.get(viewPager.getCurrentItem()));
    final Design design = DesignHelper.getDesign(building.design_type);

    bottomPaneContentView = new UpgradeBottomPane(
        getContext(), star, colony, design, building, (bldg) -> {
      StarManager.i.updateStar(star, new StarModification.Builder()
          .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
          .colony_id(colony.id)
          .building_id(building.id)
          .design_type(building.design_type));
      hideBottomSheet();
    });

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    bottomPane.removeAllViews();
    bottomPane.addView((View) bottomPaneContentView);
  }

  /**
   * Show the "progress" sheet for a currently-in progress fleet build. The fleet will be non-null
   * if we're upgrading.
   */
  public void showProgressSheet(@Nullable Fleet fleet, BuildRequest buildRequest) {
    final Colony colony = checkNotNull(colonies.get(viewPager.getCurrentItem()));

    bottomPaneContentView =
        new ProgressBottomPane(getContext(), buildRequest, () -> {
          // "Cancel" has been clicked.
          StarManager.i.updateStar(star, new StarModification.Builder()
              .type(StarModification.MODIFICATION_TYPE.DELETE_BUILD_REQUEST)
              .build_request_id(buildRequest.id));
          hideBottomSheet();
        });

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    bottomPane.removeAllViews();
    bottomPane.addView((View) bottomPaneContentView);
  }

  public void hideBottomSheet() {
    bottomPaneContentView = null;

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.getLayoutParams().height = (int) new DimensionResolver(getContext()).dp2px(30);
    bottomPane.removeAllViews();
  }

  public void refreshColonyDetails(Colony colony) {
    Planet planet = null;
    for (Planet p : star.planets) {
      if (p.colony != null && p.colony.id.equals(colony.id)) {
        planet = p;
      }
    }
    checkNotNull(planet);

    Picasso.get()
        .load(ImageHelper.getPlanetImageUrl(getContext(), star, planet.index, 64, 64))
        .into(planetIcon);
    planetName.setText(String.format(Locale.US, "%s %s", star.name,
        RomanNumeralFormatter.format(planet.index + 1)));

    int buildQueueLength = colony.build_requests.size();
    if (buildQueueLength == 0) {
      buildQueueDescription.setText("Build queue: idle");
    } else {
      buildQueueDescription.setText(String.format(Locale.ENGLISH,
          "Build queue: %d", buildQueueLength));
    }
  }

  private final ViewPager.OnPageChangeListener pageChangeListener =
      new ViewPager.OnPageChangeListener() {
          @Override
          public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
          }

          @Override
          public void onPageSelected(int position) {
            refreshColonyDetails(colonies.get(position));
          }

          @Override
          public void onPageScrollStateChanged(int state) {
          }
        };
}
