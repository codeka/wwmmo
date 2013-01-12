package au.com.codeka.warworlds.game;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;

public class ChatActivity extends BaseActivity
                          implements ChatManager.MessageAddedListener {
    private ScrollView mScrollView;
    private LinearLayout mChatOutput;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chat);

        mScrollView = (ScrollView) findViewById(R.id.scroll_view);
        mChatOutput = (LinearLayout) findViewById(R.id.chat_output);

        final EditText chatMsg = (EditText) findViewById(R.id.chat_text);

        Button send = (Button) findViewById(R.id.chat_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatMessage msg = new ChatMessage();
                msg.setMessage(chatMsg.getText().toString());
                chatMsg.setText("");

                ChatManager.getInstance().postMessage(msg);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ChatManager.getInstance().addMessageAddedListener(this);
        refreshMessages();
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatManager.getInstance().removeMessageAddedListener(this);
    }

    @Override
    public void onMessageAdded(ChatMessage msg) {
        appendMessage(msg);
    }

    private void appendMessage(ChatMessage msg) {
        TextView tv = new TextView(this);
        tv.setText(msg.getMessage());
        mChatOutput.addView(tv);

        // need to wait for it to settle before we scroll again
        mScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 1);
    }

    private void refreshMessages() {
        mChatOutput.removeAllViews();

        ChatMessage[] msgs = ChatManager.getInstance().getLastMessages(10);
        for(int i = msgs.length - 1; i >= 0; i--) {
            ChatMessage msg = msgs[i];
            if (msg == null) {
                continue;
            }

            appendMessage(msg);
        }
    }

}
