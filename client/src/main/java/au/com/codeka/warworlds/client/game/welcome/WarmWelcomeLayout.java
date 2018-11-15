package au.com.codeka.warworlds.client.game.welcome;

import android.content.Context;
import android.widget.RelativeLayout;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.ctrl.TransparentWebView;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;

/**
 * Layout for {@link WarmWelcomeScreen}.
 */
public class WarmWelcomeLayout extends RelativeLayout {
  public interface Callbacks {
    void onStartClick();
    void onPrivacyPolicyClick();
    void onHelpClick();
  }

  private final Callbacks callbacks;

  public WarmWelcomeLayout(Context context, @Nonnull Callbacks callbacks) {
    super(context);
    inflate(context, R.layout.warm_welcome, this);
    this.callbacks = callbacks;

    ViewBackgroundGenerator.setBackground(this);

    TransparentWebView welcome = findViewById(R.id.welcome);
    String msg = TransparentWebView.getHtmlFile(getContext(), "html/warm-welcome.html");
    welcome.loadHtml("html/skeleton.html", msg);

    findViewById(R.id.next_btn).setOnClickListener(v -> callbacks.onStartClick());
    findViewById(R.id.help_btn).setOnClickListener(v -> callbacks.onHelpClick());
    findViewById(R.id.privacy_policy_btn).setOnClickListener(v -> callbacks.onPrivacyPolicyClick());
  }
}
