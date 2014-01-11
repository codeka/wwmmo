package au.com.codeka.warworlds.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;

public class ChatActivity extends BaseActivity
                          implements ChatManager.ConversationsRefreshListener {
    private static Logger log = LoggerFactory.getLogger(ChatActivity.class);
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

    public static class ChatFragment extends Fragment
                                     implements ChatManager.MessageAddedListener,
                                                ChatManager.MessageUpdatedListener {
        private ChatConversation mConversation;
        private ChatAdapter mChatAdapter;
        private Handler mHandler;
        private boolean mAutoTranslate;
        private ListView mChatOutput;
        private boolean mNoMoreChats;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            mConversation = ChatManager.i.getConversationByID(args.getInt("au.com.codeka.warworlds.ConversationID"));
            mHandler = new Handler();

            mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.chat_page, container, false);

            FrameLayout header = (FrameLayout) v.findViewById(R.id.header);
            View headerContent;
            if (mConversation.getID() == 0) {
                headerContent = inflater.inflate(R.layout.chat_header_global, null, false);
                setupGlobalChatHeader(headerContent);
            } else if (mConversation.getID() < 0) {
                headerContent = inflater.inflate(R.layout.chat_header_alliance, null, false);
                setupAllianceChatHeader(headerContent);
            } else {
                headerContent = inflater.inflate(R.layout.chat_header_private, null, false);
                setupPrivateChatHeader(headerContent);
            }
            header.addView(headerContent);

            mChatAdapter = new ChatAdapter();
            mChatOutput = (ListView) v.findViewById(R.id.chat_output);
            mChatOutput.setAdapter(mChatAdapter);

            mChatOutput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    ChatAdapter.ItemEntry entry = (ChatAdapter.ItemEntry) mChatAdapter.getItem(position);
                    if (entry.message == null || entry.message.getEmpire() == null) {
                        // it'll be there in a sec. better to just wait
                        return;
                    }

                    ChatMessageDialog dialog = new ChatMessageDialog();
                    Bundle args = new Bundle();
                    args.putByteArray("au.com.codeka.warworlds.ChatMessage", entry.message.toProtocolBuffer().toByteArray());
                    args.putByteArray("au.com.codeka.warworlds.Empire", ((Empire) entry.message.getEmpire()).toProtocolBuffer().toByteArray());
                    dialog.setArguments(args);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            return v;
        }

        @Override
        public void onMessageAdded(final ChatMessage msg) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    appendMessage(msg);
                }
            });
        }

        @Override
        public void onMessageUpdated(ChatMessage msg) {
            final int finalPosition = mChatOutput.getFirstVisiblePosition();
            mChatAdapter.notifyDataSetChanged();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mChatOutput.setSelection(finalPosition);
                }
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            mConversation.addMessageAddedListener(this);
            mConversation.addMessageUpdatedListener(this);
            refreshMessages();
        }

        @Override
        public void onStop() {
            super.onStop();
            mConversation.removeMessageAddedListener(this);
            mConversation.removeMessageUpdatedListener(this);
        }

        private void refreshMessages() {
            ArrayList<ChatMessage> allMessages = new ArrayList<ChatMessage>(mConversation.getAllMessages());
            mChatAdapter.setMessages(allMessages);
        }

        private void fetchChatItems() {
            mConversation.fetchOlderMessages(new ChatManager.MessagesFetchedListener() {
                @Override
                public void onMessagesFetched(List<ChatMessage> msgs) {
                    log.info("msgs.size() = "+msgs.size());
                    if (msgs.size() == 0) {
                        mNoMoreChats = true;
                    }

                    // get the current item at the top
                    refreshMessages();

                    // figure out which position the item we had before was at
                    int position = -1;
                    if (msgs.size() == 0) {
                        position = 0;
                    } else {
                        int lastMsgID = msgs.get(msgs.size() - 1).getID();
                        for (int i = 0; i < mChatAdapter.getCount(); i++) {
                            ChatAdapter.ItemEntry thisEntry = (ChatAdapter.ItemEntry) mChatAdapter.getItem(i);
                            if (thisEntry.message != null && thisEntry.message.getID() == lastMsgID) {
                                position = i;
                                break;
                            }
                        }
                    }

                    if (position >= 0) {
                        final int finalPosition = position;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mChatOutput.setSelection(finalPosition);
                            }
                        });
                    }
                }
            });
        }

        private void appendMessage(final ChatMessage msg) {
            mChatAdapter.appendMessage(msg);
        }

        private class ChatAdapter extends BaseAdapter {
            private ArrayList<ItemEntry> mEntries;

            public ChatAdapter() {
                mEntries = new ArrayList<ItemEntry>();
            }

            public void setMessages(ArrayList<ChatMessage> messages) {
                mEntries.clear();
                if (!mNoMoreChats) {
                    // we always add an empty entry to mark the end of the messages, well,
                    // unless there's no more chats left
                    mEntries.add(new ItemEntry());
                }

                for (ChatMessage msg : messages) {
                    appendMessage(msg);
                }
                notifyDataSetChanged();
            }

            public void appendMessage(ChatMessage msg) {
                boolean needsDateHeader = false;
                if (mEntries.size() == 0) {
                    needsDateHeader = true;
                } else if (mEntries.get(mEntries.size() - 1).message != null) {
                    DateTime lastDate = mEntries.get(mEntries.size() - 1).message.getDatePosted().withZone(DateTimeZone.getDefault());
                    DateTime thisDate = msg.getDatePosted().withZone(DateTimeZone.getDefault());

                    if (lastDate.getYear() != thisDate.getYear() ||
                            lastDate.getDayOfYear() != thisDate.getDayOfYear()) {
                        needsDateHeader = true;
                    }
                }

                if (needsDateHeader) {
                    mEntries.add(new ItemEntry(msg.getDatePosted()));
                }
                mEntries.add(new ItemEntry(msg));
                notifyDataSetChanged();
            }

            @Override
            public int getViewTypeCount() {
                return 3;
            }

            @Override
            public int getItemViewType(int position) {
                ItemEntry entry = mEntries.get(position);
                if (entry.message == null && entry.date == null) {
                    // 2 == "loading"
                    return 2;
                }

                if (entry.date != null) {
                    // 1 == "simple"
                    return 1;
                }
                if (entry.message.getAction() != null && entry.message.getAction() != ChatMessage.MessageAction.Normal) {
                    return 1;
                }
                if (entry.message.getEmpireKey() == null) {
                    return 1;
                }

                // 0 == "normal"
                return 0;
            }

            @Override
            public int getCount() {
                return mEntries.size();
            }

            @Override
            public Object getItem(int position) {
                return mEntries.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ItemEntry entry = mEntries.get(position);
                ChatMessage.MessageAction action = ChatMessage.MessageAction.Normal;
                if (entry.message != null && entry.message.getAction() != null) {
                    action = entry.message.getAction();
                }

                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);

                    if (entry.date == null && entry.message == null) {
                        view = inflater.inflate(R.layout.chat_row_loading, null);
                    } else if (entry.date != null || action != ChatMessage.MessageAction.Normal) {
                        view = inflater.inflate(R.layout.chat_row_simple, null);
                    } else {
                        view = inflater.inflate(R.layout.chat_row, null);
                    }
                }

                if (entry.date == null && entry.message == null) {
                    // this implies we're at the end of the list, fetch the next bunch
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            fetchChatItems();
                        }
                    });
                } else if (entry.date != null) {
                    TextView message = (TextView) view.findViewById(R.id.message);
                    message.setTextColor(Color.LTGRAY);
                    message.setGravity(Gravity.RIGHT);
                    message.setText(entry.date.toString("EE, dd MMM yyyy"));
                } else if (action != ChatMessage.MessageAction.Normal) {
                    TextView message = (TextView) view.findViewById(R.id.message);
                    message.setTextColor(Color.LTGRAY);
                    message.setGravity(Gravity.LEFT);
                    Empire otherEmpire = EmpireManager.i.getEmpire(entry.message.getMessage());
                    if (action == ChatMessage.MessageAction.ParticipantAdded) {
                        if (entry.message.getEmpire() != null && otherEmpire != null) {
                            String content = String.format(Locale.ENGLISH, "%s has added %s to the conversation.",
                                    entry.message.getEmpire().getDisplayName(), otherEmpire.getDisplayName());
                            message.setText(Html.fromHtml("<i>"+content+"</i>"));
                        } else {
                            message.setText(Html.fromHtml("<i>An empire has been added to the conversation."));
                        }
                    } else if (action == ChatMessage.MessageAction.ParticipantLeft) {
                        if (entry.message.getEmpire() != null && otherEmpire != null) {
                            String content = String.format(Locale.ENGLISH, "%s has left the conversation.",
                                    otherEmpire.getDisplayName());
                            message.setText(Html.fromHtml("<i>"+content+"</i>"));
                        } else {
                            message.setText(Html.fromHtml("<i>An empire has left the conversation."));
                        }
                    }
                } else if (entry.message.getEmpireKey() == null) {
                    ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                    TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                    TextView msgTime = (TextView) view.findViewById(R.id.msg_time);
                    TextView message = (TextView) view.findViewById(R.id.message);

                    empireName.setText("");
                    empireIcon.setImageBitmap(null);
                    msgTime.setText(entry.message.getDatePosted().withZone(DateTimeZone.getDefault()).toString("h:mm a"));

                    String html = entry.message.format(true, true, false);
                    message.setText(Html.fromHtml("<font color=\"#00ffff\"><b>[SERVER]</b></font> " + html));

                    if (html.indexOf("<a ") >= 0) { // only if there's actually a link...
                        message.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                } else {
                    ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                    TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                    TextView msgTime = (TextView) view.findViewById(R.id.msg_time);
                    TextView message = (TextView) view.findViewById(R.id.message);
    
                    if (entry.message.getEmpire() != null) {
                        Bitmap shield = EmpireShieldManager.i.getShield(getActivity(),
                                (Empire) entry.message.getEmpire());
                        empireName.setText(entry.message.getEmpire().getDisplayName());
                        empireIcon.setImageBitmap(shield);
                    } else {
                        empireIcon.setImageBitmap(null);
                        empireName.setText("");
                    }

                    msgTime.setText(entry.message.getDatePosted().withZone(DateTimeZone.getDefault()).toString("h:mm a"));
                    String html = entry.message.format(mConversation.getID() == 0, true, mAutoTranslate);
                    message.setText(Html.fromHtml(html));
                    if (html.indexOf("<a ") >= 0) { // only if there's actually a link...
                        message.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                }

                return view;
            }

            private class ItemEntry {
                public ChatMessage message;
                public DateTime date;

                public ItemEntry() {
                }
                public ItemEntry(ChatMessage message) {
                    this.message = message;
                }
                public ItemEntry(DateTime date) {
                    this.date = date;
                }
            }
        }

        private void setupGlobalChatHeader(View v) {
            ImageButton settingsBtn = (ImageButton) v.findViewById(R.id.settings_btn);
            settingsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChatGlobalSettingsDialog dialog = new ChatGlobalSettingsDialog();
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            Button newGroupBtn = (Button) v.findViewById(R.id.new_group_btn);
            newGroupBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ChatActivity) getActivity()).startConversation(null);
                }
            });

            setupUnreadMessageCount(v);
        }

        private void setupAllianceChatHeader(View v) {
            Alliance alliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
            if (alliance == null) {
                return; // should never happen...
            }

            TextView title = (TextView) v.findViewById(R.id.title);
            title.setText(alliance.getName());

            setupUnreadMessageCount(v);
        }

        private void setupPrivateChatHeader(View v) {
            // remove our own ID from the list...
            ArrayList<String> empireKeys = new ArrayList<String>();
            for (BaseChatConversationParticipant participant : mConversation.getParticipants()) {
                String empireKey = Integer.toString(participant.getEmpireID());
                if (!empireKey.equals(EmpireManager.i.getEmpire().getKey())) {
                    empireKeys.add(empireKey);
                }
            }

            final LinearLayout empireIconContainer = (LinearLayout) v.findViewById(R.id.empire_icon_container);
            final TextView empireName = (TextView) v.findViewById(R.id.title);
            final double pixelScale = getActivity().getResources().getDisplayMetrics().density;

            ImageButton settingsBtn = (ImageButton) v.findViewById(R.id.settings_btn);
            settingsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChatPrivateSettingsDialog dialog = new ChatPrivateSettingsDialog();
                    Bundle args = new Bundle();
                    Messages.ChatConversation.Builder chat_conversation_pb = Messages.ChatConversation.newBuilder();
                    mConversation.toProtocolBuffer(chat_conversation_pb);
                    args.putByteArray("au.com.codeka.warworlds.ChatConversation", chat_conversation_pb.build().toByteArray());
                    dialog.setArguments(args);
                    dialog.show(getActivity().getSupportFragmentManager(), "");
                }
            });

            if (empireKeys.size() == 0) {
                empireName.setText("Empty Chat");
            } else {
                EmpireManager.i.fetchEmpires(empireKeys, new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        String currName = empireName.getText().toString();
                        if (currName.length() > 0) {
                            currName += ", ";
                        }
                        currName += empire.getDisplayName();
                        empireName.setText(currName);
    
                        ImageView icon = new ImageView(getActivity());
                        icon.setLayoutParams(new LinearLayout.LayoutParams((int)(32 * pixelScale), (int)(32 * pixelScale)));
                        icon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
                        empireIconContainer.addView(icon);
                    }
                });
            }
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
                    refreshUnreadCountButton(btn);
                }
            });
            ChatManager.i.addUnreadMessageCountListener(new ChatManager.UnreadMessageCountListener() {
                @Override
                public void onUnreadMessageCountChanged() {
                    refreshUnreadCountButton(btn);
                }
            });

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // move to the first conversation with an unread message
                    List<ChatConversation> conversations = ((ChatActivity) getActivity()).mConversations;
                    ViewPager viewPager = ((ChatActivity) getActivity()).mViewPager;
                    for (int i = 0; i < conversations.size(); i++) {
                        if (conversations.get(i).getUnreadCount() > 0) {
                            viewPager.setCurrentItem(i);
                            break;
                        }
                    }
                }
            });
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
        msg.setEmpire(EmpireManager.i.getEmpire());

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
