package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.view.View
import android.widget.*
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.chat.ChatHelper
import au.com.codeka.warworlds.client.game.world.ChatManager
import au.com.codeka.warworlds.client.game.world.ChatMessagesUpdatedEvent
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.Empire
import com.google.common.base.Preconditions
import java.util.*

/**
 * A view that contains the last couple of chat messages, and expands to the full [ChatScreen] when
 * you tap it.
 */
class ChatMiniView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
  interface Callback {
    fun showChat(conversationId: Long?)
  }

  private val scrollView: ScrollView
  private val msgsContainer: LinearLayout
  private val unreadMsgCount: Button
  private lateinit var callback: Callback
  private val autoTranslate = false

  fun setCallback(callback: Callback) {
    this.callback = Preconditions.checkNotNull(callback)
  }

  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (this.isInEditMode) {
      return
    }

    App.eventBus.register(eventHandler)
    refreshMessages()
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if (this.isInEditMode) {
      return
    }
    App.eventBus.unregister(eventHandler)
  }

  private fun refreshMessages() {
    msgsContainer.removeAllViews()
    for (msg in ChatManager.i.getMessages(System.currentTimeMillis(), MAX_ROWS)) {
      appendMessage(msg)
    }
  }

  private fun appendMessage(msg: ChatMessage) {
    val tv = TextView(context)
    tv.text = fromHtml(ChatHelper.format(msg, isPublic = true, messageOnly = false, autoTranslate))
    tv.tag = msg
    while (msgsContainer.childCount >= MAX_ROWS) {
      msgsContainer.removeViewAt(0)
    }
    msgsContainer.addView(tv)
    scrollToBottom()
  }

  private fun scrollToBottom() {
    scrollView.postDelayed({ scrollView.fullScroll(ScrollView.FOCUS_DOWN) }, 1)
  }

  private fun refreshUnreadCountButton() {
    val numUnread = totalUnreadCount
    if (numUnread > 0) {
      unreadMsgCount.visibility = View.VISIBLE
      unreadMsgCount.text = String.format(Locale.US, "  %d  ", numUnread)
    } else {
      unreadMsgCount.visibility = View.GONE
    }
  }

  companion object {
    private const val MAX_ROWS = 10

    //for (ChatConversation conversation : ChatManager.i.getConversations()) {
    //  numUnread += conversation.getUnreadCount();
    //}
    private val totalUnreadCount: Int
      get() =//for (ChatConversation conversation : ChatManager.i.getConversations()) {
      //  numUnread += conversation.getUnreadCount();
          //}
        0
  }

  init {
    View.inflate(context, R.layout.ctrl_chat_mini_view, this)
    scrollView = findViewById(R.id.scrollview)
    msgsContainer = findViewById(R.id.msgs_container)
    unreadMsgCount = findViewById(R.id.unread_btn)
    if (!isInEditMode) {
      setBackgroundColor(Color.argb(0xaa, 0, 0, 0))
      //mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();
      refreshUnreadCountButton()
      msgsContainer.setOnClickListener {
        callback.showChat(null /* conversationId */)
      }
      msgsContainer.isClickable = true
      unreadMsgCount.setOnClickListener {
        // move to the first conversation with an unread message
//      List<ChatConversation> conversations = ChatManager.i.getConversations();
//      for (int i = 0; i < conversations.size(); i++) {
//        if (conversations.get(i).getUnreadCount() > 0) {
//          intent.putExtra("au.com.codeka.warworlds.ConversationID",
//              conversations.get(i).getID());
        //         break;
        //       }
        //     }
        callback.showChat(null /* TODO: conversationId */)
      }
    }
  }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onEmpireUpdated(empire: Empire) {
      for (i in 0 until msgsContainer.childCount) {
        val tv = msgsContainer.getChildAt(i) as TextView
        val msg = tv.tag as ChatMessage
        if (msg.empire_id != null && msg.empire_id == empire.id) {
          tv.text =
              fromHtml(ChatHelper.format(msg, isPublic = true, messageOnly = false, autoTranslate))
        }
      }
      scrollToBottom()
    }

    @EventHandler
    fun onMessagesUpdatedEvent(@Suppress("unused_parameter") event: ChatMessagesUpdatedEvent?) {
      refreshMessages()
    }
  }
}