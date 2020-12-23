package au.com.codeka.warworlds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import org.joda.time.DateTime;

import au.com.codeka.warworlds.ctrl.EmpireRankList;
import au.com.codeka.warworlds.model.RankHistoryManager;
import au.com.codeka.warworlds.ui.BaseFragment;

/*
 * This activity is shown when the Blitz realm has been reset at the end of the month. We want to
 * show the final rankings before moving on.
 */
public class BlitzResetFragment extends BaseFragment {
  private EmpireRankList empireList;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.blitz_reset, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    View rootView = view.findViewById(android.R.id.content);
    ActivityBackgroundGenerator.setBackground(rootView);

    empireList = view.findViewById(R.id.empire_rankings);
    final ProgressBar progress = view.findViewById(R.id.progress_bar);
    progress.setVisibility(View.VISIBLE);
    empireList.setVisibility(View.GONE);

    Button startBtn = view.findViewById(R.id.start_btn);
    startBtn.setOnClickListener(v -> {
      // Now we can move to the WarWorlds activity again and get started.
      NavHostFragment.findNavController(this).popBackStack();
    });

    RankHistoryManager.i.getRankHistory(DateTime.now().minusMonths(1), rankHistory -> {
      empireList.setEmpireRanks(rankHistory.getRanks());

      progress.setVisibility(View.GONE);
      empireList.setVisibility(View.VISIBLE);
    });
  }
}
