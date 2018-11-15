package au.com.codeka.warworlds.client.game.chat;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.client.R;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bottom pane that contains the "send" text box and button.
 */
public class SendBottomPane extends RelativeLayout {
  /** Implement this to get notified of events. */
  public interface Callback {
    void onSendClick(String message);
  }

  private final Callback callback;
  private final EditText message;

  public SendBottomPane(Context context, @Nonnull Callback callback) {
    super(context, null);
    this.callback = checkNotNull(callback);

    inflate(context, R.layout.chat_send_bottom_pane, this);

    message = findViewById(R.id.message);
    findViewById(R.id.send_btn).setOnClickListener(this::onSendClick);

 //   message.setOnEditorActionListener((v, actionId, event) -> {
 //     if (actionId == EditorInfo.IME_NULL) {
 //       onSendClick(v);
 //       return true;
 //     }
 //     return false;
 //   });

  }

  private void onSendClick(View v) {
    String msg = message.getText().toString();
    if (msg.isEmpty()) {
      return;
    }

    callback.onSendClick(msg);
    message.setText("");
  }
}
