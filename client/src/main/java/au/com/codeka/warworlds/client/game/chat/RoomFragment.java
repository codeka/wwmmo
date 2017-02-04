package au.com.codeka.warworlds.client.game.chat;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.activity.BaseFragment;
import au.com.codeka.warworlds.client.game.world.ChatManager;
import au.com.codeka.warworlds.client.game.world.ChatMessagesUpdatedEvent;
import au.com.codeka.warworlds.client.game.world.EmpireManager;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.common.proto.Empire;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fragment for showing the chats within a single {@link ChatRoom}.
 */
public class RoomFragment extends BaseFragment {
  private static final String CHAT_ROOM_KEY = "ChatRoom";

  private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);
  private static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("EE, dd MMM yyyy", Locale.US);

  /** Number of milliseconds worth of messages to fetch each time. */
  private static final long FETCH_TIME_MS = 6L * 3600L * 1000L; // 6 hours

  private ChatRoom room;
  private ChatAdapter chatAdapter;
  private Handler handler;
  private boolean autoTranslate;
  private ListView chatOutput;
  private boolean noMoreChats;
  private View headerContent;
  private Button unreadCountBtn;

  private long newestMessageTime;

  public static Bundle createArguments(ChatRoom room) {
    Bundle bundle = new Bundle();
    bundle.putByteArray(CHAT_ROOM_KEY, room.encode());
    return bundle;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = getArguments();
    try {
      room = ChatRoom.ADAPTER.decode(checkNotNull(args.getByteArray(CHAT_ROOM_KEY)));
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IOException.", e);
    }
    handler = new Handler();
    autoTranslate = false; //new GlobalOptions().autoTranslateChatMessages();
  }

  @Override
  protected int getViewResourceId() {
    return R.layout.frag_chat_room;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    LayoutInflater inflater = getLayoutInflater(savedInstanceState);
    FrameLayout header = (FrameLayout) view.findViewById(R.id.header);
    if (room.id == null) {
      headerContent = inflater.inflate(R.layout.chat_header_global, header, false);
      setupGlobalChatHeader(headerContent);
//    } else if (conversation.getID() < 0) {
//      headerContent = inflater.inflate(R.layout.chat_header_alliance, header, false);
//      setupAllianceChatHeader(headerContent);
//    } else {
//      headerContent = inflater.inflate(R.layout.chat_header_private, header, false);
//      setupPrivateChatHeader(headerContent);
    }
    header.addView(headerContent);

    chatAdapter = new ChatAdapter();
    chatOutput = (ListView) view.findViewById(R.id.chat_output);
    chatOutput.setAdapter(chatAdapter);

    unreadCountBtn = (Button) view.findViewById(R.id.unread_btn);
    if (unreadCountBtn != null) {
      refreshUnreadCountButton();
      unreadCountBtn.setOnClickListener(v -> {
        // move to the first conversation with an unread message
        //((ChatActivity) getActivity()).moveToFirstUnreadConversation();
      });
    }

    chatOutput.setOnItemClickListener((parent, view1, position, id) -> {
      ChatAdapter.ItemEntry entry = (ChatAdapter.ItemEntry) chatAdapter.getItem(position);
      if (entry.message == null) {
        return;
      }
      Empire empire = EmpireManager.i.getEmpire(entry.message.empire_id);
      if (empire == null) {
        return;
      }

//      ChatMessageDialog dialog = new ChatMessageDialog();
//      Bundle args = new Bundle();
//      args.putByteArray("au.com.codeka.warworlds.ChatMessage",
//          entry.message.toProtocolBuffer().toByteArray());
//      args.putByteArray("au.com.codeka.warworlds.Empire",
//          empire.toProtocolBuffer().toByteArray());
//      dialog.setArguments(args);
//      dialog.show(getActivity().getSupportFragmentManager(), "");
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    App.i.getEventBus().register(eventHandler);
    refreshMessages();
  }

  @Override
  public void onStop() {
    super.onStop();
    App.i.getEventBus().unregister(eventHandler);
  }

  private void refreshMessages() {
    long now = System.currentTimeMillis();
    List<ChatMessage> allMessages = ChatManager.i.getMessages(room, now - FETCH_TIME_MS, now);
    for (int i = 0; i < allMessages.size(); i++) {
      if (i == 0 || allMessages.get(i).date_posted > newestMessageTime) {
        newestMessageTime = allMessages.get(i).date_posted;
      }
    }
    chatAdapter.setMessages(allMessages);
  }

  /**
   * This is called when we're notified that chat messages have been updated. We'll append new
   * ones to the end and old ones to the beginning.
   */
  private void updateMessages() {
    List<ChatMessage> newerMessages = ChatManager.i.getMessages(
        room, newestMessageTime, System.currentTimeMillis() + 1000L);
    for (int i = 0; i < newerMessages.size(); i++) {
      if (newerMessages.get(i).date_posted > newestMessageTime) {
        newestMessageTime = newerMessages.get(i).date_posted;
      }
    }
    chatAdapter.appendMessages(newerMessages);
  }

  private void fetchChatItems() {/*
    conversation.fetchOlderMessages(new ChatManager.MessagesFetchedListener() {
      @Override
      public void onMessagesFetched(List<ChatMessage> msgs) {
        if (msgs.size() == 0) {
          noMoreChats = true;
        }

        // get the current item at the top
        refreshMessages();

        // figure out which position the item we had before was at
        int position = -1;
        if (msgs.size() == 0) {
          position = 0;
        } else {
          int lastMsgID = msgs.get(msgs.size() - 1).getID();
          for (int i = 0; i < chatAdapter.getCount(); i++) {
            ChatAdapter.ItemEntry thisEntry = (ChatAdapter.ItemEntry) chatAdapter.getItem(i);
            if (thisEntry.message != null && thisEntry.message.getID() == lastMsgID) {
              position = i;
              break;
            }
          }
        }

        if (position >= 0) {
          final int finalPosition = position;
          handler.post(new Runnable() {
            @Override
            public void run() {
              chatOutput.setSelection(finalPosition);
            }
          });
        }
      }
    });
  */}

  private Object eventHandler = new Object() {
    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      chatAdapter.notifyDataSetChanged();
    }

    @EventHandler
    public void onChatMessagesUpdated(ChatMessagesUpdatedEvent event) {
      updateMessages();
    }
/*
    @EventHandler
    public void onMessageAdded(ChatManager.MessageAddedEvent event) {
      if (event.conversation.getID() == conversation.getID()) {
        appendMessage(event.msg);
        refreshUnreadCountButton();
      }
    }

    @EventHandler
    public void onUnreadMessageCountUpdated(ChatManager.UnreadMessageCountUpdatedEvent event) {
      refreshUnreadCountButton();
    }*/
  };

  private class ChatAdapter extends BaseAdapter {
    private ArrayList<ItemEntry> entries;

    public ChatAdapter() {
      entries = new ArrayList<>();
    }

    public void setMessages(List<ChatMessage> messages) {
      entries.clear();
      if (!noMoreChats) {
        // we always add an empty entry to mark the end of the messages,  unless there's no
        // more chats left.
        entries.add(new ItemEntry());
      }

      appendMessages(messages);
    }

    public void appendMessages(List<ChatMessage> msgs) {
      for (ChatMessage msg : msgs) {
        appendMessage(msg, false);
      }
      notifyDataSetChanged();
    }

    public void appendMessage(ChatMessage msg, boolean notify) {
      boolean needsDateHeader = false;
      if (entries.size() == 0) {
        needsDateHeader = true;
      } else if (entries.get(entries.size() - 1).message != null) {
//        DateTime lastDate = entries.get(entries.size() - 1).message.getDatePosted()
//            .withZone(DateTimeZone.getDefault());
//        DateTime thisDate = msg.getDatePosted().withZone(DateTimeZone.getDefault());
//
//        if (lastDate.getYear() != thisDate.getYear()
//            || lastDate.getDayOfYear() != thisDate.getDayOfYear()) {
//          needsDateHeader = true;
//        }
      }

      if (needsDateHeader) {
        entries.add(new ItemEntry(msg.date_posted));
      }
      entries.add(new ItemEntry(msg));
      if (notify) {
        notifyDataSetChanged();
      }
    }

    @Override
    public int getViewTypeCount() {
      return 3;
    }

    @Override
    public int getItemViewType(int position) {
      ItemEntry entry = entries.get(position);
      if (entry.message == null && entry.date == null) {
        // 2 == "loading"
        return 2;
      }

      if (entry.date != null) {
        // 1 == "simple"
        return 1;
      }
      if (entry.message.action != null
          && entry.message.action != ChatMessage.MessageAction.Normal) {
        return 1;
      }
      if (entry.message.empire_id == null) {
        return 1;
      }

      // 0 == "normal"
      return 0;
    }

    @Override
    public int getCount() {
      return entries.size();
    }

    @Override
    public Object getItem(int position) {
      return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ItemEntry entry = entries.get(position);
      ChatMessage.MessageAction action = ChatMessage.MessageAction.Normal;
      if (entry.message != null && entry.message.action != null) {
        action = entry.message.action;
      }

      View view = convertView;
      if (view == null) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);

        if (entry.date == null && entry.message == null) {
          view = inflater.inflate(R.layout.chat_row_loading, parent, false);
        } else if (entry.date != null || action != ChatMessage.MessageAction.Normal) {
          view = inflater.inflate(R.layout.chat_row_simple, parent, false);
        } else {
          view = inflater.inflate(R.layout.chat_row, parent, false);
        }
      }

      if (entry.date == null && entry.message == null) {
        // this implies we're at the end of the list, fetch the next bunch
        handler.post(RoomFragment.this::fetchChatItems);
      } else if (entry.date != null) {
        TextView message = (TextView) view.findViewById(R.id.message);
        message.setTextColor(Color.LTGRAY);
        message.setGravity(Gravity.END);
        message.setText(DATE_FORMAT.format(new Date(entry.date)));
      } else if (action != ChatMessage.MessageAction.Normal) {
        TextView message = (TextView) view.findViewById(R.id.message);
        message.setTextColor(Color.LTGRAY);
        message.setGravity(Gravity.START);
        Empire empire = null;
        if (entry.message.empire_id != null) {
          empire = EmpireManager.i.getEmpire(entry.message.empire_id);
        }
        Empire otherEmpire =
            EmpireManager.i.getEmpire(Long.parseLong(entry.message.message));
        if (action == ChatMessage.MessageAction.ParticipantAdded) {
          if (empire != null && otherEmpire != null) {
            String content = String.format(Locale.ENGLISH, "%s has added %s to the conversation.",
                empire.display_name, otherEmpire.display_name);
            message.setText(Html.fromHtml("<i>" + content + "</i>"));
          } else {
            message.setText(Html.fromHtml("<i>An empire has been added to the conversation."));
          }
        } else if (action == ChatMessage.MessageAction.ParticipantLeft) {
          if (empire != null && otherEmpire != null) {
            String content = String.format(Locale.ENGLISH, "%s has left the conversation.",
                otherEmpire.display_name);
            message.setText(Html.fromHtml("<i>" + content + "</i>"));
          } else {
            message.setText(Html.fromHtml("<i>An empire has left the conversation."));
          }
        }
      } else if (entry.message.empire_id == null) {
        ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
        TextView empireName = (TextView) view.findViewById(R.id.empire_name);
        TextView msgTime = (TextView) view.findViewById(R.id.msg_time);
        TextView message = (TextView) view.findViewById(R.id.message);

        empireName.setText("");
        empireIcon.setImageBitmap(null);
        msgTime.setText(TIME_FORMAT.format(new Date(entry.message.date_posted)));

        String html = ChatHelper.format(entry.message, true, true, false);
        message.setText(Html.fromHtml("<font color=\"#00ffff\"><b>[SERVER]</b></font> " + html));

        if (html.contains("<a ")) { // only if there's actually a link...
          message.setMovementMethod(LinkMovementMethod.getInstance());
        }
      } else {
        ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
        TextView empireName = (TextView) view.findViewById(R.id.empire_name);
        TextView msgTime = (TextView) view.findViewById(R.id.msg_time);
        TextView message = (TextView) view.findViewById(R.id.message);

        Empire empire = EmpireManager.i.getEmpire(entry.message.empire_id);
        if (empire != null) {
          empireName.setText(empire.display_name);
          ImageHelper.bindEmpireShield(empireIcon, empire);
        } else {
          empireIcon.setImageBitmap(null);
          empireName.setText("");
        }

        msgTime.setText(TIME_FORMAT.format(new Date(entry.message.date_posted)));
        String html = ChatHelper.format(entry.message, room.id == null, true, autoTranslate);
        message.setText(Html.fromHtml(html));
        if (html.contains("<a ")) { // only if there's actually a link...
          message.setMovementMethod(LinkMovementMethod.getInstance());
        }
      }

      return view;
    }

    private class ItemEntry {
      public ChatMessage message;
      public Long date;

      public ItemEntry() {
      }

      public ItemEntry(ChatMessage message) {
        this.message = message;
      }

      public ItemEntry(long date) {
        this.date = date;
      }
    }
  }

  private void setupGlobalChatHeader(View v) {
    ImageButton settingsBtn = (ImageButton) v.findViewById(R.id.settings_btn);
    settingsBtn.setOnClickListener(v1 -> {
//        ChatGlobalSettingsDialog dialog = new ChatGlobalSettingsDialog();
//        dialog.show(getActivity().getSupportFragmentManager(), "");
    });

    Button newGroupBtn = (Button) v.findViewById(R.id.new_group_btn);
    newGroupBtn.setOnClickListener(v12 -> {
//        ChatManager.i.startConversation(null);
    });
  }

  private void setupAllianceChatHeader(View v) {/*
    Alliance alliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
    if (alliance == null) {
      return; // should never happen...
    }

    TextView title = (TextView) v.findViewById(R.id.title);
    title.setText(alliance.getName());

    ImageView allianceIcon = (ImageView) v.findViewById(R.id.alliance_icon);
    allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(), alliance));
  */}

  private void setupPrivateChatHeader(View v) {/*
    // remove our own ID from the list...
    ArrayList<Integer> empireIDs = new ArrayList<>();
    for (BaseChatConversationParticipant participant : conversation.getParticipants()) {
      if (participant.getEmpireID() != EmpireManager.i.getEmpire().getID()) {
        empireIDs.add(participant.getEmpireID());
      }
    }

    final LinearLayout empireIconContainer =
        (LinearLayout) v.findViewById(R.id.empire_icon_container);
    final TextView empireName = (TextView) v.findViewById(R.id.title);
    final double pixelScale = getActivity().getResources().getDisplayMetrics().density;

    ImageButton settingsBtn = (ImageButton) v.findViewById(R.id.settings_btn);
    settingsBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        ChatPrivateSettingsDialog dialog = new ChatPrivateSettingsDialog();
        Bundle args = new Bundle();
        Messages.ChatConversation.Builder chat_conversation_pb =
            Messages.ChatConversation.newBuilder();
        conversation.toProtocolBuffer(chat_conversation_pb);
        args.putByteArray("au.com.codeka.warworlds.ChatConversation",
            chat_conversation_pb.build().toByteArray());
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
        icon.setLayoutParams(
            new LinearLayout.LayoutParams((int) (32 * pixelScale), (int) (32 * pixelScale)));
        icon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
        empireIconContainer.addView(icon);
      }
      empireName.setText(sb.toString());
    }
  */}

  private static int getTotalUnreadCount() {
    int numUnread = 0;
    /*for (ChatConversation conversation : ChatManager.i.getConversations()) {
      numUnread += conversation.getUnreadCount();
    }*/
    return numUnread;
  }

  private void refreshUnreadCountButton() {
    if (unreadCountBtn == null) {
      return;
    }

    int numUnread = getTotalUnreadCount();

    if (numUnread > 0) {
      unreadCountBtn.setVisibility(View.VISIBLE);
      unreadCountBtn.setText(String.format(Locale.US, "  %d  ", numUnread));
    } else {
      unreadCountBtn.setVisibility(View.GONE);
    }
  }
}
