package au.com.codeka.warworlds.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.game.starfield.StarfieldActivity;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;

public class EnemyEmpireActivity extends BaseActivity
                                 implements EmpireManager.EmpireFetchedHandler,
                                            EmpireShieldManager.EmpireShieldUpdatedHandler {
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

        Button privateMsgBtn = (Button) findViewById(R.id.private_message_btn);
        privateMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrivateMessageClick();
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
    protected void onStart() {
        super.onStart();
        EmpireShieldManager.i.addEmpireShieldUpdatedHandler(this);
    }

    @Override
    protected void onStop() {
        super.onStart();
        EmpireShieldManager.i.removeEmpireShieldUpdatedHandler(this);
    }


    /** Called when an empire's shield is updated, we'll have to refresh the list. */
    @Override
    public void onEmpireShieldUpdated(int empireID) {
        refresh();
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        mEmpire = empire;
        refresh();
    }

    private void refresh() {
        TextView empireName = (TextView) findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);

        empireName.setText(mEmpire.getDisplayName());
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(mContext, mEmpire));

        TextView tv = (TextView) findViewById(R.id.private_message_btn_msg);
        tv.setText(String.format(tv.getText().toString(), mEmpire.getDisplayName()));

        tv = (TextView) findViewById(R.id.view_msg);
        tv.setText(String.format(tv.getText().toString(), mEmpire.getDisplayName()));
    }

    public void onEmpireViewClick() {
        if (mEmpire != null && mEmpire.getHomeStar() != null) {
            BaseStar homeStar = mEmpire.getHomeStar();
            Intent intent = new Intent(mContext, StarfieldActivity.class);
            intent.putExtra("au.com.codeka.warworlds.StarKey", homeStar.getKey());
            intent.putExtra("au.com.codeka.warworlds.SectorX", homeStar.getSectorX());
            intent.putExtra("au.com.codeka.warworlds.SectorY", homeStar.getSectorY());
            intent.putExtra("au.com.codeka.warworlds.OffsetX", homeStar.getOffsetX());
            intent.putExtra("au.com.codeka.warworlds.OffsetY", homeStar.getOffsetY());
            startActivity(intent);
        }
    }

    public void onPrivateMessageClick() {
        if (mEmpire != null) {
            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra("au.com.codeka.warworlds.NewConversationEmpireKey", mEmpire.getKey());
            startActivity(intent);

        }
    }
}
