package au.com.codeka.warworlds.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.EmpireHelper;
import au.com.codeka.warworlds.model.EmpireManager;

public class EnemyEmpireActivity extends BaseActivity
                                 implements EmpireManager.EmpireFetchedHandler {
    private Context mContext = this;
    private Empire mEmpire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.enemy_empire);

        Button viewBtn = (Button) findViewById(R.id.view_btn);
        viewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEmpireViewClick();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(EnemyEmpireActivity.this, WarWorldsActivity.class));
                } else {
                    String empireKey = getIntent().getExtras().getString("au.com.codeka.warworlds.EmpireKey");
                    EmpireManager.i.fetchEmpire(empireKey, EnemyEmpireActivity.this);
                }
            }
        });
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        mEmpire = empire;

        TextView empireName = (TextView) findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);

        empireName.setText(mEmpire.display_name);
        empireIcon.setImageBitmap(EmpireHelper.getShield(mContext, mEmpire));
    }

    public void onEmpireViewClick() {
        if (mEmpire.home_star != null) {
            Star homeStar = mEmpire.home_star;
            Intent intent = new Intent(mContext, StarfieldActivity.class);
            intent.putExtra("au.com.codeka.warworlds.StarKey", homeStar.key);
            intent.putExtra("au.com.codeka.warworlds.SectorX", homeStar.sector_x);
            intent.putExtra("au.com.codeka.warworlds.SectorY", homeStar.sector_y);
            intent.putExtra("au.com.codeka.warworlds.OffsetX", homeStar.offset_x);
            intent.putExtra("au.com.codeka.warworlds.OffsetY", homeStar.offset_y);
            startActivity(intent);
        }
    }
}
