package au.com.codeka.warworlds.client.game.welcome

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.net.ServerStateEvent
import au.com.codeka.warworlds.client.net.auth.AuthAccountUpdate
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.util.UrlFetcher.fetchStream
import au.com.codeka.warworlds.client.util.Version.string
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Empire
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.common.base.Preconditions
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The "Welcome" screen is what you see when you first start the game, it has a view for showing
 * news, letting you change your empire and so on.
 */
class WelcomeScreen : Screen() {
  private lateinit var context: ScreenContext
  private lateinit var welcomeLayout: WelcomeLayout
  private var motd: String? = null

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    this.context = context

    welcomeLayout = WelcomeLayout(context.activity, layoutCallbacks)
    App.eventBus.register(eventHandler)
    refreshWelcomeMessage()

    // TODO
    //optionsButton.setOnClickListener(v ->
    //    getFragmentTransitionManager().replaceFragment(GameSettingsFragment.class));
  }

  override fun onShow(): ShowInfo {
    welcomeLayout.setConnectionStatus(false, "")
    updateServerState(App.server.currState)
    if (EmpireManager.hasMyEmpire()) {
      welcomeLayout.refreshEmpireDetails(EmpireManager.getMyEmpire())
    }
    if (motd != null) {
      welcomeLayout.updateWelcomeMessage(motd!!)
    }

    updateSignInButtonText(App.auth.account)

/*
          String currAccountName = prefs.getString("AccountName", null);
          if (currAccountName != null && currAccountName.endsWith("@anon.war-worlds.com")) {
            Button reauthButton = (Button) findViewById(R.id.reauth_btn);
            reauthButton.setText("Sign in");
          }*/maybeShowSignInPrompt()
    //        }
//      }
//    });

    return ShowInfo.builder().view(welcomeLayout).toolbarVisible(false).build()
  }

  override fun onDestroy() {
    App.eventBus.unregister(eventHandler)
  }

  private fun maybeShowSignInPrompt() {
    /*final SharedPreferences prefs = Util.getSharedPreferences();
    int numStartsSinceSignInPrompt = prefs.getInt("NumStartsSinceSignInPrompt", 0);
    if (numStartsSinceSignInPrompt < 5) {
      prefs.edit().putInt("NumStartsSinceSignInPrompt", numStartsSinceSignInPrompt + 1).apply();
      return;
    }

    // set the count to -95, which means they won't get prompted for another 100 starts... should
    // be plenty to not be annoying, yet still be a useful prompt.
    prefs.edit().putInt("NumStartsSinceSignInPrompt", -95).apply();
    new StyledDialog.Builder(context)
        .setMessage(Html.fromHtml("<p>In order to ensure your empire is safe in the event you lose "
            + "your phone, it's recommended that you sign in. You must also sign in if you want to "
            + "access your empire from multiple devices.</p><p>Click \"Sign in\" below to sign in "
            + "with a Google account.</p>"))
        .setTitle("Sign in")
        .setNegativeButton("No, thanks", null)
        .setPositiveButton("Sign in", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            onReauthClick();
          }
        })
        .create().show();*/
  }

  private fun parseDate(pubDate: String, format: SimpleDateFormat): Date? {
    return try {
      format.parse(pubDate)
    } catch (e: ParseException) {
      null
    }
  }

  private fun refreshWelcomeMessage() {
    App.taskRunner.runTask(Runnable {
      val ins: InputStream? = try {
        fetchStream(MOTD_RSS)
      } catch (e: IOException) {
        log.warning("Error loading MOTD: %s", MOTD_RSS, e)
        return@Runnable
      }
      val builderFactory = DocumentBuilderFactory.newInstance()
      builderFactory.isValidating = false
      val doc: Document = try {
        val builder = builderFactory.newDocumentBuilder()
        builder.parse(ins)
      } catch (e: Exception) {
        log.warning("Error parsing MOTD: %s", MOTD_RSS, e)
        return@Runnable
      }
      val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
      val outputFormat = SimpleDateFormat("dd MMM yyyy h:mm a", Locale.US)
      val motd = StringBuilder()
      val itemNodes = doc.getElementsByTagName("item")
      for (i in 0 until itemNodes.length) {
        val itemElem = itemNodes.item(i) as Element
        val title = itemElem.getElementsByTagName("title").item(0).textContent
        val content = itemElem.getElementsByTagName("description").item(0).textContent
        val pubDate = itemElem.getElementsByTagName("pubDate").item(0).textContent
        val link = itemElem.getElementsByTagName("link").item(0).textContent
        val date = parseDate(pubDate, inputFormat)
        if (date != null) {
          motd.append("<h1>")
          motd.append(outputFormat.format(date))
          motd.append("</h1>")
        }
        motd.append("<h2>")
        motd.append(title)
        motd.append("</h2>")
        motd.append(content)
        motd.append("<div style=\"text-align: right; border-bottom: dashed 1px #fff; "
            + "padding-bottom: 4px;\">")
        motd.append("<a href=\"")
        motd.append(link)
        motd.append("\">")
        motd.append("View forum post")
        motd.append("</a></div>")
      }
      App.taskRunner.runTask({
        this.motd = motd.toString()
        welcomeLayout.updateWelcomeMessage(motd.toString())
      }, Threads.UI)
    }, Threads.BACKGROUND)
  }

  private fun updateServerState(event: ServerStateEvent) {
    Preconditions.checkNotNull(welcomeLayout)
    if (event.state === ServerStateEvent.ConnectionState.CONNECTED) {
      val maxMemoryBytes = Runtime.getRuntime().maxMemory()
      val context = App.applicationContext
      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val memoryClass = activityManager.memoryClass
      val formatter = DecimalFormat("#,##0")
      val msg = String.format(Locale.ENGLISH,
          "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s", memoryClass,
          formatter.format(maxMemoryBytes), string())
      welcomeLayout.setConnectionStatus(true, msg)
    } else {
      if (event.state === ServerStateEvent.ConnectionState.ERROR) {
        handleConnectionError(event)
      }
      val msg = String.format("%s - %s", event.url, event.state)
      welcomeLayout.setConnectionStatus(false, msg)
    }
  }

  /**
   * Called when get notified that there was some error connecting to the server. Sometimes we'll
   * be able to fix the error and try again.
   */
  private fun handleConnectionError(event: ServerStateEvent) {
    if (event.loginStatus == null) {
      // Nothing we can do.
      log.debug("Got an error, but login status is null.")
    }
  }

  private fun updateSignInButtonText(account: GoogleSignInAccount?) {
    if (account == null) {
      welcomeLayout.setSignInText(R.string.signin)
    } else {
      welcomeLayout.setSignInText(R.string.switch_user)
    }
  }

  private val layoutCallbacks: WelcomeLayout.Callbacks = object : WelcomeLayout.Callbacks {
    override fun onStartClick() {
      context.home()
      context.pushScreen(StarfieldScreen())
    }

    override fun onHelpClick() {
      val i = Intent(Intent.ACTION_VIEW)
      i.data = Uri.parse("http://www.war-worlds.com/doc/getting-started")
      context.startActivity(i)
    }

    override fun onWebsiteClick() {
      val i = Intent(Intent.ACTION_VIEW)
      i.data = Uri.parse("http://www.war-worlds.com/")
      context.startActivity(i)
    }

    override fun onSignInClick() {
      context.pushScreen(SignInScreen(false /* immediate */))
    }
  }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onServerStateUpdated(event: ServerStateEvent) {
      updateServerState(event)
    }

    @EventHandler
    fun onEmpireUpdated(empire: Empire) {
      val myEmpire = EmpireManager.getMyEmpire()
      if (myEmpire.id == empire.id) {
        welcomeLayout.refreshEmpireDetails(empire)
      }
    }

    @EventHandler
    fun onAuthUpdated(auth: AuthAccountUpdate) {
      updateSignInButtonText(auth.account)
    }
  }

  companion object {
    private val log = Log("WelcomeScreen")

    /** URL of RSS content to fetch and display in the motd view.  */
    private const val MOTD_RSS = "http://www.war-worlds.com/forum/announcements/rss"
  }
}