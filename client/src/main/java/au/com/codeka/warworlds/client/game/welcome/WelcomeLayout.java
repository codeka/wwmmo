package au.com.codeka.warworlds.client.game.welcome;

import android.content.Context;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.common.proto.Empire;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Layout for the {@link WelcomeScreen}.
 */
public class WelcomeLayout extends RelativeLayout {
  public interface Callbacks {
    void onStartClick();
    void onHelpClick();
    void onWebsiteClick();
    void onSignInClick();
  }

  private final Callbacks callbacks;
  private final Button startButton;
  private final Button signInButton;
  private final TextView connectionStatus;
  private final TextView empireName;
  private final ImageView empireIcon;
  private final TransparentWebView motdView;

  public WelcomeLayout(Context context, @Nonnull Callbacks callbacks) {
    super(context, null);
    this.callbacks = callbacks;
    inflate(context, R.layout.welcome, this);
    ViewBackgroundGenerator.setBackground(this);

    startButton = checkNotNull(findViewById(R.id.start_btn));
    signInButton = findViewById(R.id.signin_btn);
    motdView = checkNotNull(findViewById(R.id.motd));
    empireName = checkNotNull(findViewById(R.id.empire_name));
    empireIcon = checkNotNull(findViewById(R.id.empire_icon));
    connectionStatus = checkNotNull(findViewById(R.id.connection_status));
    final Button optionsButton = checkNotNull(findViewById(R.id.options_btn));

    startButton.setOnClickListener(v -> callbacks.onStartClick());
    findViewById(R.id.help_btn).setOnClickListener(v -> callbacks.onHelpClick());
    findViewById(R.id.website_btn).setOnClickListener(v -> callbacks.onWebsiteClick());
    signInButton.setOnClickListener(v -> callbacks.onSignInClick());
  }

  public void refreshEmpireDetails(Empire empire) {
    empireName.setText(empire.display_name);
    Picasso.get()
        .load(ImageHelper.getEmpireImageUrl(getContext(), empire, 20, 20))
        .into(empireIcon);
  }

  public void updateWelcomeMessage(String html) {
    motdView.loadHtml("html/skeleton.html", html);
  }

  public void setConnectionStatus(boolean connected, @Nullable String message) {
    startButton.setEnabled(connected);
    if (message == null) {
      connectionStatus.setText("");
    } else {
      connectionStatus.setText(message);
    }
  }

  public void setSignInText(int resId) {
    signInButton.setText(resId);
  }
}
