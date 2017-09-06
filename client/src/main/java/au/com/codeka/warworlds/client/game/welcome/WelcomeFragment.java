package au.com.codeka.warworlds.client.game.welcome;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.game.starfield.StarfieldFragment;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.UrlFetcher;
import au.com.codeka.warworlds.client.util.Version;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The "Welcome" activity is what you see when you first start the game, it has a view for showing
 * news, letting you change your empire and so on.
 */
public class WelcomeFragment extends BaseFragment {
  private static final Log log = new Log("WelcomeFragment");

  /** URL of RSS content to fetch and display in the motd view. */
  private static final String MOTD_RSS = "http://www.war-worlds.com/forum/announcements/rss";

  private View rootView;
  private Button startButton;
  private Button signInButton;
  private TextView connectionStatus;
  private TextView empireName;
  private ImageView empireIcon;
  private TransparentWebView motdView;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_welcome;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    rootView = view;
    ViewBackgroundGenerator.setBackground(view);

    startButton = checkNotNull(view.findViewById(R.id.start_btn));
    signInButton = view.findViewById(R.id.signin_btn);
    motdView = checkNotNull(view.findViewById(R.id.motd));
    empireName = checkNotNull(view.findViewById(R.id.empire_name));
    empireIcon = checkNotNull(view.findViewById(R.id.empire_icon));
    connectionStatus =
        checkNotNull(view.findViewById(R.id.connection_status));
    final Button optionsButton =
        checkNotNull(view.findViewById(R.id.options_btn));

    refreshWelcomeMessage();

    // TODO
    //optionsButton.setOnClickListener(v ->
    //    getFragmentTransitionManager().replaceFragment(GameSettingsFragment.class));

    startButton.setOnClickListener(v ->
        getFragmentTransitionManager().replaceFragment(StarfieldFragment.class));

    view.findViewById(R.id.help_btn).setOnClickListener(v -> {
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
          startActivity(i);
        });

    view.findViewById(R.id.website_btn).setOnClickListener(v -> {
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse("http://www.war-worlds.com/"));
          startActivity(i);
        });

    if (GameSettings.i.getString(GameSettings.Key.EMAIL_ADDR).isEmpty()) {
      signInButton.setTag(R.string.signin);
    } else {
      signInButton.setText(R.string.switch_user);
    }
    signInButton.setOnClickListener(v -> onSignInClick());
  }

  @Override
  public void onResume() {
    super.onResume();

    startButton.setEnabled(false);
    updateServerState(App.i.getServer().getCurrState());
    App.i.getEventBus().register(eventHandler);

    if (EmpireManager.i.hasMyEmpire()) {
      refreshEmpireDetails(EmpireManager.i.getMyEmpire());
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
  }

  private void refreshEmpireDetails(Empire empire) {
    empireName.setText(empire.display_name);
    Picasso.with(getContext())
        .load(ImageHelper.getEmpireImageUrl(getContext(), empire, 20, 20))
        .into(empireIcon);
  }

  private void onSignInClick() {
    getFragmentTransitionManager().replaceFragment(SignInFragment.class);
  }

  @Override
  public void onPause() {
    super.onPause();
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

  private void updateWelcomeMessage(String html) {
    motdView.loadHtml("html/skeleton.html", html);
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

      App.i.getTaskRunner().runTask(() -> updateWelcomeMessage(motd.toString()), Threads.UI);
    }, Threads.BACKGROUND);
  }

  private void updateServerState(ServerStateEvent event) {
    if (event.getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      long maxMemoryBytes = Runtime.getRuntime().maxMemory();
      int memoryClass = ((ActivityManager) getFragmentActivity()
          .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

      DecimalFormat formatter = new DecimalFormat("#,##0");
      String msg = String.format(Locale.ENGLISH,
          "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s", memoryClass,
          formatter.format(maxMemoryBytes), Version.string());
      connectionStatus.setText(msg);
      startButton.setEnabled(true);
    } else {
      if (event.getState() == ServerStateEvent.ConnectionState.ERROR) {
        handleConnectionError(event);
      }
      String msg = String.format("%s - %s", event.getUrl(), event.getState());
      connectionStatus.setText(msg);
      startButton.setEnabled(false);
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
      return;
    }

  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onServerStateUpdated(ServerStateEvent event) {
      updateServerState(event);
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      Empire myEmpire = EmpireManager.i.getMyEmpire();
      if (myEmpire.id.equals(empire.id)) {
        refreshEmpireDetails(empire);
      }
    }
  };
}