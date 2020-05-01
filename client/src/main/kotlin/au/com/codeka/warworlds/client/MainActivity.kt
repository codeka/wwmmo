package au.com.codeka.warworlds.client

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import au.com.codeka.warworlds.client.ctrl.DebugView
import au.com.codeka.warworlds.client.ctrl.drawer.DrawerController
import au.com.codeka.warworlds.client.game.starfield.StarfieldManager
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen
import au.com.codeka.warworlds.client.game.welcome.CreateEmpireScreen
import au.com.codeka.warworlds.client.game.welcome.WarmWelcomeScreen
import au.com.codeka.warworlds.client.game.welcome.WelcomeScreen
import au.com.codeka.warworlds.client.net.auth.SIGN_IN_COMPLETE_RESULT_CODE
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView
import au.com.codeka.warworlds.client.ui.ScreenStack
import au.com.codeka.warworlds.client.util.GameSettings
import au.com.codeka.warworlds.client.util.GameSettings.getBoolean
import au.com.codeka.warworlds.client.util.GameSettings.getString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.common.base.Preconditions


class MainActivity : AppCompatActivity() {
  // Will be non-null between of onCreate/onDestroy.
  lateinit var starfieldManager: StarfieldManager
    private set

  // Will be non-null between onCreate/onDestroy.
  private var screenStack: ScreenStack? = null

  // Will be non-null between onCreate/onDestroy.
  private var drawerController: DrawerController? = null
  private var fragmentContainer: FrameLayout? = null
  private var topPane: View? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    topPane = findViewById(R.id.top_pane)
    setSupportActionBar(findViewById(R.id.toolbar))

    App.auth.silentSignIn(this)

    val renderSurfaceView = findViewById<RenderSurfaceView>(R.id.render_surface)
    renderSurfaceView.setRenderer()

    starfieldManager = StarfieldManager(renderSurfaceView)
    starfieldManager.create()

    val debugView = Preconditions.checkNotNull(findViewById<DebugView>(R.id.debug_view))
    debugView.setFrameCounter(renderSurfaceView.frameCounter)

    fragmentContainer = Preconditions.checkNotNull(findViewById(R.id.fragment_container))

    screenStack = ScreenStack(this, fragmentContainer!!)

    drawerController = DrawerController(
        this,
        screenStack!!,
        supportActionBar!!,
        findViewById(R.id.drawer_layout),
        findViewById(R.id.drawer_content))
    if (savedInstanceState != null) {
      // TODO: restore the view state?
    }

    if (!getBoolean(GameSettings.Key.WARM_WELCOME_SEEN)) {
      screenStack!!.push(WarmWelcomeScreen())
    } else if (getString(GameSettings.Key.COOKIE).isEmpty()) {
      screenStack!!.push(CreateEmpireScreen())
    } else {
      screenStack!!.push(WelcomeScreen())
    }
  }

  fun setToolbarVisible(visible: Boolean) {
    var marginSize: Int
    if (visible) {
      val typedValue = TypedValue()
      theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
      val attribute = intArrayOf(android.R.attr.actionBarSize)
      val array = obtainStyledAttributes(typedValue.resourceId, attribute)
      marginSize = array.getDimensionPixelSize(0, -1)
      array.recycle()

      // Adjust the margin by a couple of dp, the top pane has that strip of transparent pixels
      marginSize -= TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
      topPane!!.visibility = View.VISIBLE
      supportActionBar!!.show()
    } else {
      marginSize = 0
      topPane!!.visibility = View.GONE
      supportActionBar!!.hide()
    }
    (fragmentContainer!!.layoutParams as FrameLayout.LayoutParams).topMargin = marginSize
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == SIGN_IN_COMPLETE_RESULT_CODE) {
      val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
      App.auth.handleSignInResult(task)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        drawerController!!.toggleDrawer()
        return true
      }
    }
    return false
  }

  override fun onBackPressed() {
    if (!screenStack!!.backTo(StarfieldScreen::class.java)) {
      super.onBackPressed()
    }
  }

  public override fun onDestroy() {
    super.onDestroy()
    starfieldManager.destroy()
  }
}