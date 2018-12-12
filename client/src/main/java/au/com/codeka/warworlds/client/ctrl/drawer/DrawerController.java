package au.com.codeka.warworlds.client.ctrl.drawer;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.empire.EmpireScreen;
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen;
import au.com.codeka.warworlds.client.game.starsearch.StarSearchScreen;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.ui.ScreenStack;

import static com.google.common.base.Preconditions.checkNotNull;

public class DrawerController {
  private final ActionBar actionBar;
  private final DrawerLayout drawerLayout;
  private final FrameLayout drawerContent;
  private final NavigationView navigationView;


  public DrawerController(
      MainActivity activity,
      ScreenStack screenStack,
      ActionBar actionBar,
      DrawerLayout drawerLayout,
      FrameLayout drawerContent) {
    this.actionBar = checkNotNull(actionBar);
    this.drawerLayout = checkNotNull(drawerLayout);
    this.drawerContent = checkNotNull(drawerContent);

    actionBar.show();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
        activity, drawerLayout,
        R.string.drawer_open,
        R.string.drawer_close) {
      @Override
      public void onDrawerClosed(View view) {
        super.onDrawerClosed(view);
        refreshTitle();
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        refreshTitle();
        //searchListAdapter.setCursor(StarManager.i.getMyStars());
      }
    };
    drawerLayout.addDrawerListener(drawerToggle);
    drawerToggle.syncState();

    navigationView = drawerContent.findViewById(R.id.navigation_view);

    navigationView.setNavigationItemSelectedListener(item -> {
      //item.setChecked(true);
      switch (item.getItemId()) {
        case R.id.nav_starfield:
          screenStack.home();
          screenStack.push(new StarfieldScreen());
          break;
        case R.id.nav_star_search:
          screenStack.home();
          screenStack.push(new StarSearchScreen());
          break;
        case R.id.nav_empire:
          screenStack.home();
          screenStack.push(new EmpireScreen());
          break;
      }
      closeDrawer();
      return true;
    });

    // TODO: update this if your icon changes
    // Replace the empire icon with... your empire's icon.
    final MenuItem empireMenuItem = navigationView.getMenu().findItem(R.id.nav_empire);
    App.i.getServer().waitForHello(() -> App.i.getTaskRunner().runTask(() -> {
      String url = ImageHelper.getEmpireImageUrl(activity, EmpireManager.i.getMyEmpire(), 48, 48);
      Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
          empireMenuItem.setIcon(new BitmapDrawable(activity.getResources(), bitmap));
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
          empireMenuItem.setIcon(errorDrawable);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
          empireMenuItem.setIcon(placeHolderDrawable);
        }
      };
      // Picasso only keeps a weak reference to the target, but we want to keep it alive (at least
      // as long as the nav menu is alive), so add it to a tag in the view.
      navigationView.setTag(R.id.target_tag, target);
      Picasso.get().load(url).into(target);
    }, Threads.UI));
  }

  public void closeDrawer() {
    drawerLayout.closeDrawer(GravityCompat.START);
  }

  public void toggleDrawer() {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START);
    } else {
      drawerLayout.openDrawer(GravityCompat.START);
    }
  }

  private void refreshTitle() {
    if (drawerLayout.isDrawerOpen(drawerContent)) {
      actionBar.setTitle("Star Search");
    } else {
      actionBar.setTitle("War Worlds 2");
    }
  }
}
