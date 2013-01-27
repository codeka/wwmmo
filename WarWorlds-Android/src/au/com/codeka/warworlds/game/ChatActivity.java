package au.com.codeka.warworlds.game;

import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.EmpireManager;

public class ChatActivity extends BaseActivity
                          implements ChatManager.MessageAddedListener {
    private ScrollView mScrollView;
    private LinearLayout mChatOutput;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chat);

        mScrollView = (ScrollView) findViewById(R.id.scroll_view);
        mChatOutput = (LinearLayout) findViewById(R.id.chat_output);
        mHandler = new Handler();

        final EditText chatMsg = (EditText) findViewById(R.id.chat_text);

        chatMsg.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL) {
                    sendCurrentChat();
                    return true;
                }
                return false;
            }
        });

        Button send = (Button) findViewById(R.id.chat_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCurrentChat();
            }
        });
    }

    private void sendCurrentChat() {
        EditText chatMsg = (EditText) findViewById(R.id.chat_text);
        if (chatMsg.getText().toString().equals("")) {
            return;
        }

        ChatMessage msg = new ChatMessage();
        msg.setMessage(chatMsg.getText().toString());
        msg.setEmpire(EmpireManager.getInstance().getEmpire());
        chatMsg.setText("");

        ChatManager.getInstance().postMessage(msg);

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
    public void onMessageAdded(final ChatMessage msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                appendMessage(msg);
            }
        });
    }

    private void appendMessage(final ChatMessage msg) {
        TextView tv = new TextView(this);
        tv.setText(msg.format());
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

        List<ChatMessage> msgs = ChatManager.getInstance().getAllMessages();
        for(ChatMessage msg : msgs) {
            appendMessage(msg);
        }
    }

}
