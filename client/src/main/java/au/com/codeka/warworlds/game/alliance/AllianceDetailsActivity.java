package au.com.codeka.warworlds.game.alliance;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;

public class AllianceDetailsActivity extends BaseActivity {
    @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.alliance_details);

    Intent intent = getIntent();
    Bundle extras = intent.getExtras();

    AllianceDetailsFragment allianceDetailsFragment = new AllianceDetailsFragment();
    allianceDetailsFragment.setArguments(extras);
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.alliance_details, allianceDetailsFragment);
    transaction.commit();
  }
}
