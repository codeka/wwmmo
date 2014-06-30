package au.com.codeka.warworlds.game.chat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.EmpireManager;

public class ChatActivity extends BaseActivity
                          implements ChatManager.ConversationsRefreshListener {
    private ChatPagerAdapter mChatPagerAdapter;
    private ViewPager mViewPager;
    private List<ChatConversation> mConversations;
    private Handler mHandler;
    private boolean mFirstRefresh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chat);

        mChatPagerAdapter = new ChatPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mChatPagerAdapter);
        mHandler = new Handler();
        mFirstRefresh = true;

        final EditText chatMsg = (EditText) findViewById(R.id.chat_text);
        chatMsg.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL) {
                    sendCurrentChat();
                    return true;
                }
                return false;
            }
        });

        Button send = (Button) findViewById(R.id.chat_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCurrentChat();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {

                onConversationsRefreshed();

                if (mFirstRefresh) {
                    mFirstRefresh = false;

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
                                    startConversation(empireKey);
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ChatManager.i.addConversationsRefreshListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ChatManager.i.removeConversationsRefreshListener(this);
    }

    public void moveToFirstUnreadConversation() {
        for (int i = 0; i < mConversations.size(); i++) {
            if (mConversations.get(i).getUnreadCount() > 0) {
                mViewPager.setCurrentItem(i);
                break;
            }
        }
    }

    @Override
    public void onConversationsRefreshed() {
        mConversations = ChatManager.i.getConversations();
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
    }

    /** Start a new conversation with the given empire (empireKey could be null for an empty chat) */
    public void startConversation(String empireKey) {
        ChatManager.i.startConversation(empireKey, new ChatManager.ConversationStartedListener() {
            @Override
            public void onConversationStarted(ChatConversation conversation) {
                onConversationsRefreshed();

                int index = mConversations.indexOf(conversation);
                if (index >= 0) {
                    mViewPager.setCurrentItem(index);
                }
            }
        });
    }

    public class ChatPagerAdapter extends FragmentStatePagerAdapter {
        List<ChatConversation> mConversations;

        public ChatPagerAdapter(FragmentManager fm) {
            super(fm);
            mConversations = new ArrayList<ChatConversation>();
        }

        public void refresh(List<ChatConversation> conversations) {
            mConversations = conversations;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new ChatFragment();
            Bundle args = new Bundle();
            args.putInt("au.com.codeka.warworlds.ConversationID", mConversations.get(i).getID());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemPosition(Object item) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return mConversations.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            ChatConversation conversation = mConversations.get(position);
            return String.format("Chat #"+conversation.getID());
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);

            ChatConversation conversation = mConversations.get(position);
            conversation.markAllRead();
        }
    }

    private void sendCurrentChat() {
        EditText chatMsg = (EditText) findViewById(R.id.chat_text);
        if (chatMsg.getText().toString().equals("")) {
            return;
        }

        String message = chatMsg.getText().toString();

        ChatMessage msg = new ChatMessage();
        msg.setMessage(message);
        msg.setEmpireID(EmpireManager.i.getEmpire().getID());

        ChatConversation conversation = mConversations.get(mViewPager.getCurrentItem());
        msg.setConversation(conversation);

        // if this is our first chat after the update ...
        if (!Util.getSharedPreferences().getBoolean("au.com.codeka.warworlds.ChatAskedAboutTranslation", false)) {
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
            .commit();

        new StyledDialog.Builder(this)
            .setMessage("Do you want to enable auto-translation of chat message? If you enable this setting, then any chat messages that are not in English will be automatically translated to English for you.\r\n\r\nYou can adjust this setting later from the Options screen.")
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
            Character ch = str.charAt(i);
            if (ch > 0x80) {
                return false;
            }
        }
        return true;
    }
}
