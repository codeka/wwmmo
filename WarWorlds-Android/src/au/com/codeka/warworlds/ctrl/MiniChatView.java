package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.graphics.Color;
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

    private LinearLayout mMsgsContainer;

    public MiniChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        this.setBackgroundColor(Color.argb(0xaa, 0, 0, 0));

        ScrollView sv = new ScrollView(mContext);
        this.addView(sv);

        mMsgsContainer = new LinearLayout(mContext);
        mMsgsContainer.setOrientation(LinearLayout.VERTICAL);
        sv.addView(mMsgsContainer);

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

            TextView tv = new TextView(mContext);
            tv.setText(msg.getMessage());
            mMsgsContainer.addView(tv);
        }
    }
}
