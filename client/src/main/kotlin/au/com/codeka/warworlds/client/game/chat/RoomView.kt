package au.com.codeka.warworlds.client.game.chat

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.game.world.ChatManager
import au.com.codeka.warworlds.client.game.world.ChatMessagesUpdatedEvent
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.client.game.world.ImageHelper
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatMessage.MessageAction
import au.com.codeka.warworlds.common.proto.ChatRoom
import au.com.codeka.warworlds.common.proto.Empire
import java.text.SimpleDateFormat
import java.util.*

/**
 * View for showing the chats within a single [ChatRoom].
 */
class RoomView(context: Context?, private val room: ChatRoom) : RelativeLayout(context) {
  private val chatAdapter: ChatAdapter
  private val autoTranslate: Boolean
  private val chatOutput: ListView
  private val noMoreChats = false
  private var headerContent: View? = null
  private val unreadCountBtn: Button?
  private var newestMessageTime: Long = 0
  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    App.eventBus.register(eventHandler)
    refreshMessages()
  }

  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    App.eventBus.unregister(eventHandler)
  }

  private fun refreshMessages() {
    val now = System.currentTimeMillis()
    val allMessages = ChatManager.i.getMessages(room, now, BATCH_SIZE)
    for (i in allMessages.indices) {
      val datePosted = allMessages[i].date_posted
      if (i == 0 || datePosted > newestMessageTime) {
        newestMessageTime = datePosted
      }
    }
    chatAdapter.setMessages(allMessages)
  }

  /**
   * This is called when we're notified that chat messages have been updated. We'll append new
   * ones to the end and old ones to the beginning.
   */
  private fun updateMessages() {
    val newerMessages = ChatManager.i.getMessagesAfter(room, newestMessageTime)
    for (i in newerMessages.indices) {
      val datePosted = newerMessages[i].date_posted
      if (datePosted > newestMessageTime) {
        newestMessageTime = datePosted
      }
    }
    chatAdapter.appendMessages(newerMessages)
  }

  private fun fetchChatItems() { /*
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
  */
  }

  private inner class ChatAdapter : BaseAdapter() {
    private val entries: ArrayList<ItemEntry>
    fun setMessages(messages: List<ChatMessage>) {
      entries.clear()
      if (!noMoreChats) {
        // we always add an empty entry to mark the end of the messages,  unless there's no
        // more chats left.
        entries.add(ItemEntry())
      }
      appendMessages(messages)
    }

    fun appendMessages(msgs: List<ChatMessage>) {
      for (msg in msgs) {
        appendMessage(msg, false)
      }
      notifyDataSetChanged()
    }

    fun appendMessage(msg: ChatMessage, notify: Boolean) {
      var needsDateHeader = false
      if (entries.size == 0) {
        needsDateHeader = true
      } else if (entries[entries.size - 1].message != null) {
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
        entries.add(ItemEntry(msg.date_posted))
      }
      entries.add(ItemEntry(msg))
      if (notify) {
        notifyDataSetChanged()
      }
    }

    override fun getViewTypeCount(): Int {
      return 3
    }

    override fun getItemViewType(position: Int): Int {
      val entry = entries[position]
      if (entry.message == null && entry.date == null) {
        // 2 == "loading"
        return 2
      }
      if (entry.date != null) {
        // 1 == "simple"
        return 1
      }
      if (entry.message!!.action != null
          && entry.message!!.action != MessageAction.Normal) {
        return 1
      }
      return if (entry.message!!.empire_id == null) {
        1
      } else 0

      // 0 == "normal"
    }

    override fun getCount(): Int {
      return entries.size
    }

    override fun getItem(position: Int): Any {
      return entries[position]
    }

    override fun getItemId(position: Int): Long {
      return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val entry = entries[position]
      var action = MessageAction.Normal
      if (entry.message != null && entry.message!!.action != null) {
        action = entry.message?.action!!
      }
      var view = convertView ?: run {
        val inflater = context.getSystemService(
          Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        if (entry.date == null && entry.message == null) {
          inflater.inflate(R.layout.chat_row_loading, parent, false)
        } else if (entry.date != null || action != MessageAction.Normal) {
          inflater.inflate(R.layout.chat_row_simple, parent, false)
        } else {
          inflater.inflate(R.layout.chat_row, parent, false)
        }
      }
      if (entry.date == null && entry.message == null) {
        // this implies we're at the end of the list, fetch the next bunch
        post { fetchChatItems() }
      } else if (entry.date != null) {
        val message = view.findViewById<TextView>(R.id.message)
        message.setTextColor(Color.LTGRAY)
        message.gravity = Gravity.END
        message.text = DATE_FORMAT.format(Date(entry.date!!))
      } else if (action != MessageAction.Normal) {
        val message = view.findViewById<TextView>(R.id.message)
        message.setTextColor(Color.LTGRAY)
        message.gravity = Gravity.START
        var empire: Empire? = null
        if (entry.message!!.empire_id != null) {
          empire = EmpireManager.getEmpire(entry.message!!.empire_id)
        }
        val otherEmpire = EmpireManager.getEmpire(entry.message!!.message.toLong())
        if (action == MessageAction.ParticipantAdded) {
          if (empire != null && otherEmpire != null) {
            val content = String.format(Locale.ENGLISH, "%s has added %s to the conversation.",
                empire.display_name, otherEmpire.display_name)
            message.text = Html.fromHtml("<i>$content</i>")
          } else {
            message.text = Html.fromHtml("<i>An empire has been added to the conversation.")
          }
        } else if (action == MessageAction.ParticipantLeft) {
          if (empire != null && otherEmpire != null) {
            val content = String.format(Locale.ENGLISH, "%s has left the conversation.",
                otherEmpire.display_name)
            message.text = Html.fromHtml("<i>$content</i>")
          } else {
            message.text = Html.fromHtml("<i>An empire has left the conversation.")
          }
        }
      } else if (entry.message!!.empire_id == null) {
        val empireIcon = view.findViewById<ImageView>(R.id.empire_icon)
        val empireName = view.findViewById<TextView>(R.id.empire_name)
        val msgTime = view.findViewById<TextView>(R.id.msg_time)
        val message = view.findViewById<TextView>(R.id.message)
        empireName.text = ""
        empireIcon.setImageBitmap(null)
        msgTime.text = TIME_FORMAT.format(Date(entry.message!!.date_posted))
        val html = ChatHelper.format(entry.message!!, true, true, false)
        message.text = Html.fromHtml("<font color=\"#00ffff\"><b>[SERVER]</b></font> $html")
        if (html.contains("<a ")) { // only if there's actually a link...
          message.movementMethod = LinkMovementMethod.getInstance()
        }
      } else {
        val empireIcon = view.findViewById<ImageView>(R.id.empire_icon)
        val empireName = view.findViewById<TextView>(R.id.empire_name)
        val msgTime = view.findViewById<TextView>(R.id.msg_time)
        val message = view.findViewById<TextView>(R.id.message)
        val empire = EmpireManager.getEmpire(entry.message!!.empire_id)
        if (empire != null) {
          empireName.text = empire.display_name
          ImageHelper.bindEmpireShield(empireIcon, empire)
        } else {
          empireIcon.setImageBitmap(null)
          empireName.text = ""
        }
        msgTime.text = TIME_FORMAT.format(Date(entry.message!!.date_posted))
        val html = ChatHelper.format(entry.message!!, room.id == null, true, autoTranslate)
        message.text = Html.fromHtml(html)
        if (html!!.contains("<a ")) { // only if there's actually a link...
          message.movementMethod = LinkMovementMethod.getInstance()
        }
      }
      return view
    }

    inner class ItemEntry {
      var message: ChatMessage? = null
      var date: Long? = null

      constructor() {}
      constructor(message: ChatMessage?) {
        this.message = message
      }

      constructor(date: Long) {
        this.date = date
      }
    }

    init {
      entries = ArrayList()
    }
  }

  private fun setupGlobalChatHeader(v: View?) {
    val settingsBtn = v!!.findViewById<ImageButton>(R.id.settings_btn)
    settingsBtn.setOnClickListener { v1: View? -> }
    val newGroupBtn = v.findViewById<Button>(R.id.new_group_btn)
    newGroupBtn.setOnClickListener { v12: View? -> }
  }

  private fun setupAllianceChatHeader(v: View) { /*
    Alliance alliance = (Alliance) EmpireManager.i.getEmpire().getAlliance();
    if (alliance == null) {
      return; // should never happen...
    }

    TextView title = (TextView) v.findViewById(R.id.title);
    title.setText(alliance.getName());

    ImageView allianceIcon = (ImageView) v.findViewById(R.id.alliance_icon);
    allianceIcon.setImageBitmap(AllianceShieldManager.i.getShield(getActivity(), alliance));
  */
  }

  private fun setupPrivateChatHeader(v: View) { /*
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
  */
  }

  private fun refreshUnreadCountButton() {
    if (unreadCountBtn == null) {
      return
    }
    val numUnread = totalUnreadCount
    if (numUnread > 0) {
      unreadCountBtn.visibility = View.VISIBLE
      unreadCountBtn.text = String.format(Locale.US, "  %d  ", numUnread)
    } else {
      unreadCountBtn.visibility = View.GONE
    }
  }

  companion object {
    private val TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.US)
    private val DATE_FORMAT = SimpleDateFormat("EE, dd MMM yyyy", Locale.US)

    /** Number of messages to fetch in a batch.  */
    private const val BATCH_SIZE = 10

    /*for (ChatConversation conversation : ChatManager.i.getConversations()) {
      numUnread += conversation.getUnreadCount();
    }*/
    private val totalUnreadCount: Int
      private get() =/*for (ChatConversation conversation : ChatManager.i.getConversations()) {
  numUnread += conversation.getUnreadCount();
}*/0
  }

  init {
    View.inflate(context, R.layout.chat_room, this)
    autoTranslate = false //new GlobalOptions().autoTranslateChatMessages();
    val inflater = LayoutInflater.from(context)
    val header = findViewById<FrameLayout>(R.id.header)
    if (room.id == null) {
      headerContent = inflater.inflate(R.layout.chat_header_global, header, false)
      setupGlobalChatHeader(headerContent)
      //    } else if (conversation.getID() < 0) {
//      headerContent = inflater.inflate(R.layout.chat_header_alliance, header, false);
//      setupAllianceChatHeader(headerContent);
//    } else {
//      headerContent = inflater.inflate(R.layout.chat_header_private, header, false);
//      setupPrivateChatHeader(headerContent);
    }
    header.addView(headerContent)
    chatAdapter = ChatAdapter()
    chatOutput = findViewById(R.id.chat_output)
    chatOutput.adapter = chatAdapter
    unreadCountBtn = findViewById(R.id.unread_btn)
    if (unreadCountBtn != null) {
      refreshUnreadCountButton()
      unreadCountBtn.setOnClickListener(OnClickListener { v: View? -> })
    }
    chatOutput.onItemClickListener = OnItemClickListener { parent: AdapterView<*>?, view1: View?, position: Int, id: Long ->
      val entry = chatAdapter.getItem(position) as ChatAdapter.ItemEntry
      if (entry.message == null) {
        return@OnItemClickListener
      }
      val empire = EmpireManager.getEmpire(entry.message!!.empire_id)
          ?: return@OnItemClickListener
    }
  }

  private val eventHandler: Any = object : Any() {
    @EventHandler
    fun onEmpireUpdated(empire: Empire?) {
      chatAdapter.notifyDataSetChanged()
    }

    @EventHandler
    fun onChatMessagesUpdated(event: ChatMessagesUpdatedEvent?) {
      updateMessages()
    } /*
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
  }
}