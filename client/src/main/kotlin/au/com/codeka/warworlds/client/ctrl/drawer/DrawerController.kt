package au.com.codeka.warworlds.client.ctrl.drawer

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.MainActivity
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.empire.EmpireScreen
import au.com.codeka.warworlds.client.game.sitrep.SitReportScreen
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen
import au.com.codeka.warworlds.client.game.starsearch.StarSearchScreen
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.ui.ScreenStack
import au.com.codeka.warworlds.client.ui.ScreenStack.ScreenStackStateUpdatedCallback
import au.com.codeka.warworlds.common.Log
import com.google.android.material.navigation.NavigationView
import com.google.common.base.Preconditions
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target

class DrawerController(
    activity: MainActivity,
    screenStack: ScreenStack,
    actionBar: ActionBar,
    drawerLayout: DrawerLayout,
    drawerContent: FrameLayout) {
  private val activity: MainActivity
  private val actionBar: ActionBar
  private val drawerLayout: DrawerLayout
  private val drawerContent: FrameLayout
  private val navigationView: NavigationView
  private val screenStack: ScreenStack
  private val drawerToggle: ActionBarDrawerToggle
  fun closeDrawer() {
    drawerLayout.closeDrawer(GravityCompat.START)
  }

  fun toggleDrawer() {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      // If the drawer is open, we'll close it.
      drawerLayout.closeDrawer(GravityCompat.START)
    } else if (screenStack.depth() > 1) {
      // If there's a screen under this screen, go back.
      activity.onBackPressed()
    } else {
      // Otherwise, we can open the drawer.
      drawerLayout.openDrawer(GravityCompat.START)
    }
  }

  /**
   * Called when the screen stack changed, we'll make sure the button is a back button when the
   * screen stack is more than 1 deep.
   */
  private fun updateBackButton() {
    if (screenStack.depth() > 1) {
      actionBar.setDisplayHomeAsUpEnabled(false)
      drawerToggle.isDrawerIndicatorEnabled = false
      actionBar.setDisplayHomeAsUpEnabled(true)
    } else {
      drawerToggle.isDrawerIndicatorEnabled = true
    }
    refreshTitle()
  }

  private fun refreshTitle() {
    if (!drawerLayout.isDrawerOpen(drawerContent)) {
      val screen = screenStack.peek()
      if (screen != null) {
        val title = screen.title
        if (title != null) {
          actionBar.title = title
          return
        }
      }
    }
    actionBar.title = "War Worlds 2"
  }

  companion object {
    private val log = Log("DrawerController")
  }

  init {
    this.activity = Preconditions.checkNotNull(activity)
    this.actionBar = Preconditions.checkNotNull(actionBar)
    this.drawerLayout = Preconditions.checkNotNull(drawerLayout)
    this.drawerContent = Preconditions.checkNotNull(drawerContent)
    this.screenStack = Preconditions.checkNotNull(screenStack)
    actionBar.show()
    actionBar.setDisplayHomeAsUpEnabled(true)
    actionBar.setHomeButtonEnabled(true)
    drawerToggle = object : ActionBarDrawerToggle(
        activity, drawerLayout,
        R.string.drawer_open,
        R.string.drawer_close) {
      override fun onDrawerClosed(view: View) {
        super.onDrawerClosed(view)
        refreshTitle()
      }

      override fun onDrawerOpened(drawerView: View) {
        super.onDrawerOpened(drawerView)
        refreshTitle()
        //searchListAdapter.setCursor(StarManager.i.getMyStars());
      }
    }
    drawerLayout.addDrawerListener(drawerToggle)
    drawerToggle.syncState()
    screenStack.addScreenStackStateUpdatedCallback(object : ScreenStackStateUpdatedCallback {
      override fun onStackChanged() {
        updateBackButton()
      }
    })
    navigationView = drawerContent.findViewById(R.id.navigation_view)
    navigationView.setNavigationItemSelectedListener { item: MenuItem ->
      when (item.itemId) {
        R.id.nav_starfield -> {
          screenStack.home()
          screenStack.push(StarfieldScreen())
        }
        R.id.nav_star_search -> {
          screenStack.home()
          screenStack.push(StarSearchScreen())
        }
        R.id.nav_empire -> {
          screenStack.home()
          screenStack.push(EmpireScreen())
        }
        R.id.nav_sitrep -> {
          screenStack.home()
          screenStack.push(SitReportScreen())
        }
      }
      closeDrawer()
      true
    }

    // TODO: update this if your icon changes
    // Replace the empire icon with... your empire's icon.
    val empireMenuItem = navigationView.menu.findItem(R.id.nav_empire)
    App.server.waitForHello(Runnable {
      App.taskRunner.runTask(Runnable {
        val url = ImageHelper.getEmpireImageUrl(activity, EmpireManager.getMyEmpire(), 48, 48)
        val target: Target = object : Target {
          override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
            empireMenuItem.icon = BitmapDrawable(activity.resources, bitmap)
          }

          override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            empireMenuItem.icon = errorDrawable
          }

          override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            if (placeHolderDrawable != null) {
              empireMenuItem.icon = placeHolderDrawable
            }
          }
        }
        // Picasso only keeps a weak reference to the target, but we want to keep it alive (at least
        // as long as the nav menu is alive), so add it to a tag in the view.
        navigationView.setTag(R.id.target_tag, target)
        Picasso.get().load(url).into(target)
      }, Threads.UI)
    })
  }
}