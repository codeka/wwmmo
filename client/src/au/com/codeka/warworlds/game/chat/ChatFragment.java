package au.com.codeka.warworlds.game.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceShieldManager;
import au.com.codeka.warworlds.model.ChatConversation;
import au.com.codeka.warworlds.model.ChatManager;
import au.com.codeka.warworlds.model.ChatMessage;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.ShieldManager;


public class ChatFragment extends Fragment {
    private ChatConversation mConversation;
    private ChatAdapter mChatAdapter;
    private Handler mHandler;
    private boolean mAutoTranslate;
    private ListView mChatOutput;
    private boolean mNoMoreChats;
    private View mHeaderContent;
    private Button mUnreadCountBtn;

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
        if (mConversation.getID() == 0) {
            mHeaderContent = inflater.inflate(R.layout.chat_header_global, null, false);
            setupGlobalChatHeader(mHeaderContent);
        } else if (mConversation.getID() < 0) {
            mHeaderContent = inflater.inflate(R.layout.chat_header_alliance, null, false);
            setupAllianceChatHeader(mHeaderContent);
        } else {
            mHeaderContent = inflater.inflate(R.layout.chat_header_private, null, false);
            setupPrivateChatHeader(mHeaderContent);
        }
        header.addView(mHeaderContent);

        mChatAdapter = new ChatAdapter();
        mChatOutput = (ListView) v.findViewById(R.id.chat_output);
        mChatOutput.setAdapter(mChatAdapter);

        mUnreadCountBtn = (Button) v.findViewById(R.id.unread_btn);
        if (mUnreadCountBtn != null) {
            refreshUnreadCountButton();
            mUnreadCountBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // move to the first conversation with an unread message
                    ((ChatActivity) getActivity()).moveToFirstUnreadConversation();
                }
            });
        }

        mChatOutput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                ChatAdapter.ItemEntry entry = (ChatAdapter.ItemEntry) mChatAdapter.getItem(position);
                if (entry.message == null) {
                    return;
                }
                Empire empire = EmpireManager.i.getEmpire(entry.message.getEmpireID());
                if (empire == null) {
                    return;
                }

                ChatMessageDialog dialog = new ChatMessageDialog();
                Bundle args = new Bundle();
                args.putByteArray("au.com.codeka.warworlds.ChatMessage", entry.message.toProtocolBuffer().toByteArray());
                args.putByteArray("au.com.codeka.warworlds.Empire", empire.toProtocolBuffer().toByteArray());
                dialog.setArguments(args);
                dialog.show(getActivity().getSupportFragmentManager(), "");
            }
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        ChatManager.eventBus.register(mEventHandler);
        EmpireManager.eventBus.register(mEventHandler);
        ShieldManager.eventBus.register(mEventHandler);
        refreshMessages();
    }

    @Override
    public void onStop() {
        super.onStop();
        ShieldManager.eventBus.unregister(mEventHandler);
        EmpireManager.eventBus.unregister(mEventHandler);
        ChatManager.eventBus.unregister(mEventHandler);
    }

    private void refreshMessages() {
        ArrayList<ChatMessage> allMessages = new ArrayList<ChatMessage>(mConversation.getAllMessages());
        mChatAdapter.setMessages(allMessages);
    }

    private void fetchChatItems() {
        mConversation.fetchOlderMessages(new ChatManager.MessagesFetchedListener() {
            @Override
            public void onMessagesFetched(List<ChatMessage> msgs) {
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

    private Object mEventHandler = new Object() {
        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
            if (mConversation.getID() < 0) {
                setupAllianceChatHeader(mHeaderContent);
            }
            mChatAdapter.notifyDataSetChanged();
        }

        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onEmpireUpdated(Empire empire) {
            mChatAdapter.notifyDataSetChanged();
        }

        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onMessageAdded(ChatManager.MessageAddedEvent event) {
            if (event.conversation.getID() == mConversation.getID()) {
                appendMessage(event.msg);
                refreshUnreadCountButton();
            }
        }

        @EventHandler(thread = EventHandler.UI_THREAD)
        public void onUnreadMessageCountUpdated(ChatManager.UnreadMessageCountUpdatedEvent event) {
            refreshUnreadCountButton();
        }
    };

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
                Empire empire = null;
                if (entry.message.getEmpireID() != null) {
                    empire = EmpireManager.i.getEmpire(entry.message.getEmpireID());
                }
                Empire otherEmpire = EmpireManager.i.getEmpire(Integer.parseInt(entry.message.getMessage()));
                if (action == ChatMessage.MessageAction.ParticipantAdded) {
                    if (empire != null && otherEmpire != null) {
                        String content = String.format(Locale.ENGLISH, "%s has added %s to the conversation.",
                                empire.getDisplayName(), otherEmpire.getDisplayName());
                        message.setText(Html.fromHtml("<i>"+content+"</i>"));
                    } else {
                        message.setText(Html.fromHtml("<i>An empire has been added to the conversation."));
                    }
                } else if (action == ChatMessage.MessageAction.ParticipantLeft) {
                    if (empire != null && otherEmpire != null) {
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

                Empire empire = EmpireManager.i.getEmpire(entry.message.getEmpireID());
                if (empire != null) {
                    Bitmap shield = EmpireShieldManager.i.getShield(getActivity(), empire);
                    empireName.setText(empire.getDisplayName());
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
                ChatManager.i.startConversation(null);
            }
        });
    }

    private void setupAllianceChatHeader(View v) {
        Alliance alliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
        if (alliance == null) {
            return; // should never happen...
        }

        TextView title = (TextView) v.findViewById(R.id.title);
        title.setText(alliance.getName());

        ImageView allianceIcon = (ImageView) v.findViewById(R.id.alliance_icon);
        allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(), alliance));
    }

    private void setupPrivateChatHeader(View v) {
        // remove our own ID from the list...
        ArrayList<Integer> empireIDs = new ArrayList<Integer>();
        for (BaseChatConversationParticipant participant : mConversation.getParticipants()) {
            if (participant.getEmpireID() != EmpireManager.i.getEmpire().getID()) {
                empireIDs.add(participant.getEmpireID());
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

        if (empireIDs.size() == 0) {
            empireName.setText("Empty Chat");
        } else {
            List<Empire> empires = EmpireManager.i.getEmpires(empireIDs);
            Collections.sort(empires, new Comparator<Empire>() {
                @Override
                public int compare(Empire lhs, Empire rhs) {
                    return lhs.getDisplayName().compareTo(rhs.getDisplayName());
                }
            });

            StringBuilder sb = new StringBuilder();
            for (Empire empire : empires) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(empire.getDisplayName());
    
                ImageView icon = new ImageView(getActivity());
                icon.setLayoutParams(new LinearLayout.LayoutParams((int)(32 * pixelScale), (int)(32 * pixelScale)));
                icon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
                empireIconContainer.addView(icon);
            }
            empireName.setText(sb.toString());
        }
    }

    private static int getTotalUnreadCount() {
        int numUnread = 0;
        for (ChatConversation conversation : ChatManager.i.getConversations()) {
            numUnread += conversation.getUnreadCount();
        }
        return numUnread;
    }

    private void refreshUnreadCountButton() {
        if (mUnreadCountBtn == null) {
            return;
        }

        int numUnread = getTotalUnreadCount();

        if (numUnread > 0) {
            mUnreadCountBtn.setVisibility(View.VISIBLE);
            mUnreadCountBtn.setText(String.format("  %d  ", numUnread));
        } else {
            mUnreadCountBtn.setVisibility(View.GONE);
        }
    }
}
