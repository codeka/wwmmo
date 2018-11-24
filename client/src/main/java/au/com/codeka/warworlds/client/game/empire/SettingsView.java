package au.com.codeka.warworlds.client.game.empire;

import android.content.Context;
import android.widget.ScrollView;
import au.com.codeka.warworlds.client.R;

public class SettingsView extends ScrollView {
  public SettingsView(Context context) {
    super(context);
    inflate(context, R.layout.empire_settings, this);
  }
}
