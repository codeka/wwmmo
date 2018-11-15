package au.com.codeka.warworlds.client.ctrl;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.game.chat.ChatHelper;
import au.com.codeka.warworlds.client.game.chat.ChatScreen;
import au.com.codeka.warworlds.client.game.world.ChatManager;
import au.com.codeka.warworlds.client.game.world.ChatMessagesUpdatedEvent;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.Empire;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A view that contains the last couple of chat messages, and expands to the full
 * {@link ChatScreen} when you tap it.
 */
public class ChatMiniView extends RelativeLayout {
  public interface Callback {
    void showChat(@Nullable Long conversationId);
  }

  private static final int MAX_ROWS = 10;

  private final ScrollView scrollView;
  private final LinearLayout msgsContainer;
  private final Button unreadMsgCount;

  private Callback callback;
  private boolean autoTranslate;

  public ChatMiniView(Context context, AttributeSet attrs) {
    super(context, attrs);

    inflate(context, R.layout.ctrl_chat_mini_view, this);

    scrollView = findViewById(R.id.scrollview);
    msgsContainer = findViewById(R.id.msgs_container);
    unreadMsgCount = findViewById(R.id.unread_btn);

    if (this.isInEditMode()) {
      return;
    }

    this.setBackgroundColor(Color.argb(0xaa, 0, 0, 0));
    //mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();
    refreshUnreadCountButton();

    msgsContainer.setOnClickListener(v -> {
      checkNotNull(callback);
      callback.showChat(null /* conversationId */);
    });
    msgsContainer.setClickable(true);

    unreadMsgCount.setOnClickListener(v -> {
      checkNotNull(callback);

      // move to the first conversation with an unread message
//      List<ChatConversation> conversations = ChatManager.i.getConversations();
//      for (int i = 0; i < conversations.size(); i++) {
//        if (conversations.get(i).getUnreadCount() > 0) {
//          intent.putExtra("au.com.codeka.warworlds.ConversationID",
//              conversations.get(i).getID());
 //         break;
 //       }
 //     }

      callback.showChat(null /* TODO: conversationId */);
    });
  }

  public void setCallback(Callback callback) {
    this.callback = checkNotNull(callback);
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
    for(ChatMessage msg : ChatManager.i.getMessages(System.currentTimeMillis(), MAX_ROWS)) {
      appendMessage(msg);
    }
  }

  private void appendMessage(final ChatMessage msg) {
    TextView tv = new TextView(getContext());
    tv.setText(Html.fromHtml(ChatHelper.format(msg, true, false, autoTranslate)));
    tv.setTag(msg);

    while (msgsContainer.getChildCount() >= MAX_ROWS) {
      msgsContainer.removeViewAt(0);
    }
    msgsContainer.addView(tv);

    scrollToBottom();
  }

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
        ChatMessage msg = (ChatMessage) tv.getTag();
        if (msg != null && msg.empire_id != null && msg.empire_id.equals(empire.id)) {
          tv.setText(Html.fromHtml(ChatHelper.format(msg, true, false, autoTranslate)));
        }
      }
      scrollToBottom();
    }

    @EventHandler
    public void onMessagesUpdatedEvent(ChatMessagesUpdatedEvent event) {
      refreshMessages();
    }
  };
}
