package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import au.com.codeka.warworlds.game.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;

/**
 * This control displays the mini chat window, which displays recent chat
 * messages on each screen.
 */
public class MiniChatView extends LinearLayout {
    private Context mContext;

    private ScrollView mScrollView;
    private LinearLayout mMsgsContainer;
    private MessageAddedListener mMessageAddedListener;

    public MiniChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        this.setBackgroundColor(Color.argb(0xaa, 0, 0, 0));

        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0);
        mScrollView = new ScrollView(mContext);
        mScrollView.setLayoutParams(lp);
        this.addView(mScrollView);

        mMsgsContainer = new LinearLayout(mContext);
        mMsgsContainer.setOrientation(LinearLayout.VERTICAL);
        mMsgsContainer.setLayoutParams(lp);
        mScrollView.addView(mMsgsContainer);

        mMessageAddedListener = new MessageAddedListener();
        ChatManager.getInstance().addMessageAddedListener(mMessageAddedListener);

        refreshMessages();
    }

    private void refreshMessages() {
        mMsgsContainer.removeAllViews();

        ChatMessage[] msgs = ChatManager.getInstance().getLastMessages(10);
        for(int i = msgs.length - 1; i >= 0; i--) {
            ChatMessage msg = msgs[i];
            if (msg == null) {
                continue;
            }

            appendMessage(msg);
        }
    }

    private void appendMessage(ChatMessage msg) {
        TextView tv = new TextView(mContext);
        tv.setText(msg.getMessage());
        mMsgsContainer.addView(tv);

        // need to wait for it to settle before we scroll again
        mScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 1);
    }

    class MessageAddedListener implements ChatManager.MessageAddedListener {
        @Override
        public void onMessageAdded(final ChatMessage msg) {
            // needs to posted on the UI thread, which this is probably not...
            MiniChatView.this.post(new Runnable() {
                @Override
                public void run() {
                    appendMessage(msg);
                }
            });
        }
    }
}
