package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.Empire;

/**
 * A view that contains the last couple of chat messages, and expands to the full
 * {@link ChatFragment} when you tap it.
 */
public class ChatMiniView extends RelativeLayout {
  private static final int MAX_ROWS = 10;

  private final ScrollView scrollView;
  private final LinearLayout msgsContainer;
  private final Button unreadMsgCount;

  private boolean autoTranslate;

  public ChatMiniView(Context context, AttributeSet attrs) {
    super(context, attrs);

    inflate(context, R.layout.ctrl_chat_mini_view, this);

    scrollView = (ScrollView) findViewById(R.id.scrollview);
    msgsContainer = (LinearLayout) findViewById(R.id.msgs_container);
    unreadMsgCount = (Button) findViewById(R.id.unread_btn);

    if (this.isInEditMode()) {
      return;
    }

    this.setBackgroundColor(Color.argb(0xaa, 0, 0, 0));
    //mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();
    refreshUnreadCountButton();

    msgsContainer.setOnClickListener(v -> {
//      Intent intent = new Intent(mContext, ChatActivity.class);
//      mContext.startActivity(intent);
    });
    msgsContainer.setClickable(true);

    unreadMsgCount.setOnClickListener(v -> {
      // move to the first conversation with an unread message
//      Intent intent = new Intent(mContext, ChatActivity.class);

//      List<ChatConversation> conversations = ChatManager.i.getConversations();
//      for (int i = 0; i < conversations.size(); i++) {
//        if (conversations.get(i).getUnreadCount() > 0) {
//          intent.putExtra("au.com.codeka.warworlds.ConversationID",
//              conversations.get(i).getID());
 //         break;
 //       }
 //     }

 //     mContext.startActivity(intent);
    });

  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (this.isInEditMode()) {
      return;
    }

    App.i.getEventBus().register(eventHandler);
    refreshMessages();
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (this.isInEditMode()) {
      return;
    }

    App.i.getEventBus().unregister(eventHandler);
  }

  private void refreshMessages() {
    msgsContainer.removeAllViews();
//    for(ChatMessage msg : ChatManager.i.getRecentMessages().getMessages()) {
//      appendMessage(msg);
//    }
  }
/*
  private void appendMessage(final ChatMessage msg) {
    TextView tv = new TextView(mContext);
    tv.setText(Html.fromHtml(msg.format(true, false, mAutoTranslate)));
    tv.setTag(msg);

    while (mMsgsContainer.getChildCount() >= MAX_ROWS) {
      mMsgsContainer.removeViewAt(0);
    }
    mMsgsContainer.addView(tv);

    scrollToBottom();
  }
*/
  private void scrollToBottom() {
    scrollView.postDelayed(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN), 1);
  }

  private static int getTotalUnreadCount() {
    int numUnread = 0;
    //for (ChatConversation conversation : ChatManager.i.getConversations()) {
    //  numUnread += conversation.getUnreadCount();
    //}
    return numUnread;
  }

  private void refreshUnreadCountButton() {
    int numUnread = getTotalUnreadCount();

    if (numUnread > 0) {
      unreadMsgCount.setVisibility(View.VISIBLE);
      unreadMsgCount.setText(String.format(Locale.US, "  %d  ", numUnread));
    } else {
      unreadMsgCount.setVisibility(View.GONE);
    }
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      for (int i = 0; i < msgsContainer.getChildCount(); i++) {
        TextView tv = (TextView) msgsContainer.getChildAt(i);
//        ChatMessage msg = (ChatMessage) tv.getTag();
//        if (msg != null && msg.getEmpireID() == empire.getID()) {
//          tv.setText(Html.fromHtml(msg.format(true, false, mAutoTranslate)));
//        }
      }
      scrollToBottom();
    }

//    @EventHandler
//    public void onMessageAdded(ChatManager.MessageAddedEvent event) {
//      appendMessage(event.msg);
//      refreshUnreadCountButton();
//    }

//    @EventHandler
//    public void onUnreadMessageCountUpdated(ChatManager.UnreadMessageCountUpdatedEvent event) {
//      refreshUnreadCountButton();
//    }
  };
}
