package au.com.codeka.warworlds.client.game.chat

import android.os.Handler
import android.view.ViewGroup
import au.com.codeka.warworlds.client.game.world.ChatManager
import au.com.codeka.warworlds.client.ui.Screen
import au.com.codeka.warworlds.client.ui.ScreenContext
import au.com.codeka.warworlds.client.ui.ShowInfo
import au.com.codeka.warworlds.client.ui.ShowInfo.Companion.builder
import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatRoom

/** Main fragment for showing the chat system.  */
class ChatScreen : Screen() {
  // private ChatPagerAdapter chatPagerAdapter;
  private lateinit var layout: ChatLayout
  private var rooms: List<ChatRoom>? = null
  private val handler: Handler? = null

  override fun onCreate(context: ScreenContext, container: ViewGroup) {
    super.onCreate(context, container)
    layout = ChatLayout(context.activity, layoutCallbacks)
  }

  override fun onShow(): ShowInfo? {
    refreshRooms()
    return builder().view(layout).build()
    /*
    Bundle extras = getIntent().getExtras();
    if (extras != null) {
      final int conversationID = extras.getInt("au.com.codeka.warworlds.ConversationID");
      if (conversationID != 0) {
        int position = 0;
        for (; position < mConversations.size(); position++) {
          if (mConversations.get(position).getID() == conversationID) {
            break;
          }
        }
        if (position < mConversations.size()) {
          final int finalPosition = position;
          mHandler.post(new Runnable() {
            @Override
            public void run() {
              mViewPager.setCurrentItem(finalPosition);
            }
          });
        }
      }

      final String empireKey = extras.getString("au.com.codeka.warworlds.NewConversationEmpireKey");
      if (empireKey != null) {
        mHandler.post(new Runnable() {
          @Override
          public void run() {
            ChatManager.i.startConversation(empireKey);
          }
        });
      }
    }*/
  }

  fun moveToFirstUnreadConversation() {
//    for (int i = 0; i < mConversations.size(); i++) {
//      if (mConversations.get(i).getUnreadCount() > 0) {
//        mViewPager.setCurrentItem(i);
//        break;
//      }
//    }
  }

  private val eventHandler: Any = object : Any() { //    @EventHandler
    //    public void onConversationsRefreshed(ChatManager.ConversationsUpdatedEvent event) {
    //      refreshConversations();
    //    }
    //    @EventHandler
    //    public void onConversationsRefreshed(ChatManager.ConversationStartedEvent event) {
    //      refreshConversations();
    //      int index = mConversations.indexOf(event.conversation);
    //      if (index >= 0) {
    //        mViewPager.setCurrentItem(index);
    //     }
    //   }
  }

  private fun refreshRooms() {
    rooms = ChatManager.i.rooms
    layout.refresh(rooms)
  }

  private fun showConfirmAutoTranslateDialog() {
//    Util.getSharedPreferences().edit()
//        .putBoolean("au.com.codeka.warworlds.ChatAskedAboutTranslation", true)
//        .commit();

//    new StyledDialog.Builder(this)
//        .setMessage("Do you want to enable auto-translation of chat message? If you enable this setting, then any chat messages that are not in English will be automatically translated to English for you.\r\n\r\nYou can adjust this setting later from the Options screen.")
//        .setTitle("Auto-translation")
    //       .setPositiveButton("Enable", true, new DialogInterface.OnClickListener() {
//          @Override
//          public void onClick(DialogInterface dialog, int which) {
//            new GlobalOptions().autoTranslateChatMessages(true);
//          }
//        })
//        .setNegativeButton("Don't Enable", null)
//        .create().show();
  }

  private val layoutCallbacks = object : ChatLayout.Callbacks {
    override fun onSend(msg: String?) {
      val room = rooms!![0]
      val chatMessageBuilder = ChatMessage.Builder()
          .message(msg)
          .room_id(room.id)

      // if this is our first chat after the update ...
//    if (!Util.getSharedPreferences().getBoolean("au.com.codeka.warworlds.ChatAskedAboutTranslation", false)) {
//      // ... and this message is all in English ...
//      if (isEnglish(message)) {
//        // ... and they haven't already set the 'auto-translate' setting ...
//        if (!new GlobalOptions().autoTranslateChatMessages()) {
//          // ... then ask whether they want to enable auto-translate
//          showConfirmAutoTranslateDialog();
//        }
//      }
//    }
      ChatManager.i.sendMessage(chatMessageBuilder.build())
    }
  }

  companion object {
    private fun isEnglish(str: String): Boolean {
      for (i in 0 until str.length) {
        val ch = str[i]
        if (ch.toInt() > 0x80) {
          return false
        }
      }
      return true
    }
  }
}