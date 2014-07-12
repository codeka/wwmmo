package au.com.codeka.warworlds.game.solarsystem;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

/**
 * This activity is displayed when you're actually looking at a solar system (star + planets)
 */
public class SolarSystemActivity extends BaseActivity {
    private DrawerLayout drawerLayout;
    private View drawer;
    private ActionBarDrawerToggle drawerToggle;
    private Integer starID;
    private StarSummary star;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.solarsystem_activity);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer = findViewById(R.id.drawer);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                android.support.v7.appcompat.R.drawable.abc_ic_clear_search_api_holo_light,
                R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle("Star Name");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Star Search");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
          return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        StarManager.eventBus.register(eventHandler);

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                Bundle extras = getIntent().getExtras();
                String starKey = extras.getString("au.com.codeka.warworlds.StarKey");
                if (starKey != null) {
                    showStar(Integer.parseInt(starKey));
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        StarManager.eventBus.unregister(eventHandler);
    }

    @Override
    public void onBackPressed() {
        StarSummary star = StarManager.i.getStarSummary(starID, Float.MAX_VALUE);
        Intent intent = new Intent();
        if (star != null) {
            intent.putExtra("au.com.codeka.warworlds.SectorX", star.getSectorX());
            intent.putExtra("au.com.codeka.warworlds.SectorY", star.getSectorY());
            intent.putExtra("au.com.codeka.warworlds.StarKey", star.getKey());
        }
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    private void showStar(Integer starID) {
        this.starID = starID;
        star = StarManager.i.getStarSummary(starID);
        if (star != null) {
            getSupportActionBar().setTitle(star.getName());
        } else {
            getSupportActionBar().setTitle("Star Name");
        }

        Fragment fragment = new SolarSystemFragment();
        Bundle args = new Bundle();
        args.putLong("au.com.codeka.warworlds.StarID", starID);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.content, fragment)
                                   .commit();

        drawerLayout.closeDrawer(drawer);
    }

    private Object eventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(StarSummary starSummary) {
            if (starSummary.getID() == starID) {
                star = starSummary;
                getSupportActionBar().setTitle(starSummary.getName());
            }
        }
    };
}
