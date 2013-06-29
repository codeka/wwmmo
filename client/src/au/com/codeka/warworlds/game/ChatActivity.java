package au.com.codeka.warworlds.game;

import java.util.List;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.Util;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.ChatMessage.Location;

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
        private ScrollView mScrollView;
        private LinearLayout mChatOutput;
        private Handler mHandler;
        private boolean mScrollPosted;
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.chat_page, container, false);

            mScrollView = (ScrollView) v;
            mChatOutput = (LinearLayout) v.findViewById(R.id.chat_output);

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
            for (int i = 0; i < mChatOutput.getChildCount(); i++) {
                TextView tv = (TextView) mChatOutput.getChildAt(i);
                ChatMessage other = (ChatMessage) tv.getTag();
                if (other == null || other.getDatePosted() == null) {
                    continue;
                }

                if (other.getEmpireKey() == null | msg.getEmpireKey() == null) {
                    continue;
                }

                if (other.getDatePosted().equals(msg.getDatePosted()) &&
                    other.getEmpireKey().equals(msg.getEmpireKey())) {
                    tv.setText(msg.format(mChatLocation, mAutoTranslate));
                }
            }
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
            mChatOutput.removeAllViews();

            List<ChatMessage> msgs = ChatManager.getInstance().getAllMessages();
            for(ChatMessage msg : msgs) {
                appendMessage(msg);
            }
        }

        private void appendMessage(final ChatMessage msg) {
            if (!msg.shouldDisplay(mChatLocation)) {
                return;
            }

            TextView tv = new TextView(getActivity());
            tv.setText(msg.format(mChatLocation, mAutoTranslate));
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setTag(msg);
            mChatOutput.addView(tv);

            // need to wait for it to settle before we scroll again
            if (!mScrollPosted) {
                mScrollPosted = true;
                mScrollView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScrollPosted = false;
                        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 15);
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
