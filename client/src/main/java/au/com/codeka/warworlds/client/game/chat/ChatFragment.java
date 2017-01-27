package au.com.codeka.warworlds.client.game.chat;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;

/** Main fragment for showing the chat system. */
public class ChatFragment extends BaseFragment {
  private ChatPagerAdapter chatPagerAdapter;
  private ViewPager viewPager;
  private EditText chatMsg;
  private Button sendButton;

  private List<ChatRoom> rooms;
  private Handler handler;

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_chat;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    viewPager = (ViewPager) view.findViewById(R.id.pager);
    chatMsg = (EditText) view.findViewById(R.id.chat_text);
    sendButton = (Button) view.findViewById(R.id.chat_send);

    chatMsg.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_NULL) {
        onSendClick(v);
        return true;
      }
      return false;
    });

    sendButton.setOnClickListener(this::onSendClick);
    chatPagerAdapter = new ChatPagerAdapter(getFragmentManager());
    viewPager.setAdapter(chatPagerAdapter);
  }

  @Override
  public void onResume() {
    super.onResume();

    refreshConversations();
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

  @Override
  public void onStart() {
    super.onStart();
  //  ChatManager.eventBus.register(mEventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
//    ChatManager.eventBus.unregister(mEventHandler);
  }

  public void moveToFirstUnreadConversation() {
//    for (int i = 0; i < mConversations.size(); i++) {
//      if (mConversations.get(i).getUnreadCount() > 0) {
//        mViewPager.setCurrentItem(i);
//        break;
//      }
//    }
  }

  private final Object eventHandler = new Object() {
//    @EventHandler
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
  };

  private void refreshConversations() {
/*    mConversations = ChatManager.i.getConversations();
    // remove the recent conversation, we don't display it here
    Iterator<ChatConversation> it = mConversations.iterator();
    while (it.hasNext()) {
      ChatConversation conversation = it.next();
      if (conversation.getID() < 0 &&
          conversation.getID() != ChatManager.ALLIANCE_CONVERSATION_ID) {
        it.remove();
      }
    }
    if (EmpireManager.i.getEmpire().getAlliance() != null && mConversations.size() > 1) {
      // swap alliance and global around...
      ChatConversation globalConversation = mConversations.get(1);
      mConversations.set(1, mConversations.get(0));
      mConversations.set(0, globalConversation);
    }

    mChatPagerAdapter.refresh(mConversations);
*/
  }

  public class ChatPagerAdapter extends FragmentStatePagerAdapter {
    private final List<ChatRoom> rooms = new ArrayList<>();

    ChatPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    public void refresh(List<ChatRoom> rooms) {
      this.rooms.clear();
      this.rooms.addAll(rooms);
      notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int index) {
//      Fragment fragment = new ChatFragment();
//      Bundle args = new Bundle();
//      args.putInt("au.com.codeka.warworlds.ConversationID", rooms.get(index).id);
//      fragment.setArguments(args);
//      return fragment;
      return null;
    }

    @Override
    public int getItemPosition(Object item) {
      return POSITION_NONE;
    }

    @Override
    public int getCount() {
      return rooms.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      ChatRoom room = rooms.get(position);
      return String.format(Locale.US, "Chat #%d", room.id);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);

      //ChatRoom room = rooms.get(position);
      //conversation.markAllRead();
    }
  }

  private void onSendClick(View view) {
    if (chatMsg.getText().toString().isEmpty()) {
      return;
    }

    ChatRoom room = rooms.get(viewPager.getCurrentItem());

    ChatMessage.Builder chatMessageBuilder = new ChatMessage.Builder()
        .message(chatMsg.getText().toString())
        .room_id(room.id);

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

    chatMsg.setText("");
//    ChatManager.i.postMessage(msg);
  }

  private void showConfirmAutoTranslateDialog() {
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

  private static boolean isEnglish(String str) {
    for (int i = 0; i < str.length(); i++) {
      Character ch = str.charAt(i);
      if (ch > 0x80) {
        return false;
      }
    }
    return true;
  }
}
