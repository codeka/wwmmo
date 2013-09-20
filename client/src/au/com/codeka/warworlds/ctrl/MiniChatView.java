package au.com.codeka.warworlds.ctrl;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.game.ChatActivity;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;

/**
 * This control displays the mini chat window, which displays recent chat
 * messages on each screen.
 */
public class MiniChatView extends RelativeLayout
                          implements ChatManager.MessageAddedListener,
                                     ChatManager.MessageUpdatedListener {
    private Context mContext;

    private ScrollView mScrollView;
    private LinearLayout mMsgsContainer;
    private boolean mAutoTranslate;

    private static final int MAX_ROWS = 10;

    public MiniChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        if (this.isInEditMode()) {
            return;
        }

        int id = 1;
        this.setBackgroundColor(Color.argb(0xaa, 0, 0, 0));

        Resources r = getResources();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, r.getDisplayMetrics());
        LayoutParams lp = new LayoutParams(size, size);

        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mScrollView = new ScrollView(mContext);
        mScrollView.setLayoutParams(lp);
        mScrollView.setId(id++);
        mScrollView.setFillViewport(true);
        addView(mScrollView);

        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mMsgsContainer = new LinearLayout(mContext);
        mMsgsContainer.setOrientation(LinearLayout.VERTICAL);
        mMsgsContainer.setLayoutParams(lp);
        mMsgsContainer.setId(id++);
        mScrollView.addView(mMsgsContainer);

        mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();

        refreshMessages();

        mMsgsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ChatActivity.class);
                mContext.startActivity(intent);
            }
        });
        mMsgsContainer.setClickable(true);;
    }

    @Override
    public void onAttachedToWindow() {
        ChatManager.i.addMessageAddedListener(this);
        ChatManager.i.addMessageUpdatedListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        ChatManager.i.removeMessageAddedListener(this);
        ChatManager.i.removeMessageUpdatedListener(this);
    }

    private void refreshMessages() {
        mMsgsContainer.removeAllViews();
/*TODO: no way to get last 10 messages across all conversations...
        List<ChatMessage> msgs = ChatManager.i.getLastMessages(10);
        for(ChatMessage msg : msgs) {
            appendMessage(msg);
        }
*/
    }

    private void appendMessage(final ChatMessage msg) {
        TextView tv = new TextView(mContext);
        tv.setText(msg.format(true, false, mAutoTranslate));
        tv.setTag(msg);

        while (mMsgsContainer.getChildCount() >= MAX_ROWS) {
            mMsgsContainer.removeViewAt(0);
        }
        mMsgsContainer.addView(tv);

        // need to wait for it to settle before we scroll again
        mScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 1);
    }

    private void updateMessage(final ChatMessage msg) {
        for (int i = 0; i < mMsgsContainer.getChildCount(); i++) {
            TextView tv = (TextView) mMsgsContainer.getChildAt(i);
            ChatMessage other = (ChatMessage) tv.getTag();
            if (other == null || other.getDatePosted() == null) {
                continue;
            }

            if (other.getEmpireKey() == null | msg.getEmpireKey() == null) {
                continue;
            }

            if (other.getDatePosted().equals(msg.getDatePosted()) &&
                other.getEmpireKey().equals(msg.getEmpireKey())) {
                tv.setText(msg.format(true, false, mAutoTranslate));
            }
        }
    }

    @Override
    public void onMessageAdded(final ChatMessage msg) {
        post(new Runnable() {
            @Override
            public void run() {
                appendMessage(msg);
            }
        });
    }

    @Override
    public void onMessageUpdated(final ChatMessage msg) {
        post(new Runnable() {
            @Override
            public void run() {
                updateMessage(msg);
            }
        });
    }
}
