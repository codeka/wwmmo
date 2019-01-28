package au.com.codeka.warworlds.game.empire;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ctrl.EmpireRankList;
import au.com.codeka.warworlds.ctrl.EmpireRankRecyclerViewHelper;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.ShieldManager;

public class OverviewFragment extends BaseFragment {
  private static final Log log = new Log("OverviewFragment");
  private View rootView;
  private EmpireRankRecyclerViewHelper empireRankListHelper;

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

  private Object eventHandler = new Object() {
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

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.empire_overview_tab, container, false);
    final RecyclerView recyclerView = rootView.findViewById(R.id.empire_rankings);

    final ProgressBar progress = rootView.findViewById(R.id.progress_bar);
    progress.setVisibility(View.VISIBLE);

    empireRankListHelper = new EmpireRankRecyclerViewHelper(recyclerView,
        new EmpireRankRecyclerViewHelper.Callbacks() {
      @Override
      public void onEmpireClick(Empire empire) {
        Intent intent = new Intent(getActivity(), EnemyEmpireActivity.class);
        intent.putExtra("au.com.codeka.warworlds.EmpireKey", empire.getKey());
        Activity activity = getActivity();
        if (activity != null) {
          activity.startActivity(intent);
        }
      }

      @Override
      public void fetchRows(
          int startPosition,
          int count,
          final EmpireRankRecyclerViewHelper.RowsFetchCallback callback) {
        EmpireManager.i.searchEmpiresByRank(startPosition + 1, startPosition + 1 + count,
            new EmpireManager.SearchCompleteHandler() {
              @Override
              public void onSearchComplete(List<Empire> empires) {
                callback.onRowsFetched(empires);
                if (progress.getVisibility() == View.VISIBLE) {
                  progress.setVisibility(View.GONE);
                }
              }
            });
      }
    });

    refresh();

    TextView empireSearch = rootView.findViewById(R.id.empire_search);
    empireSearch.setOnEditorActionListener(new OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
          onEmpireSearch();
          return true;
        }
        return false;
      }
    });

    final Button searchBtn = rootView.findViewById(R.id.search_btn);
    searchBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onEmpireSearch();
      }
    });

    return rootView;
  }

  private void refresh() {
    Empire empire = EmpireManager.i.getEmpire();
    log.warning("REFRESHING: " + empire.getDisplayName());

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
        new EmpireManager.SearchCompleteHandler() {
          @Override
          public void onSearchComplete(List<Empire> empires) {
            empireRankListHelper.setEmpires(empires);
            rankList.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
          }
        });
  }
}
