package au.com.codeka.warworlds.game.chat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import javax.annotation.Nonnull;

import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class ChatActivity extends BaseActivity {
  private ChatPagerAdapter chatPagerAdapter;
  private ViewPager viewPager;
  private List<ChatConversation> conversations;
  private Handler handler;
  private boolean firstRefresh;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.chat);

    chatPagerAdapter = new ChatPagerAdapter(getSupportFragmentManager());
    viewPager = findViewById(R.id.pager);
    viewPager.setAdapter(chatPagerAdapter);
    handler = new Handler();
    firstRefresh = true;

    final EditText chatMsg = findViewById(R.id.chat_text);
    chatMsg.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_NULL) {
        sendCurrentChat();
        return true;
      }
      return false;
    });

    Button send = findViewById(R.id.chat_send);
    send.setOnClickListener(v -> sendCurrentChat());
  }

  @Override
  public void onResumeFragments() {
    super.onResumeFragments();

    ServerGreeter.waitForHello(this, (success, greeting) -> {
      refreshConversations();

      if (firstRefresh) {
        firstRefresh = false;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
          final int conversationID = extras.getInt("au.com.codeka.warworlds.ConversationID");
          if (conversationID != 0) {
            int position = 0;
            for (; position < conversations.size(); position++) {
              if (conversations.get(position).getID() == conversationID) {
                break;
              }
            }
            if (position < conversations.size()) {
              final int finalPosition = position;
              handler.post(() -> viewPager.setCurrentItem(finalPosition));
            }
          }

          final String empireKey = extras.getString("au.com.codeka.warworlds.NewConversationEmpireKey");
          if (empireKey != null) {
            handler.post(() -> ChatManager.i.startConversation(empireKey));
          }
        }
      }

      // Anonymous users can't chat, so disable the controls for sending messages.
      final EditText chatMsg = findViewById(R.id.chat_text);
      final Button send = findViewById(R.id.chat_send);
      chatMsg.setEnabled(!Util.isAnonymous());
      send.setEnabled(!Util.isAnonymous());
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    ChatManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    ChatManager.eventBus.unregister(eventHandler);
  }

  public void moveToFirstUnreadConversation() {
    for (int i = 0; i < conversations.size(); i++) {
      if (conversations.get(i).getUnreadCount() > 0) {
        viewPager.setCurrentItem(i);
        break;
      }
    }
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onConversationsRefreshed(ChatManager.ConversationsUpdatedEvent event) {
      refreshConversations();
    }

    @EventHandler
    public void onConversationsRefreshed(ChatManager.ConversationStartedEvent event) {
      refreshConversations();

      int index = conversations.indexOf(event.conversation);
      if (index >= 0) {
        viewPager.setCurrentItem(index);
      }
    }
  };

  private void refreshConversations() {
    conversations = ChatManager.i.getConversations();
    // remove the recent conversation, we don't display it here
    Iterator<ChatConversation> it = conversations.iterator();
    while (it.hasNext()) {
      ChatConversation conversation = it.next();
      if (conversation.getID() < 0 &&
          conversation.getID() != ChatManager.ALLIANCE_CONVERSATION_ID) {
        it.remove();
      }
    }
    if (EmpireManager.i.getEmpire().getAlliance() != null && conversations.size() > 1) {
      // swap alliance and global around...
      ChatConversation globalConversation = conversations.get(1);
      conversations.set(1, conversations.get(0));
      conversations.set(0, globalConversation);
    }

    chatPagerAdapter.refresh(conversations);
  }

  public class ChatPagerAdapter extends FragmentStatePagerAdapter {
    List<ChatConversation> conversations;

    ChatPagerAdapter(FragmentManager fm) {
      super(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
      conversations = new ArrayList<>();
    }

    public void refresh(List<ChatConversation> conversations) {
      this.conversations = conversations;
      notifyDataSetChanged();
    }

    @Override
    @Nonnull
    public Fragment getItem(int i) {
      Fragment fragment = new ChatFragment();
      Bundle args = new Bundle();
      args.putInt("au.com.codeka.warworlds.ConversationID", conversations.get(i).getID());
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public int getItemPosition(@Nonnull Object item) {
      return POSITION_NONE;
    }

    @Override
    public int getCount() {
      return conversations.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      ChatConversation conversation = conversations.get(position);
      return String.format(Locale.ENGLISH, "Chat #%d", conversation.getID());
    }

    @Override
    public void setPrimaryItem(@Nonnull ViewGroup container, int position, @Nonnull Object object) {
      super.setPrimaryItem(container, position, object);

      ChatConversation conversation = conversations.get(position);
      conversation.markAllRead();
    }
  }

  private void sendCurrentChat() {
    EditText chatMsg = findViewById(R.id.chat_text);
    if (chatMsg.getText().toString().equals("")) {
      return;
    }

    String message = chatMsg.getText().toString();

    ChatMessage msg = new ChatMessage();
    msg.setMessage(message);
    msg.setEmpireID(EmpireManager.i.getEmpire().getID());

    ChatConversation conversation = conversations.get(viewPager.getCurrentItem());
    msg.setConversation(conversation);

    // if this is our first chat after the update ...
    if (!Util.getSharedPreferences().getBoolean(
        "au.com.codeka.warworlds.ChatAskedAboutTranslation", false)) {
      // ... and this message is all in English ...
      if (isEnglish(message)) {
        // ... and they haven't already set the 'auto-translate' setting ...
        if (!new GlobalOptions().autoTranslateChatMessages()) {
          // ... then ask whether they want to enable auto-translate
          showConfirmAutoTranslateDialog();
        }
      }
    }

    chatMsg.setText("");
    ChatManager.i.postMessage(msg);
  }

  private void showConfirmAutoTranslateDialog() {
    Util.getSharedPreferences().edit()
        .putBoolean("au.com.codeka.warworlds.ChatAskedAboutTranslation", true)
        .apply();

    new StyledDialog.Builder(this)
        .setMessage("Do you want to enable auto-translation of chat message? If you enable this " +
            "setting, then any chat messages that are not in English will be automatically " +
            "translated to English for you.\r\n\r\nYou can adjust this setting later from the " +
            "Options screen.")
        .setTitle("Auto-translation")
        .setPositiveButton("Enable", true, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            new GlobalOptions().autoTranslateChatMessages(true);
          }
        })
        .setNegativeButton("Don't Enable", null)
        .create().show();
  }

  private static boolean isEnglish(String str) {
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch > 0x80) {
        return false;
      }
    }
    return true;
  }
}
