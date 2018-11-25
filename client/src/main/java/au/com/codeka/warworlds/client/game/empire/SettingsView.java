package au.com.codeka.warworlds.client.game.empire;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.concurrency.Threads;

public class SettingsView extends ScrollView {
  public interface Callback {
    void onPatreonConnectClick(PatreonConnectCompleteCallback completeCallback);
  }

  public interface PatreonConnectCompleteCallback {
    void onPatreonConnectComplete(String msg);
  }

  private final Callback callback;

  public SettingsView(Context context, @Nonnull Callback callback) {
    super(context);
    this.callback = callback;
    inflate(context, R.layout.empire_settings, this);

    final Button patreonBtn = findViewById(R.id.patreon_btn);
    patreonBtn.setOnClickListener(
        (v) -> {
          patreonBtn.setEnabled(false);
          callback.onPatreonConnectClick(
              (msg) -> {
                App.i.getTaskRunner().runTask(() -> {
                  patreonBtn.setEnabled(true);
                  TextView msgView = findViewById(R.id.patreon_complete);
                  msgView.setVisibility(View.VISIBLE);
                  msgView.setText(msg);
                }, Threads.UI);
              }
          );
        });
  }
}
