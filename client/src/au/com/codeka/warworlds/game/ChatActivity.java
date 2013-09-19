package au.com.codeka.warworlds.game;

import java.util.ArrayList;

import org.joda.time.DateTimeZone;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.ChatMessage.Location;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;

public class ChatActivity extends BaseActivity {
    private ChatPagerAdapter mChatPagerAdapter;
    private ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chat);

        mChatPagerAdapter = new ChatPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mChatPagerAdapter);

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

    public class ChatPagerAdapter extends FragmentStatePagerAdapter {
        public ChatPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new ChatFragment();
            Bundle args = new Bundle();
            args.putInt("au.com.codeka.warworlds.ChatLocation", ChatMessage.Location.values()[i].getNumber());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return ChatMessage.Location.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(Location.fromNumber(position)) {
            case PUBLIC_CHANNEL:
                return "Global";
            case ALLIANCE_CHANNEL:
                return "Alliance";
            }
            return "";
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            // TODO?
        }
    }

    public static class ChatFragment extends Fragment
                                     implements ChatManager.MessageAddedListener,
                                                ChatManager.MessageUpdatedListener {
        private ChatMessage.Location mChatLocation;
        private ChatAdapter mChatAdapter;
        private Handler mHandler;
        private boolean mAutoTranslate;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            mChatLocation = ChatMessage.Location.fromNumber(args.getInt("au.com.codeka.warworlds.ChatLocation"));
            mHandler = new Handler();

            mAutoTranslate = new GlobalOptions().autoTranslateChatMessages();
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.chat_page, container, false);

            mChatAdapter = new ChatAdapter();
            final ListView chatOutput = (ListView) v.findViewById(R.id.chat_output);
            chatOutput.setAdapter(mChatAdapter);
           // registerForContextMenu(chatOutput);

            chatOutput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    ChatMessage msg = (ChatMessage) mChatAdapter.getItem(position);

                    ChatMessageDialog dialog = new ChatMessageDialog();
                    Bundle args = new Bundle();
                    args.putByteArray("au.com.codeka.warworlds.ChatMessage", ((ChatMessage) msg).toProtocolBuffer().toByteArray());
                    args.putByteArray("au.com.codeka.warworlds.Empire", ((Empire) msg.getEmpire()).toProtocolBuffer().toByteArray());
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
            mChatAdapter.notifyDataSetChanged();
        }

        @Override
        public void onStart() {
            super.onStart();
            ChatManager.getInstance().addMessageAddedListener(this);
            ChatManager.getInstance().addMessageUpdatedListener(this);
            refreshMessages();
        }

        @Override
        public void onStop() {
            super.onStop();
            ChatManager.getInstance().removeMessageAddedListener(this);
            ChatManager.getInstance().removeMessageUpdatedListener(this);
        }

        private void refreshMessages() {
            ArrayList<ChatMessage> allMessages = new ArrayList<ChatMessage>();
            for (ChatMessage msg : ChatManager.getInstance().getAllMessages()) {
                if (msg.shouldDisplay(mChatLocation)) {
                    allMessages.add(msg);
                }
            }
            mChatAdapter.setMessages(allMessages);
        }

        private void appendMessage(final ChatMessage msg) {
            if (!msg.shouldDisplay(mChatLocation)) {
                return;
            }

            mChatAdapter.appendMessage(msg);
        }

        private class ChatAdapter extends BaseAdapter {
            private ArrayList<ChatMessage> mMessages;

            public ChatAdapter() {
                mMessages = new ArrayList<ChatMessage>();
            }

            public void setMessages(ArrayList<ChatMessage> messages) {
                mMessages = messages;
                notifyDataSetChanged();
            }

            public void appendMessage(ChatMessage msg) {
                mMessages.add(msg);
                notifyDataSetChanged();
            }

            @Override
            public int getCount() {
                return mMessages.size();
            }

            @Override
            public Object getItem(int position) {
                return mMessages.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                            (Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.chat_row, null);
                }

                ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
                TextView empireName = (TextView) view.findViewById(R.id.empire_name);
                TextView msgTime = (TextView) view.findViewById(R.id.msg_time);
                TextView message = (TextView) view.findViewById(R.id.message);

                ChatMessage msg = mMessages.get(position);
                if (msg.getEmpire() != null) {
                    Bitmap shield = EmpireShieldManager.i.getShield(getActivity(), (Empire) msg.getEmpire());
                    empireName.setText(msg.getEmpire().getDisplayName());
                    empireIcon.setImageBitmap(shield);
                } else {
                    empireIcon.setImageBitmap(null);
                    empireName.setText("");
                }

                msgTime.setText(msg.getDatePosted().withZone(DateTimeZone.getDefault()).toString("h:mm a"));
                message.setText(msg.format(mChatLocation, true, mAutoTranslate));
                message.setMovementMethod(LinkMovementMethod.getInstance());

                return view;
            }
            
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

        ChatMessage.Location location = ChatMessage.Location.fromNumber(mViewPager.getCurrentItem());
        if (location == ChatMessage.Location.ALLIANCE_CHANNEL) {
            msg.setAllianceChat(true);
        }

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

        ChatManager.getInstance().postMessage(msg);
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
