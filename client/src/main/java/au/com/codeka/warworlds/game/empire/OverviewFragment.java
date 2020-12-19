package au.com.codeka.warworlds.game.empire;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.EmpireRankRecyclerViewHelper;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class OverviewFragment extends Fragment {
  private static final Log log = new Log("OverviewFragment");
  private View rootView;
  private EmpireRankRecyclerViewHelper empireRankListHelper;
  private int rankType = R.id.battle_rank_7d;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.empire_overview_tab, container, false);
    final RecyclerView recyclerView = rootView.findViewById(R.id.empire_rankings);

    final ProgressBar progress = rootView.findViewById(R.id.progress_bar);
    progress.setVisibility(View.VISIBLE);

    empireRankListHelper = new EmpireRankRecyclerViewHelper(recyclerView, rankCallbacks);

    rootView.findViewById(R.id.popup_menu).setOnClickListener(view -> {
      FragmentActivity activity = getActivity();
      if (activity == null) {
        return;
      }
      PopupMenu popupMenu = new PopupMenu(activity, rootView.findViewById(R.id.popup_menu));
      popupMenu.setOnMenuItemClickListener(item -> {
        // Change the rank type and then refresh.
        rankType = item.getItemId();
        recyclerView.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        empireRankListHelper.refresh();
        return false;
      });
      popupMenu.inflate(R.menu.empire_rank_menu);
      popupMenu.show();
    });

    TextView empireSearch = rootView.findViewById(R.id.empire_search);
    empireSearch.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        onEmpireSearch();
        return true;
      }
      return false;
    });

    final Button searchBtn = rootView.findViewById(R.id.search_btn);
    searchBtn.setOnClickListener(v -> onEmpireSearch());

    return rootView;
  }

  @Override
  public void onStart() {
    super.onStart();
    ShieldManager.eventBus.register(eventHandler);
    EmpireManager.eventBus.register(eventHandler);
    refresh();
  }

  @Override
  public void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(eventHandler);
    EmpireManager.eventBus.unregister(eventHandler);
  }

  private void refresh() {
    Empire empire = EmpireManager.i.getEmpire();

    TextView empireName = rootView.findViewById(R.id.empire_name);
    ImageView empireIcon = rootView.findViewById(R.id.empire_icon);
    TextView allianceName = rootView.findViewById(R.id.alliance_name);
    ImageView allianceIcon = rootView.findViewById(R.id.alliance_icon);

    empireName.setText(empire.getDisplayName());
    empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
    if (empire.getAlliance() != null) {
      allianceName.setText(empire.getAlliance().getName());
      allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(),
          (Alliance) empire.getAlliance()));
    } else {
      allianceName.setText("");
      allianceIcon.setImageBitmap(null);
    }
  }

  private void onEmpireSearch() {
    log.info("onEmpireSearch...");
    final TextView empireSearch = rootView.findViewById(R.id.empire_search);
    final ProgressBar progress = rootView.findViewById(R.id.progress_bar);
    final RecyclerView rankList = rootView.findViewById(R.id.empire_rankings);

    progress.setVisibility(View.VISIBLE);
    rankList.setVisibility(View.GONE);

    // hide the soft keyboard (if showing) while the search happens
    InputMethodManager imm = (InputMethodManager) rootView.getContext().getSystemService(
        Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(empireSearch.getWindowToken(), 0);

    String nameSearch = empireSearch.getText().toString();
    EmpireManager.i.searchEmpires(nameSearch,
        empires -> {
          empireRankListHelper.setEmpires(empires);
          rankList.setVisibility(View.VISIBLE);
          progress.setVisibility(View.GONE);
        });
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      MyEmpire empire = EmpireManager.i.getEmpire();

      ImageView empireIcon = rootView.findViewById(R.id.empire_icon);
      empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));

      ImageView allianceIcon = rootView.findViewById(R.id.alliance_icon);
      if (empire.getAlliance() != null) {
        allianceIcon.setImageBitmap(
            AllianceShieldManager.i.getShield(getActivity(),
                (Alliance) empire.getAlliance()));
      }
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      if (empire.getID() == EmpireManager.i.getEmpire().getID()) {
        refresh();
      }
    }
  };

  private final EmpireRankRecyclerViewHelper.Callbacks rankCallbacks =
      new EmpireRankRecyclerViewHelper.Callbacks() {
    @Override
    public void onEmpireClick(Empire empire) {
      NavHostFragment.findNavController(OverviewFragment.this).navigate(
          R.id.enemyEmpireFragment,
          new EnemyEmpireFragmentArgs.Builder(empire.getID()).build().toBundle());
    }

    @Override
    public void fetchRows(
        int startPosition,
        int count,
        final EmpireRankRecyclerViewHelper.RowsFetchCallback callback) {
      log.info("EmpireRankRecyclerViewHelper fetchRows");
      final ProgressBar progress = rootView.findViewById(R.id.progress_bar);
      if (rankType == R.id.stars_rank) {
        EmpireManager.i.searchEmpiresByRank(startPosition + 1, startPosition + 1 + count,
            empires -> {
              callback.onRowsFetched(empires);
              if (progress.getVisibility() == View.VISIBLE) {
                progress.setVisibility(View.GONE);
              }
            });
      } else {
        int numDays =
            (rankType == R.id.battle_rank_7d ? 7 : rankType == R.id.battle_rank_14d ? 14 : 28);
        EmpireManager.i.getEmpireBattleRanks(startPosition, count, numDays,
            battleRanks -> {
              callback.onBattleRankRowsFetched(battleRanks);
              if (progress.getVisibility() == View.VISIBLE) {
                progress.setVisibility(View.GONE);
              }
            });
      }
    }
  };
}
