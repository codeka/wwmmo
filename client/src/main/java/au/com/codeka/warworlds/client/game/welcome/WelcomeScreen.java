package au.com.codeka.warworlds.client.game.welcome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.game.starfield.StarfieldScreen;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.ui.Screen;
import au.com.codeka.warworlds.client.ui.ScreenContext;
import au.com.codeka.warworlds.client.ui.ShowInfo;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.UrlFetcher;
import au.com.codeka.warworlds.client.util.Version;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The "Welcome" screen is what you see when you first start the game, it has a view for showing
 * news, letting you change your empire and so on.
 */
public class WelcomeScreen extends Screen {
  private static final Log log = new Log("WelcomeScreen");

  /** URL of RSS content to fetch and display in the motd view. */
  private static final String MOTD_RSS = "http://www.war-worlds.com/forum/announcements/rss";

  private ScreenContext context;
  @Nullable private WelcomeLayout welcomeLayout;
  @Nullable private String motd;

  @Override
  public void onCreate(ScreenContext context, ViewGroup container) {
    super.onCreate(context, container);

    this.context = context;
    welcomeLayout = new WelcomeLayout(context.getActivity(), layoutCallbacks);

    App.i.getEventBus().register(eventHandler);

    refreshWelcomeMessage();

    // TODO
    //optionsButton.setOnClickListener(v ->
    //    getFragmentTransitionManager().replaceFragment(GameSettingsFragment.class));

    if (GameSettings.i.getString(GameSettings.Key.EMAIL_ADDR).isEmpty()) {
      welcomeLayout.setSignInText(R.string.signin);
    } else {
      welcomeLayout.setSignInText(R.string.switch_user);
    }
  }

  @Override
  public ShowInfo onShow() {
    welcomeLayout.setConnectionStatus(false, null);
    updateServerState(App.i.getServer().getCurrState());

    if (EmpireManager.i.hasMyEmpire()) {
      welcomeLayout.refreshEmpireDetails(EmpireManager.i.getMyEmpire());
    }

    if (motd != null) {
      welcomeLayout.updateWelcomeMessage(motd);
    }

/*
          String currAccountName = prefs.getString("AccountName", null);
          if (currAccountName != null && currAccountName.endsWith("@anon.war-worlds.com")) {
            Button reauthButton = (Button) findViewById(R.id.reauth_btn);
            reauthButton.setText("Sign in");
          }*/
          maybeShowSignInPrompt();
//        }
//      }
//    });
    return ShowInfo.builder().view(welcomeLayout).toolbarVisible(false).build();
  }

  @Override
  public void onDestroy() {
    App.i.getEventBus().unregister(eventHandler);
  }

  private void maybeShowSignInPrompt() {
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

  private void refreshWelcomeMessage() {
    App.i.getTaskRunner().runTask(() -> {
      InputStream ins;
      try {
        ins = UrlFetcher.fetchStream(MOTD_RSS);
      } catch (IOException e) {
        log.warning("Error loading MOTD: %s", MOTD_RSS, e);
        return;
      }

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setValidating(false);

      Document doc;
      try {
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        doc = builder.parse(ins);
      } catch (Exception e) {
        log.warning("Error parsing MOTD: %s", MOTD_RSS, e);
        return;
      }

      SimpleDateFormat inputFormat =
          new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy h:mm a", Locale.US);

      final StringBuilder motd = new StringBuilder();
      if (doc != null) {
        NodeList itemNodes = doc.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
          Element itemElem = (Element) itemNodes.item(i);
          String title = itemElem.getElementsByTagName("title").item(0).getTextContent();
          String content = itemElem.getElementsByTagName("description").item(0).getTextContent();
          String pubDate = itemElem.getElementsByTagName("pubDate").item(0).getTextContent();
          String link = itemElem.getElementsByTagName("link").item(0).getTextContent();

          try {
            Date date = inputFormat.parse(pubDate);
            motd.append("<h1>");
            motd.append(outputFormat.format(date));
            motd.append("</h1>");
          } catch (ParseException e) {
            // Shouldn't ever happen.
          }

          motd.append("<h2>");
          motd.append(title);
          motd.append("</h2>");
          motd.append(content);
          motd.append("<div style=\"text-align: right; border-bottom: dashed 1px #fff; "
              + "padding-bottom: 4px;\">");
          motd.append("<a href=\"");
          motd.append(link);
          motd.append("\">");
          motd.append("View forum post");
          motd.append("</a></div>");
        }
      }

      App.i.getTaskRunner().runTask(() -> {
        this.motd = motd.toString();
        if (welcomeLayout != null) {
          welcomeLayout.updateWelcomeMessage(motd.toString());
        }
      }, Threads.UI);
    }, Threads.BACKGROUND);
  }

  private void updateServerState(ServerStateEvent event) {
    checkNotNull(welcomeLayout);

    if (event.getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      long maxMemoryBytes = Runtime.getRuntime().maxMemory();
      Context context = App.i.getApplicationContext();
      ActivityManager activityManager =
          checkNotNull((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
      int memoryClass = activityManager.getMemoryClass();

      DecimalFormat formatter = new DecimalFormat("#,##0");
      String msg = String.format(Locale.ENGLISH,
          "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s", memoryClass,
          formatter.format(maxMemoryBytes), Version.string());
      welcomeLayout.setConnectionStatus(true, msg);
    } else {
      if (event.getState() == ServerStateEvent.ConnectionState.ERROR) {
        handleConnectionError(event);
      }
      String msg = String.format("%s - %s", event.getUrl(), event.getState());
      welcomeLayout.setConnectionStatus(false, msg);
    }
  }

  /**
   * Called when get notified that there was some error connecting to the server. Sometimes we'll
   * be able to fix the error and try again.
   */
  private void handleConnectionError(ServerStateEvent event) {
    if (event.getLoginStatus() == null) {
      // Nothing we can do.
      log.debug("Got an error, but login status is null.");
    }
  }

  private final WelcomeLayout.Callbacks layoutCallbacks = new WelcomeLayout.Callbacks() {
    @Override
    public void onStartClick() {
      context.home();
      context.pushScreen(new StarfieldScreen());
    }

    @Override
    public void onHelpClick() {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
      context.startActivity(i);
    }

    @Override
    public void onWebsiteClick() {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://www.war-worlds.com/"));
      context.startActivity(i);
    }

    @Override
    public void onSignInClick() {
      context.pushScreen(new SignInScreen());
    }
  };

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onServerStateUpdated(ServerStateEvent event) {
      updateServerState(event);
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      Empire myEmpire = EmpireManager.i.getMyEmpire();
      if (myEmpire.id.equals(empire.id) && welcomeLayout != null) {
        welcomeLayout.refreshEmpireDetails(empire);
      }
    }
  };
}