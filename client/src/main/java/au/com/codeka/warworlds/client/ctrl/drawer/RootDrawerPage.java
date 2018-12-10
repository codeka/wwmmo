package au.com.codeka.warworlds.client.ctrl.drawer;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.MainActivity;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.empire.EmpireScreen;
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.ui.ScreenStack;

public class RootDrawerPage implements DrawerPage {
  // Will be non-null between onCreate/onDestroy.
  @Nullable
  private NavigationView navigationView;

  public RootDrawerPage(
      MainActivity activity,
      DrawerController drawerController,
      ScreenStack screenStack) {

    navigationView.setNavigationItemSelectedListener(item -> {
      //item.setChecked(true);
      switch (item.getItemId()) {
        case R.id.nav_starfield:
          screenStack.home();
          screenStack.push(new StarfieldScreen());
          break;
        case R.id.nav_empire:
          screenStack.home();
          screenStack.push(new EmpireScreen());
          break;
      }
      drawerController.closeDrawer();
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

}
