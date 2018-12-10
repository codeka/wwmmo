package au.com.codeka.warworlds.client.ctrl.drawer;

import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Stack;

import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ui.ScreenStack;

import static com.google.common.base.Preconditions.checkNotNull;

public class DrawerController {
  private final MainActivity activity;
  private final ScreenStack screenStack;
  private final ActionBar actionBar;
  private final DrawerLayout drawerLayout;
  private final FrameLayout drawerContent;
  private final ActionBarDrawerToggle drawerToggle;
  private final Stack<DrawerPage> pageStack;

  public DrawerController(
      MainActivity activity,
      ScreenStack screenStack,
      ActionBar actionBar,
      DrawerLayout drawerLayout,
      FrameLayout drawerContent) {
    this.activity = checkNotNull(activity);
    this.screenStack = checkNotNull(screenStack);
    this.actionBar = checkNotNull(actionBar);
    this.drawerLayout = checkNotNull(drawerLayout);
    this.drawerContent = checkNotNull(drawerContent);
    this.pageStack = new Stack<>();

    actionBar.show();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    drawerToggle =
        new ActionBarDrawerToggle(
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

    pageStack.push(new RootDrawerPage(activity, this, screenStack, drawerContent));
  }

  public void openDrawer() {
    drawerLayout.openDrawer(GravityCompat.START);
  }

  public void closeDrawer() {
    drawerLayout.closeDrawer(GravityCompat.START);
  }

  private void refreshTitle() {
    if (drawerLayout.isDrawerOpen(drawerContent)) {
      actionBar.setTitle("Star Search");
    } else {
      actionBar.setTitle("War Worlds 2");
    }
  }
}
