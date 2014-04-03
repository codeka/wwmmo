package au.com.codeka.warworlds.ctrl;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.game.chat.ChatActivity;
import au.com.codeka.warworlds.model.ChatConversation;
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

        this.setBackgroundColor(Color.argb(0xaa, 0, 0, 0));

        View view = inflate(context, R.layout.chat_mini_ctrl, null);
        addView(view);

        mScrollView = (ScrollView) view.findViewById(R.id.scrollview);
        mMsgsContainer = (LinearLayout) view.findViewById(R.id.msgs_container);
        mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();
        setupUnreadMessageCount(view);

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
        if (this.isInEditMode()) {
            return;
        }

        ChatManager.i.addMessageAddedListener(this);
        ChatManager.i.addMessageUpdatedListener(this);

        refreshMessages();
    }

    @Override
    public void onDetachedFromWindow() {
        if (this.isInEditMode()) {
            return;
        }

        ChatManager.i.removeMessageAddedListener(this);
        ChatManager.i.removeMessageUpdatedListener(this);
    }

    private void refreshMessages() {
        mMsgsContainer.removeAllViews();
        ChatConversation conversation = ChatManager.i.getConversationByID(ChatManager.RECENT_CONVERSATION_ID);
        for(ChatMessage msg : conversation.getAllMessages()) {
            appendMessage(msg);
        }
    }

    private void appendMessage(final ChatMessage msg) {
        TextView tv = new TextView(mContext);
        tv.setText(Html.fromHtml(msg.format(true, false, mAutoTranslate)));
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
                tv.setText(Html.fromHtml(msg.format(true, false, mAutoTranslate)));
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

    private static int getTotalUnreadCount() {
        int numUnread = 0;
        for (ChatConversation conversation : ChatManager.i.getConversations()) {
            numUnread += conversation.getUnreadCount();
        }
        return numUnread;
    }

    private static void refreshUnreadCountButton(Button btn) {
        int numUnread = getTotalUnreadCount();

        if (numUnread > 0) {
            btn.setVisibility(View.VISIBLE);
            btn.setText(String.format("  %d  ", numUnread));
        } else {
            btn.setVisibility(View.GONE);
        }
    }

    private void setupUnreadMessageCount(View v) {
        final Button btn = (Button) v.findViewById(R.id.unread_btn);
        refreshUnreadCountButton(btn);

        ChatManager.i.addMessageAddedListener(new ChatManager.MessageAddedListener() {
            @Override
            public void onMessageAdded(ChatMessage msg) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        refreshUnreadCountButton(btn);
                    }
                });
            }
        });
        ChatManager.i.addUnreadMessageCountListener(new ChatManager.UnreadMessageCountListener() {
            @Override
            public void onUnreadMessageCountChanged() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        refreshUnreadCountButton(btn);
                    }
                });
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // move to the first conversation with an unread message
                Intent intent = new Intent(mContext, ChatActivity.class);

                List<ChatConversation> conversations = ChatManager.i.getConversations();
                for (int i = 0; i < conversations.size(); i++) {
                    if (conversations.get(i).getUnreadCount() > 0) {
                        intent.putExtra("au.com.codeka.warworlds.ConversationID", conversations.get(i).getID());
                        break;
                    }
                }


                mContext.startActivity(intent);
            }
        });
    }
}

