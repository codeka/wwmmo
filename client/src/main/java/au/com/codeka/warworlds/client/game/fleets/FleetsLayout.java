package au.com.codeka.warworlds.client.game.fleets;

import android.content.Context;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.world.StarCollection;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * Layout for the {@link FleetsScreen}.
 */
public class FleetsLayout extends RelativeLayout {
  private final StarCollection starCollection;
  private final ExpandableListView listView;
  private final FleetExpandableStarListAdapter adapter;
  private final FrameLayout bottomPane;

  public FleetsLayout(Context context, StarCollection starCollection) {
    super(context);
    inflate(context, R.layout.fleets, this);
    this.starCollection = starCollection;

    bottomPane = findViewById(R.id.bottom_pane);

    listView = findViewById(R.id.fleet_list);
    adapter = new FleetExpandableStarListAdapter(LayoutInflater.from(getContext()), starCollection);
    listView.setAdapter(adapter);

    listView.setOnChildClickListener((lv, v, groupPosition, childPosition, id) -> {
      adapter.onItemClick(groupPosition, childPosition);
      return false;
    });

    // Actions pane by default.
    bottomPane.addView(new ActionBottomPane(getContext(), actionBottomPaneCallback));
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    adapter.destroy();
  }

  public void showActionsPane() {
    showStarfield(false /* visible */);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(new ActionBottomPane(getContext(), actionBottomPaneCallback));
  }

  public void showMovePane() {
    if (adapter.getSelectedFleetId() == null) {
      return;
    }
    showStarfield(true /* visible */);

    Star star = adapter.getSelectedStar();
    if (star == null) {
      return;
    }

    long fleetId = adapter.getSelectedFleetId();

    // TODO: the cast seems... not great.
    StarfieldManager starfieldManager = ((MainActivity) getContext()).getStarfieldManager();

    MoveBottomPane moveBottomPane =
        new MoveBottomPane(getContext(), starfieldManager, this::showActionsPane);
    moveBottomPane.setFleet(star, fleetId);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(moveBottomPane);
  }

  public void showSplitPane() {
    if (adapter.getSelectedFleetId() == null) {
      // No fleet selected, can't split.
      return;
    }
    showStarfield(false /* visible */);

    Star star = adapter.getSelectedStar();
    if (star == null) {
      return;
    }

    long fleetId = adapter.getSelectedFleetId();

    SplitBottomPane splitBottomPane = new SplitBottomPane(getContext(), this::showActionsPane);
    splitBottomPane.setFleet(star, fleetId);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(splitBottomPane);
  }

  public void showMergePane() {
    if (adapter.getSelectedFleetId() == null) {
      // No fleet selected, can't split.
      return;
    }
    showStarfield(false /* visible */);

    Star star = adapter.getSelectedStar();
    if (star == null) {
      return;
    }

    long fleetId = adapter.getSelectedFleetId();

    MergeBottomPane mergeBottomPane =
        new MergeBottomPane(getContext(), adapter, this::showActionsPane);
    mergeBottomPane.setFleet(star, fleetId);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(mergeBottomPane);
  }

  public void showStarfield(boolean visible) {
    if (visible) {
      listView.setVisibility(View.GONE);
      setBackground(null);
    } else {
      listView.setVisibility(View.VISIBLE);
      setBackgroundResource(R.color.default_background);
    }
  }

  private final ActionBottomPane.Callback actionBottomPaneCallback =
      new ActionBottomPane.Callback() {
        @Override
        public void onMoveClick() {
          showMovePane();
        }

        @Override
        public void onSplitClick() {
          showSplitPane();
        }

        @Override
        public void onMergeClick() {
          showMergePane();
        }
      };
}
