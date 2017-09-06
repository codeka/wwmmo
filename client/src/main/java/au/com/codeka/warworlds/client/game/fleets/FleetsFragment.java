package au.com.codeka.warworlds.client.game.fleets;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager;
import au.com.codeka.warworlds.client.game.world.ArrayListStarCollection;
import au.com.codeka.warworlds.client.game.world.MyEmpireStarCollection;
import au.com.codeka.warworlds.client.game.world.StarCollection;
import au.com.codeka.warworlds.client.game.world.StarManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * This fragment contains a list of fleets, and lets you do all the interesting stuff on them (like
 * merge, split, move, etc).
 */
public class FleetsFragment extends BaseFragment {
  private static final Log log = new Log("FleetFragment");
  private static final String STAR_ID_KEY = "StarID";
  private static final String FLEET_ID_KEY = "FleetID";

  /** The star whose fleets we're displaying, null if we're displaying all stars fleets. */
  @Nullable private Star star;

  private StarCollection starCollection;
  private ExpandableListView listView;
  private FleetExpandableStarListAdapter adapter;
  private FrameLayout bottomPane;
  private View view;

  public static Bundle createArguments(long starId, @Nullable Long selectedFleetId) {
    Bundle args = new Bundle();
    args.putLong(STAR_ID_KEY, starId);
    if (selectedFleetId != null) {
      args.putLong(FLEET_ID_KEY, selectedFleetId);
    }
    return args;
  }

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_fleets;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    this.view = view;

    bottomPane = view.findViewById(R.id.bottom_pane);

    listView = view.findViewById(R.id.fleet_list);
    listView.setOnChildClickListener((lv, v, groupPosition, childPosition, id) -> {
      if (adapter != null) {
        adapter.onItemClick(groupPosition, childPosition);
      }
      return false;
    });
  }

  @Override
  public void onStart() {
    super.onStart();

    starCollection = new ArrayListStarCollection();

    long starID = getArguments().getLong(STAR_ID_KEY, 0);
    if (starID != 0) {
      star = StarManager.i.getStar(starID);
      if (star != null) {
        ArrayListStarCollection arrayListStarCollection = new ArrayListStarCollection();
        arrayListStarCollection.getStars().clear();
        arrayListStarCollection.getStars().add(star);
        starCollection = arrayListStarCollection;
      } else {
        // TODO: wait for the star to be returned from cache?
      }
    } else {
      starCollection = new MyEmpireStarCollection();
    }

    adapter = new FleetExpandableStarListAdapter(LayoutInflater.from(getContext()), starCollection);
    listView.setAdapter(adapter);
    long fleetID = getArguments().getLong(FLEET_ID_KEY, 0);
    if (fleetID != 0 && starID != 0) {
      adapter.setSelectedFleetId(starID, fleetID);
      listView.expandGroup(starCollection.indexOf(starID), false /* animate */);
    }

    adapter.notifyDataSetChanged();

    showActionsPane();
  }

  @Override
  public void onStop() {
    super.onStop();

    starCollection = null;
    listView.setAdapter((ExpandableListAdapter) null);
    adapter.destroy();
    adapter = null;
  }

  private void showActionsPane() {
    showStarfield(false /* visible */);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(new ActionBottomPane(getContext(), new ActionBottomPane.Callback() {
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
    }));
  }

  private void showMovePane() {
    if (adapter.getSelectedFleetId() == null) {
      return;
    }
    showStarfield(true /* visible */);

    long fleetId = adapter.getSelectedFleetId();

    // TODO: the cast seems... not great.
    StarfieldManager starfieldManager =
        ((MainActivity) getFragmentActivity()).getStarfieldManager();

    MoveBottomPane moveBottomPane =
        new MoveBottomPane(getContext(), starfieldManager, this::showActionsPane);
    moveBottomPane.setFleet(star, fleetId);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(moveBottomPane);
  }

  private void showSplitPane() {
    if (adapter.getSelectedFleetId() == null) {
      // No fleet selected, can't split.
      return;
    }
    showStarfield(false /* visible */);

    long fleetId = adapter.getSelectedFleetId();

    SplitBottomPane splitBottomPane = new SplitBottomPane(getContext(), this::showActionsPane);
    splitBottomPane.setFleet(star, fleetId);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(splitBottomPane);
  }

  private void showMergePane() {
    if (adapter.getSelectedFleetId() == null) {
      // No fleet selected, can't split.
      return;
    }
    showStarfield(false /* visible */);

    long fleetId = adapter.getSelectedFleetId();

    MergeBottomPane mergeBottomPane =
        new MergeBottomPane(getContext(), adapter, this::showActionsPane);
    mergeBottomPane.setFleet(star, fleetId);

    TransitionManager.beginDelayedTransition(bottomPane);
    bottomPane.removeAllViews();
    bottomPane.addView(mergeBottomPane);
  }

  private void showStarfield(boolean visible) {
    if (view != null) {
      if (visible) {
        listView.setVisibility(View.GONE);
        view.setBackground(null);
      } else {
        listView.setVisibility(View.VISIBLE);
        view.setBackgroundResource(R.color.default_background);
      }
    }
  }
}
