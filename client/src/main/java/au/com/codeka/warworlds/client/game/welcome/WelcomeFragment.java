package au.com.codeka.warworlds.client.game.welcome;

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

import com.google.common.base.Preconditions;
import com.squareup.picasso.Picasso;

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
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.net.ServerStateEvent;
import au.com.codeka.warworlds.client.game.starfield.StarfieldFragment;
import au.com.codeka.warworlds.client.util.UrlFetcher;
import au.com.codeka.warworlds.client.util.Version;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;

/**
 * The "Welcome" activity is what you see when you first start the game, it has a view for showing
 * news, letting you change your empire and so on.
 */
public class WelcomeFragment extends BaseFragment {
  private static final Log log = new Log("WelcomeFragment");

  /** URL of RSS content to fetch and display in the motd view. */
  private static final String MOTD_RSS = "http://www.war-worlds.com/forum/announcements/rss";

  private Button startButton;
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
    ViewBackgroundGenerator.setBackground(view);

    startButton = (Button) Preconditions.checkNotNull(view.findViewById(R.id.start_btn));
    motdView = (TransparentWebView) Preconditions.checkNotNull(view.findViewById(R.id.motd));
    empireName = (TextView) Preconditions.checkNotNull(view.findViewById(R.id.empire_name));
    empireIcon = (ImageView) Preconditions.checkNotNull(view.findViewById(R.id.empire_icon));
    connectionStatus =
        (TextView) Preconditions.checkNotNull(view.findViewById(R.id.connection_status));
    final Button optionsButton =
        (Button) Preconditions.checkNotNull(view.findViewById(R.id.options_btn));

    refreshWelcomeMessage();

    optionsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        getFragmentTransitionManager().replaceFragment(GameSettingsFragment.class);
      }
    });

    startButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        getFragmentTransitionManager().replaceFragment(StarfieldFragment.class);
      }
    });

    Preconditions.checkNotNull(view.findViewById(R.id.help_btn)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
            startActivity(i);
          }
        });

    Preconditions.checkNotNull(view.findViewById(R.id.website_btn)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://www.war-worlds.com/"));
            startActivity(i);
          }
        });

    Preconditions.checkNotNull(view.findViewById(R.id.reauth_btn)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onReauthClick();
          }
        });
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

  @Override
  public void onPause() {
    super.onPause();
    App.i.getEventBus().unregister(eventHandler);
  }

  private void onReauthClick() {
    //final Intent intent = new Intent(getActivity(), AccountsActivity.class);
    //startActivity(intent);
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
    App.i.getTaskRunner().runTask(new Runnable() {
      @Override
      public void run() {
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

        App.i.getTaskRunner().runTask(new Runnable() {
          @Override
          public void run() {
            updateWelcomeMessage(motd.toString());
          }
        }, Threads.UI);
      }
    }, Threads.BACKGROUND);
  }

  private void updateServerState(ServerStateEvent event) {
    if (event.getState() == ServerStateEvent.ConnectionState.CONNECTED) {
      long maxMemoryBytes = Runtime.getRuntime().maxMemory();
      int memoryClass = ((ActivityManager) getActivity()
          .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

      DecimalFormat formatter = new DecimalFormat("#,##0");
      String msg = String.format(Locale.ENGLISH,
          "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s", memoryClass,
          formatter.format(maxMemoryBytes), Version.string());
      connectionStatus.setText(msg);
      startButton.setEnabled(true);
    } else {
      String msg = String.format("%s - %s", event.getUrl(), event.getState());
      connectionStatus.setText(msg);
      startButton.setEnabled(false);
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
      if (myEmpire != null && myEmpire.id.equals(empire.id)) {
        refreshEmpireDetails(empire);
      }
    }

/*    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      // if it's the same as our empire, we'll need to update the icon we're currently showing.
      MyEmpire empire = EmpireManager.i.getEmpire();
      if (event.id == Integer.parseInt(empire.getKey())) {
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(context, empire));
      }
    }*/
  };
}