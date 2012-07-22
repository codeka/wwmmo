package au.com.codeka.warworlds.game.starfield;

import java.util.HashSet;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;

/**
 * This dialog shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireDialog extends Dialog {
    private StarfieldActivity mActivity;

    public static final int ID = 2001;

    private View mOverviewContainer;
    private View mFleetContainer;
    private View mColonyContainer;
    private View mProgressContainer;
    private Button mOverviewButton;
    private Button mFleetButton;
    private Button mColonyButton;
    

    public EmpireDialog(StarfieldActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.starfield_empire_dlg);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        mOverviewContainer = findViewById(R.id.overview_container);
        mFleetContainer = findViewById(R.id.fleet_container);
        mColonyContainer = findViewById(R.id.colony_container);
        mProgressContainer = findViewById(R.id.progress_container);
        mOverviewButton = (Button) findViewById(R.id.overview_btn);
        mFleetButton = (Button) findViewById(R.id.fleet_btn);
        mColonyButton = (Button) findViewById(R.id.colony_btn);

        refresh();

        mOverviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFleetContainer.setVisibility(View.GONE);
                mColonyContainer.setVisibility(View.GONE);
                mOverviewContainer.setVisibility(View.VISIBLE);
            }
        });

        mFleetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOverviewContainer.setVisibility(View.GONE);
                mColonyContainer.setVisibility(View.GONE);
                mFleetContainer.setVisibility(View.VISIBLE);
            }
        });

        mColonyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFleetContainer.setVisibility(View.GONE);
                mOverviewContainer.setVisibility(View.GONE);
                mColonyContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    public void refresh() {
        mOverviewContainer.setVisibility(View.GONE);
        mFleetContainer.setVisibility(View.GONE);
        mColonyContainer.setVisibility(View.GONE);
        mProgressContainer.setVisibility(View.VISIBLE);
        mOverviewButton.setEnabled(false);
        mFleetButton.setEnabled(false);
        mColonyButton.setEnabled(false);

        MyEmpire empire = EmpireManager.getInstance().getEmpire();

        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        empireName.setText(empire.getDisplayName());

        empire.refreshAllDetails(new MyEmpire.RefreshAllCompleteHandler() {
            @Override
            public void onRefreshAllComplete(MyEmpire empire) {
                updateControls(empire);

                mOverviewContainer.setVisibility(View.VISIBLE);
                mFleetContainer.setVisibility(View.GONE);
                mColonyContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.GONE);
                mOverviewButton.setEnabled(true);
                mFleetButton.setEnabled(true);
                mColonyButton.setEnabled(true);
            }
        });
    }

    private void updateControls(MyEmpire empire) {
        HashSet<String> colonizedStarKeys = new HashSet<String>();
        for (Colony c : empire.getAllColonies()) {
            colonizedStarKeys.add(c.getStarKey());
        }

        int totalShips = 0;
        for (Fleet f : empire.getAllFleets()) {
            totalShips += f.getNumShips();
        }

        // Current value of R.id.empire_overview_format (in English):
        // %1$d stars and %2$d planets colonized
        // %3$d ships in %4$d fleets
        String fmt = mActivity.getString(R.string.empire_overview_format);
        final TextView overviewText = (TextView) findViewById(R.id.overview_text);
        String overview = String.format(fmt,
                colonizedStarKeys.size(), empire.getAllColonies().size(),
                totalShips, empire.getAllFleets().size());
        overviewText.setText(Html.fromHtml(overview));
    }
}
