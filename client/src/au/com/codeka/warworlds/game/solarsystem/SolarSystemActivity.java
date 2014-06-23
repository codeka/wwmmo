package au.com.codeka.warworlds.game.solarsystem;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.Intent;
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

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends BaseActivity {
    private ViewPager mViewPager;
    private StarPagerAdapter mStarPagerAdapter;
    private int mInitialStarIndex = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar

        setContentView(R.layout.solarsystem_pager);
        mStarPagerAdapter = new StarPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mStarPagerAdapter);

        if (savedInstanceState != null) {
            mInitialStarIndex = savedInstanceState.getInt("au.com.codeka.warworlds.CurrentIndex");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                List<Long> starIDs = greeting.getStarIDs();

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

                    if (mInitialStarIndex < 0) {
                        for (int i = 0; i < starIDs.size(); i++) {
                            if (starIDs.get(i) == starID) {
                                mInitialStarIndex = i;
                                break;
                            }
                        }
                    }
                }

                mStarPagerAdapter.setStarIDs(starIDs);
                if (mInitialStarIndex >= 0) {
                    mStarPagerAdapter.setFragmentExtras(starIDs.get(mInitialStarIndex),
                                                        getIntent().getExtras());

                    mViewPager.setCurrentItem(mInitialStarIndex);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle instanceState) {
        super.onSaveInstanceState(instanceState);
        mInitialStarIndex = mViewPager.getCurrentItem();
        instanceState.putInt("au.com.codeka.warworlds.CurrentIndex", mViewPager.getCurrentItem());
    }

    @Override
    public void onRestoreInstanceState(Bundle instanceState) {
        super.onRestoreInstanceState(instanceState);
        mInitialStarIndex = instanceState.getInt("au.com.codeka.warworlds.CurrentIndex");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public class StarPagerAdapter extends FragmentStatePagerAdapter {
        private List<Long> mStarIDs;
        private Map<Long, Bundle> mFragmentExtras;

        public StarPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentExtras = new TreeMap<Long, Bundle>();
        }

        public void setFragmentExtras(long starID, Bundle extras) {
            mFragmentExtras.put(starID, extras);
        }

        public void setStarIDs(List<Long> starIDs) {
            mStarIDs = starIDs;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new SolarSystemFragment();
            Bundle args = mFragmentExtras.get(mStarIDs.get(i));
            if (args == null) {
                args = new Bundle();
            }
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

    private SolarSystemFragment getCurrentFragment() {
        int index = mViewPager.getCurrentItem();
        return (SolarSystemFragment) mStarPagerAdapter.instantiateItem(mViewPager, index);
    }

    @Override
    public void onBackPressed() {
        SolarSystemFragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            Star star = currentFragment.getStar();
            Intent intent = new Intent();
            if (star != null) {
                intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
                intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
                intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
            }
            setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
    }
}
