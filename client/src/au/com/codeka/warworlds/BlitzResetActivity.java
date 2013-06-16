package au.com.codeka.warworlds;

import org.joda.time.DateTime;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import au.com.codeka.warworlds.ctrl.EmpireRankList;
import au.com.codeka.warworlds.model.RankHistory;
import au.com.codeka.warworlds.model.RankHistoryManager;

/*
 * This activity is shown when the Blitz realm has been reset at the end of the month. We want to
 * show the final rankings before moving on.
 */
public class BlitzResetActivity extends BaseActivity {
    private Context mContext = this;
    private EmpireRankList mEmpireList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
        setContentView(R.layout.blitz_reset);

        View rootView = findViewById(android.R.id.content);
        ActivityBackgroundGenerator.setBackground(rootView);

        mEmpireList = (EmpireRankList) findViewById(R.id.empire_rankings);
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_bar);
        progress.setVisibility(View.VISIBLE);
        mEmpireList.setVisibility(View.GONE);

        Button startBtn = (Button) findViewById(R.id.start_btn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // now we can move to the WarWorlds activity again and get started.
                finish();
                startActivity(new Intent(mContext, WarWorldsActivity.class));
            }
        });

        RankHistoryManager.i.getRankHistory(DateTime.now().minusMonths(1), new RankHistoryManager.RankHistoryFetchedHandler() {
            @Override
            public void onRankHistoryFetched(RankHistory rankHistory) {
                mEmpireList.setEmpireRanks(rankHistory.getRanks());

                progress.setVisibility(View.GONE);
                mEmpireList.setVisibility(View.VISIBLE);
            }
        });
    }
}