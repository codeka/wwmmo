package au.com.codeka.warworlds.game.starfield;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.EmpireManager;

/**
 * This dialog shows the status of the empire. You can see all your colonies, all your fleets, etc.
 */
public class EmpireDialog extends Dialog {
    private StarfieldActivity mActivity;

    public static final int ID = 2001;

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

        final View overviewContainer = findViewById(R.id.overview_container);
        final View fleetContainer = findViewById(R.id.fleet_container);
        final View colonyContainer = findViewById(R.id.colony_container);

        final Button overviewBtn = (Button) findViewById(R.id.overview_btn);
        final Button fleetBtn = (Button) findViewById(R.id.fleet_btn);
        final Button colonyBtn = (Button) findViewById(R.id.colony_btn);

        final TextView empireName = (TextView) findViewById(R.id.empire_name);
        empireName.setText(EmpireManager.getInstance().getEmpire().getDisplayName());

        //final TextView overviewText = (TextView) findViewById(R.id.overview_text);
        //overviewText.setText(text)

        overviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fleetContainer.setVisibility(View.GONE);
                colonyContainer.setVisibility(View.GONE);
                overviewContainer.setVisibility(View.VISIBLE);
            }
        });

        fleetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overviewContainer.setVisibility(View.GONE);
                colonyContainer.setVisibility(View.GONE);
                fleetContainer.setVisibility(View.VISIBLE);
            }
        });

        colonyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fleetContainer.setVisibility(View.GONE);
                overviewContainer.setVisibility(View.GONE);
                colonyContainer.setVisibility(View.VISIBLE);
            }
        });
    }
}
