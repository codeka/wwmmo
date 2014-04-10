package au.com.codeka.warworlds.game.alliance;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;

public class AllianceDetailsActivity extends BaseActivity {
    private AllianceDetailsFragment mAllianceDetailsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.alliance_details);
        mAllianceDetailsFragment = (AllianceDetailsFragment) getSupportFragmentManager().findFragmentById(R.id.alliance_details);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        mAllianceDetailsFragment = new AllianceDetailsFragment();
        mAllianceDetailsFragment.setArguments(extras);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.alliance_details, mAllianceDetailsFragment);
        transaction.commit();
    }
}
