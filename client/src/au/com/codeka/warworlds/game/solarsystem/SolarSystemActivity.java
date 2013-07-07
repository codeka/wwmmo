package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends BaseActivity implements StarManager.StarFetchedHandler {
    private static Logger log = LoggerFactory.getLogger(SolarSystemActivity.class);
    private ViewPager mViewPager;
    private StarPagerAdapter mStarPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.solarsystem_pager);
        mStarPagerAdapter = new StarPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mStarPagerAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                List<Long> starIDs = greeting.getStarIDs();
                int selectedIndex = -1;

                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
                if (starKey != null) {
                    long starID = Long.parseLong(starKey);

                    boolean needNewStarID = true;
                    for (long thisStarID : starIDs) {
                        if (starID == thisStarID) {
                            needNewStarID = false;
                        }
                    }
                    if (needNewStarID) {
                        starIDs.add(starID);
                    }

                    for (int i = 0; i < starIDs.size(); i++) {
                        if (starIDs.get(i) == starID) {
                            selectedIndex = i;
                            break;
                        }
                    }
                }

                mStarPagerAdapter.setStarIDs(starIDs);
                if (selectedIndex >= 0) {
                    mViewPager.setCurrentItem(selectedIndex);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StarManager.getInstance().removeStarUpdatedListener(this);
    }

    @Override
    public void onStarFetched(Star star) {
        log.debug("Star refreshed...");
    }

    public class StarPagerAdapter extends FragmentStatePagerAdapter {
        List<Long> mStarIDs;

        public StarPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setStarIDs(List<Long> starIDs) {
            mStarIDs = starIDs;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new SolarSystemFragment();
            Bundle args = new Bundle();
            args.putLong("au.com.codeka.warworlds.StarID", mStarIDs.get(i));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            if (mStarIDs == null) {
                return 0;
            }

            return mStarIDs.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Star " + (position + 1);
        }
    }

    private SolarSystemFragment getCurrentPage() {
        int index = mViewPager.getCurrentItem();
        return mStarPagerAdapter.get
    }

    @Override
    public void onBackPressed() {
        
        /*
        Intent intent = new Intent();
        if (mStar != null) {
            intent.putExtra("au.com.codeka.warworlds.SectorX", mStar.getSectorX());
            intent.putExtra("au.com.codeka.warworlds.SectorY", mStar.getSectorY());
            intent.putExtra("au.com.codeka.warworlds.StarKey", mStar.getKey());
        }
        setResult(RESULT_OK, intent);
*/
        super.onBackPressed();
    }
}
