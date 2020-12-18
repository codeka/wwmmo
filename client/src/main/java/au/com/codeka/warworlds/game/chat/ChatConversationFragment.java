package au.com.codeka.warworlds.game.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.AccountsActivity;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.Util;
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
import au.com.codeka.warworlds.ui.BaseFragment;

public class ChatConversationFragment extends BaseFragment {
  private ChatConversation conversation;
  private ChatAdapter chatAdapter;
  private Handler handler;
  private boolean autoTranslate;
  private ListView chatOutput;
  private boolean noMoreChats;
  private View headerContent;
  private Button unreadCountBtn;

  private ChatConversationFragmentArgs args;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    args = ChatConversationFragmentArgs.fromBundle(requireArguments());
    conversation = ChatManager.i.getConversationByID(args.getConversationID());
    handler = new Handler();
    autoTranslate = new GlobalOptions().autoTranslateChatMessages();
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.chat_page, container, false);

    FrameLayout header = v.findViewById(R.id.header);
    if (conversation.getID() == 0) {
      headerContent = inflater.inflate(R.layout.chat_header_global, header, false);
      setupGlobalChatHeader(headerContent);
    } else if (conversation.getID() < 0) {
      headerContent = inflater.inflate(R.layout.chat_header_alliance, header, false);
      setupAllianceChatHeader(headerContent);
    } else {
      headerContent = inflater.inflate(R.layout.chat_header_private, header, false);
      setupPrivateChatHeader(headerContent);
    }
    header.addView(headerContent);

    chatAdapter = new ChatAdapter();
    chatOutput = v.findViewById(R.id.chat_output);
    chatOutput.setAdapter(chatAdapter);

    unreadCountBtn = v.findViewById(R.id.unread_btn);
    if (unreadCountBtn != null) {
      refreshUnreadCountButton();
      unreadCountBtn.setOnClickListener(v1 -> {
        // move to the first conversation with an unread message
        /* TODO
        ChatFragment activity = (ChatFragment) getActivity();
        if (activity != null) {
          activity.moveToFirstUnreadConversation();
        }
        */
      });
    }

    chatOutput.setOnItemClickListener((parent, view, position, id) -> {
      ChatAdapter.ItemEntry entry = (ChatAdapter.ItemEntry) chatAdapter.getItem(position);
      if (entry.message == null) {
        return;
      }
      Empire empire = EmpireManager.i.getEmpire(entry.message.getEmpireID());
      if (empire == null) {
        return;
      }

      ChatMessageDialog dialog = new ChatMessageDialog();
      Bundle args = new Bundle();
      args.putByteArray("au.com.codeka.warworlds.ChatMessage",
          entry.message.toProtocolBuffer().toByteArray());
      args.putByteArray("au.com.codeka.warworlds.Empire",
          empire.toProtocolBuffer().toByteArray());
      dialog.setArguments(args);
      FragmentActivity activity = getActivity();
      if (activity != null) {
        dialog.show(activity.getSupportFragmentManager(), "");
      }
    });

    View chatUnavailableContainer = v.findViewById(R.id.anon_chat_disabled_container);
    if (Util.isAnonymous()) {
      chatUnavailableContainer.setVisibility(View.VISIBLE);

      Button signInBtn = v.findViewById(R.id.sign_in_btn);
      signInBtn.setOnClickListener(v12 -> {
        final Intent intent = new Intent(getContext(), AccountsActivity.class);
        startActivity(intent);
      });

      Button cancelBtn = v.findViewById(R.id.cancel_btn);
      cancelBtn.setOnClickListener(v1 -> chatUnavailableContainer.setVisibility(View.GONE));
    }

    return v;
  }

  @Override
  public void onStart() {
    super.onStart();
    ChatManager.eventBus.register(eventHandler);
    EmpireManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
    refreshMessages();
  }

  @Override
  public void onResume() {
    super.onResume();

    // Because we use FragmentStatePagerAdapter#BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT only the
    // current fragment is resumed. So we can use this to set up the action bar.
    ActionBar actionBar = requireMainActivity().requireSupportActionBar();
    actionBar.setTitle(getChatName());
  }

  @Override
  public void onStop() {
    super.onStop();
    ShieldManager.eventBus.unregister(eventHandler);
    EmpireManager.eventBus.unregister(eventHandler);
    ChatManager.eventBus.unregister(eventHandler);
  }

  private void refreshMessages() {
    ArrayList<ChatMessage> allMessages = new ArrayList<>(conversation.getAllMessages());
    chatAdapter.setMessages(allMessages);
  }

  private void fetchChatItems() {
    conversation.fetchOlderMessages(msgs -> {
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
        handler.post(() -> chatOutput.setSelection(finalPosition));
      }
    });
  }

  private void appendMessage(final ChatMessage msg) {
    chatAdapter.appendMessage(msg);
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      if (conversation.getID() < 0) {
        setupAllianceChatHeader(headerContent);
      }
      chatAdapter.notifyDataSetChanged();
    }

    @EventHandler
    public void onEmpireUpdated(Empire empire) {
      chatAdapter.notifyDataSetChanged();
    }

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
    }
  };

  private class ChatAdapter extends BaseAdapter {
    private ArrayList<ItemEntry> entries;

    public ChatAdapter() {
      entries = new ArrayList<>();
    }

    public void setMessages(ArrayList<ChatMessage> messages) {
      entries.clear();
      if (!noMoreChats) {
        // we always add an empty entry to mark the end of the messages, well,
        // unless there's no more chats left
        entries.add(new ItemEntry());
      }

      for (ChatMessage msg : messages) {
        appendMessage(msg);
      }
      notifyDataSetChanged();
    }

    public void appendMessage(ChatMessage msg) {
      boolean needsDateHeader = false;
      if (entries.size() == 0) {
        needsDateHeader = true;
      } else if (entries.get(entries.size() - 1).message != null) {
        DateTime lastDate = entries.get(entries.size() - 1).message.getDatePosted()
            .withZone(DateTimeZone.getDefault());
        DateTime thisDate = msg.getDatePosted().withZone(DateTimeZone.getDefault());

        if (lastDate.getYear() != thisDate.getYear()
            || lastDate.getDayOfYear() != thisDate.getDayOfYear()) {
          needsDateHeader = true;
        }
      }

      if (needsDateHeader) {
        entries.add(new ItemEntry(msg.getDatePosted()));
      }
      entries.add(new ItemEntry(msg));
      notifyDataSetChanged();
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
      if (entry.message.getAction() != null
          && entry.message.getAction() != ChatMessage.MessageAction.Normal) {
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
      if (entry.message != null && entry.message.getAction() != null) {
        action = entry.message.getAction();
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
        handler.post(ChatConversationFragment.this::fetchChatItems);
      } else if (entry.date != null) {
        TextView message = view.findViewById(R.id.message);
        message.setTextColor(Color.LTGRAY);
        message.setGravity(Gravity.END);
        message.setText(entry.date.toString("EE, dd MMM yyyy"));
      } else if (action == ChatMessage.MessageAction.ErrorMessage) {
        TextView message = view.findViewById(R.id.message);
        message.setTextColor(Color.RED);
        message.setGravity(Gravity.START);
        message.setText(entry.message.getMessage());
      } else if (action != ChatMessage.MessageAction.Normal) {
        TextView message = view.findViewById(R.id.message);
        message.setTextColor(Color.LTGRAY);
        message.setGravity(Gravity.START);
        Empire empire = null;
        if (entry.message.getEmpireID() != null) {
          empire = EmpireManager.i.getEmpire(entry.message.getEmpireID());
        }
        Empire otherEmpire =
            EmpireManager.i.getEmpire(Integer.parseInt(entry.message.getMessage()));
        if (action == ChatMessage.MessageAction.ParticipantAdded) {
          if (empire != null && otherEmpire != null) {
            String content = String.format(Locale.ENGLISH, "%s has added %s to the conversation.",
                empire.getDisplayName(), otherEmpire.getDisplayName());
            message.setText(Html.fromHtml("<i>" + content + "</i>"));
          } else {
            message.setText(Html.fromHtml("<i>An empire has been added to the conversation."));
          }
        } else if (action == ChatMessage.MessageAction.ParticipantLeft) {
          if (empire != null && otherEmpire != null) {
            String content = String.format(Locale.ENGLISH, "%s has left the conversation.",
                otherEmpire.getDisplayName());
            message.setText(Html.fromHtml("<i>" + content + "</i>"));
          } else {
            message.setText(Html.fromHtml("<i>An empire has left the conversation."));
          }
        }
      } else if (entry.message.getEmpireKey() == null) {
        ImageView empireIcon = view.findViewById(R.id.empire_icon);
        TextView empireName = view.findViewById(R.id.empire_name);
        TextView msgTime = view.findViewById(R.id.msg_time);
        TextView message = view.findViewById(R.id.message);

        empireName.setText("");
        empireIcon.setImageBitmap(null);
        msgTime.setText(
            entry.message.getDatePosted().withZone(DateTimeZone.getDefault()).toString("h:mm a"));

        String html = entry.message.format(true, true, false);
        message.setText(Html.fromHtml("<font color=\"#00ffff\"><b>[SERVER]</b></font> " + html));

        if (html.contains("<a ")) { // only if there's actually a link...
          message.setMovementMethod(LinkMovementMethod.getInstance());
        }
      } else {
        ImageView empireIcon = view.findViewById(R.id.empire_icon);
        TextView empireName = view.findViewById(R.id.empire_name);
        TextView msgTime = view.findViewById(R.id.msg_time);
        TextView message = view.findViewById(R.id.message);

        Empire empire = EmpireManager.i.getEmpire(entry.message.getEmpireID());
        if (empire != null) {
          Bitmap shield = EmpireShieldManager.i.getShield(getActivity(), empire);
          empireName.setText(empire.getDisplayName());
          empireIcon.setImageBitmap(shield);
        } else {
          empireIcon.setImageBitmap(null);
          empireName.setText("");
        }

        msgTime.setText(
            entry.message.getDatePosted().withZone(DateTimeZone.getDefault()).toString("h:mm a"));
        String html = entry.message.format(conversation.getID() == 0, true, autoTranslate);
        message.setText(Html.fromHtml(html));
        if (html.contains("<a ")) { // only if there's actually a link...
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

  private void setupGlobalChatHeader(View view) {
    ImageButton settingsBtn = view.findViewById(R.id.settings_btn);
    settingsBtn.setOnClickListener(v -> {
      ChatGlobalSettingsDialog dialog = new ChatGlobalSettingsDialog();
      FragmentActivity activity = getActivity();
      if (activity != null) {
        dialog.show(activity.getSupportFragmentManager(), "");
      }
    });

    Button blockedBtn = view.findViewById(R.id.blocked_btn);
    blockedBtn.setOnClickListener(
        v -> startActivity(new Intent(getActivity(), BlockedEmpiresActivity.class)));

    Button newGroupBtn = view.findViewById(R.id.new_group_btn);
    newGroupBtn.setOnClickListener(
        v -> ChatManager.i.startConversation(null));
  }

  private void setupAllianceChatHeader(View v) {
    Alliance alliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
    if (alliance == null) {
      return; // should never happen...
    }

    TextView title = v.findViewById(R.id.title);
    title.setText(alliance.getName());

    ImageView allianceIcon = v.findViewById(R.id.alliance_icon);
    allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(), alliance));
  }

  private void setupPrivateChatHeader(View v) {
    // remove our own ID from the list...
    ArrayList<Integer> empireIDs = new ArrayList<>();
    for (BaseChatConversationParticipant participant : conversation.getParticipants()) {
      if (participant.getEmpireID() != EmpireManager.i.getEmpire().getID()) {
        empireIDs.add(participant.getEmpireID());
      }
    }

    final LinearLayout empireIconContainer =
        v.findViewById(R.id.empire_icon_container);
    final TextView empireName = v.findViewById(R.id.title);
    final double pixelScale = requireActivity().getResources().getDisplayMetrics().density;

    ImageButton settingsBtn = v.findViewById(R.id.settings_btn);
    settingsBtn.setOnClickListener(v1 -> {
      ChatPrivateSettingsDialog dialog = new ChatPrivateSettingsDialog();
      Bundle args = new Bundle();
      Messages.ChatConversation.Builder chat_conversation_pb =
          Messages.ChatConversation.newBuilder();
      conversation.toProtocolBuffer(chat_conversation_pb);
      args.putByteArray("au.com.codeka.warworlds.ChatConversation",
          chat_conversation_pb.build().toByteArray());
      dialog.setArguments(args);
      dialog.show(getChildFragmentManager(), "");
    });

    if (empireIDs.size() == 0) {
      empireName.setText("Empty Chat");
    } else {
      List<Empire> empires = EmpireManager.i.getEmpires(empireIDs);
      Collections.sort(empires, (lhs, rhs) -> lhs.getDisplayName().compareTo(rhs.getDisplayName()));

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
  }

  private String getChatName() {
    if (conversation.getID() == 0) {
      return "Global";
    } else if (conversation.getID() < 0) {
      Alliance alliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
      if (alliance == null) {
        return "Alliance"; // should never happen...
      }
      return alliance.getName();
    } else {
      ArrayList<Integer> empireIDs = new ArrayList<>();
      for (BaseChatConversationParticipant participant : conversation.getParticipants()) {
        if (participant.getEmpireID() != EmpireManager.i.getEmpire().getID()) {
          empireIDs.add(participant.getEmpireID());
        }
      }

      if (empireIDs.size() == 0) {
        return "Empty Chat";
      } else {
        List<Empire> empires = EmpireManager.i.getEmpires(empireIDs);
        Collections.sort(empires, (lhs, rhs) -> lhs.getDisplayName().compareTo(rhs.getDisplayName()));

        StringBuilder sb = new StringBuilder();
        for (Empire empire : empires) {
          if (sb.length() > 0) {
            sb.append(", ");
          }
          sb.append(empire.getDisplayName());
        }
        return sb.toString();
      }
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
    if (unreadCountBtn == null) {
      return;
    }

    int numUnread = getTotalUnreadCount();

    if (numUnread > 0) {
      unreadCountBtn.setVisibility(View.VISIBLE);
      unreadCountBtn.setText(String.format(Locale.ENGLISH, "  %d  ", numUnread));
    } else {
      unreadCountBtn.setVisibility(View.GONE);
    }
  }
}
