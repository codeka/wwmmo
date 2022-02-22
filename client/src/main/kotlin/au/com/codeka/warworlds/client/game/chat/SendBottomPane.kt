package au.com.codeka.warworlds.client.game.chat

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import au.com.codeka.warworlds.client.R

/**
 * Bottom pane that contains the "send" text box and button.
 */
@SuppressLint("ViewConstructor") //  Must be contructed in code.
class SendBottomPane(context: Context?, private val callback: Callback) : RelativeLayout(context, null) {
  /** Implement this to get notified of events.  */
  interface Callback {
    fun onSendClick(message: String)
  }

  private val message: EditText
  private fun onSendClick() {
    val msg = message.text.toString()
    if (msg.isEmpty()) {
      return
    }
    callback.onSendClick(msg)
    message.setText("")
  }

  init {
    View.inflate(context, R.layout.chat_send_bottom_pane, this)
    message = findViewById(R.id.message)
    findViewById<View>(R.id.send_btn).setOnClickListener { onSendClick() }

    //   message.setOnEditorActionListener((v, actionId, event) -> {
    //     if (actionId == EditorInfo.IME_NULL) {
    //       onSendClick(v);
    //       return true;
    //     }
    //     return false;
    //   });
  }
}
