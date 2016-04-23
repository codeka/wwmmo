package au.com.codeka.warworlds.client.welcome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import java.text.DecimalFormat;
import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.Log;

/**
 * The "Welcome" activity is what you see when you first start the game, it has a view for showing
 * news, letting you change your empire and so on.
 */
public class WelcomeFragment extends Fragment {
  private static final Log log = new Log("WelcomeFragment");
  private Button startGameButton;
  private TextView connectionStatus;
//  private HelloWatcher helloWatcher;
  private TextView realmName;

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.frag_welcome, container, false);
    ViewBackgroundGenerator.setBackground(rootView);

    startGameButton =
        (Button) Preconditions.checkNotNull(rootView.findViewById(R.id.start_game_btn));
    connectionStatus =
        (TextView) Preconditions.checkNotNull(rootView.findViewById(R.id.connection_status));
    realmName =
        (TextView) Preconditions.checkNotNull(rootView.findViewById(R.id.realm_name));
    final Button realmSelectButton =
        (Button) Preconditions.checkNotNull(rootView.findViewById(R.id.realm_select_btn));
    final Button optionsButton =
        (Button) Preconditions.checkNotNull(rootView.findViewById(R.id.options_btn));

    refreshWelcomeMessage();

    realmSelectButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        //startActivity(new Intent(context, RealmSelectActivity.class));
      }
    });

    optionsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //startActivity(new Intent(context, GlobalOptionsActivity.class));
      }
    });

    startGameButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        //final Intent intent = new Intent(context, StarfieldActivity.class);
        //startActivity(intent);
      }
    });

    Preconditions.checkNotNull(rootView.findViewById(R.id.help_btn)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://www.war-worlds.com/doc/getting-started"));
            startActivity(i);
          }
        });

    Preconditions.checkNotNull(rootView.findViewById(R.id.website_btn)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://www.war-worlds.com/"));
            startActivity(i);
          }
        });

    Preconditions.checkNotNull(rootView.findViewById(R.id.reauth_btn)).setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onReauthClick();
          }
        });

    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();

    startGameButton.setEnabled(false);
    realmName.setText(String.format(
        Locale.getDefault(),
        getString(R.string.realm_label),
        "Main" /*RealmContext.i.getCurrentRealm().getDisplayName()*/));

    //final TextView empireName = (TextView) Preconditions.checkNotNull()findViewById(R.id.empire_name);
    //final ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
    //empireName.setText("");
    //empireIcon.setImageBitmap(null);

 //   helloWatcher = new HelloWatcher();
    //ServerGreeter.addHelloWatcher(helloWatcher);

    //ShieldManager.eventBus.register(eventHandler);

//    ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
//      @Override
 //     public void onHelloComplete(boolean success, ServerGreeter.ServerGreeting greeting) {
//        if (success) {
          // we'll display a bit of debugging info along with the 'connected' message
          long maxMemoryBytes = Runtime.getRuntime().maxMemory();
          int memoryClass = ((ActivityManager) getActivity()
              .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

          DecimalFormat formatter = new DecimalFormat("#,##0");
          String msg = String.format(Locale.ENGLISH,
              "Connected\r\nMemory Class: %d - Max bytes: %s\r\nVersion: %s%s", memoryClass,
              formatter.format(maxMemoryBytes), "0.1" /*Util.getVersion()*/,
              /*Util.isDebug() ?*/ " (debug)" /*: " (rel)"*/);
          connectionStatus.setText(msg);
          startGameButton.setEnabled(true);
/*
          MyEmpire empire = EmpireManager.i.getEmpire();
          if (empire != null) {
            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(context, empire));
          }
*//*
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

  private void refreshWelcomeMessage() {/*
    new BackgroundRunner<Document>() {
      @Override
      protected Document doInBackground() {
        String url = (String) Util.getProperties().get("welcome.rss");
        try {
          // we have to use the built-in one because our special version assume all requests go
          // to the game server...
          HttpClient httpClient = new DefaultHttpClient();
          HttpGet get = new HttpGet(url);
          get.addHeader(HTTP.USER_AGENT, "wwmmo/" + Util.getVersion());
          HttpResponse response = httpClient.execute(new HttpGet(url));
          if (response.getStatusLine().getStatusCode() == 200) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(response.getEntity().getContent());
          }
        } catch (Exception e) {
          log.error("Error fetching MOTD.", e);
        }

        return null;
      }

      @Override
      protected void onComplete(Document rss) {
        SimpleDateFormat inputFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        SimpleDateFormat outputFormat =
            new SimpleDateFormat("dd MMM yyyy h:mm a", Locale.US);

        StringBuilder motd = new StringBuilder();
        if (rss != null) {
          NodeList itemNodes = rss.getElementsByTagName("item");
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

        TransparentWebView motdView = (TransparentWebView) findViewById(R.id.motd);
        motdView.loadHtml("html/skeleton.html", motd.toString());
      }
    }.execute();*/
  }

  @Override
  public void onPause() {
    super.onPause();
    //ServerGreeter.removeHelloWatcher(helloWatcher);
    //ShieldManager.eventBus.unregister(eventHandler);
  }
/*
  private Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      // if it's the same as our empire, we'll need to update the icon we're currently showing.
      MyEmpire empire = EmpireManager.i.getEmpire();
      if (event.id == Integer.parseInt(empire.getKey())) {
        ImageView empireIcon = (ImageView) findViewById(R.id.empire_icon);
        empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(context, empire));
      }
    }
  };
*//*
  private class HelloWatcher implements ServerGreeter.HelloWatcher {
    private int numRetries = 0;

    @Override
    public void onRetry(final int retries) {
      numRetries = retries + 1;
      connectionStatus.setText(String.format("Retrying (#%d)...", numRetries));
    }

    @Override
    public void onAuthenticating() {
      if (numRetries > 0) {
        return;
      }
      connectionStatus.setText("Authenticating...");
    }

    @Override
    public void onConnecting() {
      if (numRetries > 0) {
        return;
      }
      connectionStatus.setText("Connecting...");
    }
  }*/
}