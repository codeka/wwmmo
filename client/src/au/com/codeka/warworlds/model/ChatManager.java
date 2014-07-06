package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import android.content.Context;
import android.util.SparseArray;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventBus;

/**
 * This class keeps track of chats and what-not.
 */
public class ChatManager implements BackgroundDetector.BackgroundChangeHandler {
    public static ChatManager i = new ChatManager();
    private static final Log log = new Log("ChatManager");

    public static EventBus eventBus = new EventBus();

    private DateTime mMostRecentMsg;
    private boolean mRequesting;
    private SparseArray<ChatConversation> mConversations;
    private boolean mConversationsRefreshing = false;
    private boolean mIsSetup = false;
    private ChatRecentMessages mRecentMessages = new ChatRecentMessages();

    public static final int GLOBAL_CONVERSATION_ID = 0;
    public static final int ALLIANCE_CONVERSATION_ID = -1;

    private ChatManager() {
        mConversations = new SparseArray<ChatConversation>();
    }

    /**
     * Called when the game starts up, we need to register with the channel and get
     * ready to start receiving chat messages.
     */
    public void setup(Context context) {
        BackgroundDetector.i.addBackgroundChangeHandler(this);

        mConversations.clear();
        mConversations.append(GLOBAL_CONVERSATION_ID, new ChatConversation(GLOBAL_CONVERSATION_ID));
        if (EmpireManager.i.getEmpire().getAlliance() != null) {
            mConversations.append(ALLIANCE_CONVERSATION_ID, new ChatConversation(ALLIANCE_CONVERSATION_ID));
        }
        refreshConversations();

        mIsSetup = true;

        // fetch all chats from the last 24 hours -- todo: by day? or by number?
        mMostRecentMsg = (new DateTime()).minusDays(1);
        requestMessages(mMostRecentMsg);
    }

    /** Posts a message from us to the server. */
    public void postMessage(final ChatMessage msg) {
        new BackgroundRunner<ChatMessage>() {
            @Override
            protected ChatMessage doInBackground() {
                try {
                    Messages.ChatMessage.Builder pb = Messages.ChatMessage.newBuilder()
                            .setMessage(msg.getMessage())
                            .setAllianceKey(msg.getAllianceKey() == null ? "" : msg.getAllianceKey());
                    if (msg.getConversationID() != null) {
                        pb.setConversationId(msg.getConversationID());
                    }
                    Messages.ChatMessage chat_msg = ApiClient.postProtoBuf("chat", pb.build(), Messages.ChatMessage.class);
                    ChatMessage respMsg = new ChatMessage();
                    respMsg.fromProtocolBuffer(chat_msg);
                    return respMsg;
                } catch (Exception e) {
                    log.error("Error posting chat!", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(ChatMessage msg) {
                if (msg != null) {
                    ChatConversation conv = getConversation(msg);
                    if (conv != null) {
                        conv.addMessage(msg, true);
                        if (mRecentMessages.addMessage(msg)) {
                            eventBus.publish(new MessageAddedEvent(conv, msg));
                        }
                    }
                }
            }
        }.execute();
    }

    public void addMessage(ChatConversation conv, ChatMessage msg) {
        conv.addMessage(msg, true);
        if (mRecentMessages.addMessage(msg)) {
            ChatManager.eventBus.publish(new ChatManager.MessageAddedEvent(conv, msg));
        }
    }

    public ChatRecentMessages getRecentMessages() {
        return mRecentMessages;
    }

    public void reportMessageForAbuse(final Context context, final ChatMessage msg) {
        new BackgroundRunner<Boolean>() {
            String mErrorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    Messages.ChatAbuseReport pb = Messages.ChatAbuseReport.newBuilder()
                            .setChatMsgId(msg.getID())
                            .build();
                    ApiClient.postProtoBuf("chat/"+msg.getID()+"/abuse-reports", pb, null);
                    return true;
                } catch (ApiException e) {
                    if (e.getServerErrorMessage() != null) {
                        mErrorMessage = e.getServerErrorMessage();
                    }
                    return false;
                } catch (Exception e) {
                    log.error("Error posting chat!", e);
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    if (mErrorMessage == null || mErrorMessage.isEmpty()) {
                        mErrorMessage = "An error occured reporting this empire. Try again later.";
                    }

                   // try {
                        new StyledDialog.Builder(context)
                                        .setTitle("Error")
                                        .setMessage(mErrorMessage)
                                        .setPositiveButton("OK", null)
                                        .create().show();
                    //} catch (Exception e) {
                        // ignore
                    //}
                }
            }
        }.execute();

    }

    public void addParticipant(final Context context, final ChatConversation conversation, final String empireName) {
        // look in the cache first, it'll be quicker and one less request to the server...
        List<Empire> empires = EmpireManager.i.getMatchingEmpiresFromCache(empireName);
        if (empires != null && empires.size() > 0) {
            addParticipant(conversation, empires.get(0));
            return;
        }

        // otherwise we'll have to query the server anyway.
        EmpireManager.i.searchEmpires(context, empireName, new EmpireManager.SearchCompleteHandler() {
            @Override
            public void onSearchComplete(List<Empire> empires) {
                if (empires.isEmpty()) {
                    return;
                }

                addParticipant(conversation, empires.get(0));
            }
        });
    }

    /** Adds the given participant to the given conversation. */
    private void addParticipant(final ChatConversation conversation, final Empire empire) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    Messages.ChatConversationParticipant pb = Messages.ChatConversationParticipant.newBuilder()
                            .setEmpireId(Integer.parseInt(empire.getKey()))
                            .build();

                    String url = "chat/conversations/"+conversation.getID()+"/participants";
                    ApiClient.postProtoBuf(url, pb);
                    return true;
                } catch (Exception e) {
                    log.error("Error adding participant (maybe already there, etc?)", e);
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (success) {
                    // update our internal representation with the new empire
                    ChatConversation realConvo = getConversationByID(conversation.getID());
                    realConvo.getParticipants().add(new ChatConversationParticipant(Integer.parseInt(empire.getKey())));
                    eventBus.publish(new ConversationsUpdatedEvent());
                }
            }
        }.execute();
    }

    public ChatConversation getConversation(ChatMessage msg) {
        if (!mIsSetup) {
            return null;
        }

        if (msg.getConversationID() == null) {
            if (msg.getAllianceKey() == null) {
                return getGlobalConversation();
            } else {
                return getAllianceConversation();
            }
        }

        return getConversationByID(msg.getConversationID());
    }

    public ChatConversation getGlobalConversation() {
        if (!mIsSetup) {
            return null;
        }
        return mConversations.get(GLOBAL_CONVERSATION_ID);
    }

    public ChatConversation getAllianceConversation() {
        if (!mIsSetup) {
            return null;
        }
        return mConversations.get(ALLIANCE_CONVERSATION_ID);
    }

    public ArrayList<ChatConversation> getConversations() {
        ArrayList<ChatConversation> conversations = new ArrayList<ChatConversation>();
        synchronized(mConversations) {
            for (int i = 0; i < mConversations.size(); i++) {
                conversations.add(mConversations.valueAt(i));
            }
        }
        return conversations;
    }

    public ChatConversation getConversationByID(int conversationID) {
        synchronized(mConversations) {
            if (mConversations.indexOfKey(conversationID) < 0) {
                log.info("Conversation #%d hasn't been created yet, creating now.",
                        conversationID);
                mConversations.append(conversationID, new ChatConversation(conversationID));

                // it's OK to call this, it won't do anything if a refresh is already happening
                refreshConversations();
            }
        }
        return mConversations.get(conversationID);
    }

    /** Start a new conversation with the given empireID (can be null to start an empty conversation). */
    public void startConversation(final String empireID) {
        // if we already have a conversation going with this guy, just reuse that one.
        if (empireID != null) {
            for (int index = 0; index < mConversations.size(); index++) {
                ChatConversation conversation = mConversations.valueAt(index);
                List<BaseChatConversationParticipant> participants = conversation.getParticipants();
                if (participants == null || participants.size() != 2) {
                    continue;
                }
                if (participants.get(0).getEmpireID() == Integer.parseInt(empireID) ||
                    participants.get(1).getEmpireID() == Integer.parseInt(empireID)) {
                    eventBus.publish(new ConversationStartedEvent(conversation));
                    return;
                }
            }
        }

        new BackgroundRunner<ChatConversation>() {
            @Override
            protected ChatConversation doInBackground() {
                try {
                    Messages.ChatConversation.Builder conversation_pb_builder = Messages.ChatConversation.newBuilder()
                            .addParticipants(Messages.ChatConversationParticipant.newBuilder()
                                                .setEmpireId(Integer.parseInt(EmpireManager.i.getEmpire().getKey()))
                                                .setIsMuted(false));
                    if (empireID != null) {
                        conversation_pb_builder.addParticipants(Messages.ChatConversationParticipant.newBuilder()
                                                                        .setEmpireId(Integer.parseInt(empireID))
                                                                        .setIsMuted(false));
                    }
                    Messages.ChatConversation conversation_pb = conversation_pb_builder.build();
                    conversation_pb = ApiClient.postProtoBuf("chat/conversations", conversation_pb, Messages.ChatConversation.class);

                    ChatConversation conversation = new ChatConversation(conversation_pb.getId());
                    conversation.fromProtocolBuffer(conversation_pb);
                    return conversation;
                } catch (Exception e) {
                    log.error("Error starting conversation.", e);
                    return null;
                }
            }

            @Override
            protected void onComplete(ChatConversation conversation) {
                if (conversation != null) {
                    mConversations.put(conversation.getID(), conversation);
                    eventBus.publish(new ConversationStartedEvent(conversation));
                }
            }
        }.execute();
    }

    public void leaveConversation(final ChatConversation conversation) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = "chat/conversations/"+conversation.getID()+"/participants/"+EmpireManager.i.getEmpire().getKey();
                    ApiClient.delete(url);

                    synchronized(mConversations) {
                        log.info("Leaving conversation #%d.", conversation.getID());
                        mConversations.remove(conversation.getID());
                    }
                    return true;
                } catch (Exception e) {
                    log.error("Error leaving conversation.", e);
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                eventBus.publish(new ConversationsUpdatedEvent());
            }
        }.execute();
    }

    public void muteConversation(ChatConversation conversation) {
        if (conversation.getID() <= 0) {
            return;
        }
        new GlobalOptions().muteConversation(conversation.getID(), true);
    }

    public void unmuteConversation(ChatConversation conversation) {
        if (conversation.getID() <= 0) {
            return;
        }
        new GlobalOptions().muteConversation(conversation.getID(), false);
    }

    @Override
    public void onBackgroundChange(boolean isInBackground) {
        if (!isInBackground) {
            requestMessages(mMostRecentMsg);
        }
    }

    private void refreshConversations() {
        if (mConversationsRefreshing) {
            return;
        }
        mConversationsRefreshing = true;

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    String url = "chat/conversations";
                    Messages.ChatConversations pb = ApiClient.getProtoBuf(url,
                            Messages.ChatConversations.class);

                    // this comes back most recent first, but we work in the
                    // opposite order...
                    for (Messages.ChatConversation conversation_pb : pb.getConversationsList()) {
                        ChatConversation conversation = new ChatConversation(conversation_pb.getId());
                        conversation.fromProtocolBuffer(conversation_pb);
                        synchronized(mConversations) {
                            if (mConversations.indexOfKey(conversation_pb.getId()) < 0) {
                                mConversations.append(conversation_pb.getId(), conversation);
                            } else {
                                mConversations.get(conversation_pb.getId()).update(conversation);
                            }
                        }
                    }
                } catch (Exception e) {
                    // TODO: errors?
                    return false;
                }

                return true;
            }

            @Override
            protected void onComplete(Boolean success) {
                mConversationsRefreshing = false;

                // now that we've updated all of the conversations, if there's any left that are
                // "needs update" then we'll have to queue another one... :/
                boolean needsUpdate = false;
                synchronized(mConversations) {
                    for (int i = 0; i < mConversations.size(); i++) {
                        ChatConversation conversation = mConversations.valueAt(i);
                        if (conversation.getID() > 0 && conversation.needUpdate()) {
                            needsUpdate = true;
                            // However, we want to make sure this is the LAST time that "refreshConversations" is
                            // called for this set of conversations (for example, if one of our conversations
                            // doesn't exist on the server, it won't get updated by another call) so we call this
                            // to make sure "needs update" is reset on all of them first.
                            conversation.update(conversation);
                        }
                    }
                }

                if (needsUpdate) {
                    refreshConversations();
                } else {
                    eventBus.publish(new ConversationsUpdatedEvent());
                }
            }
        }.execute();
    }

    private void requestMessages(final DateTime after) {
        requestMessages(after, null, null, null, new MessagesFetchedListener() {
            @Override
            public void onMessagesFetched(List<ChatMessage> msgs) {
                for (ChatMessage msg : msgs) {
                    ChatConversation conversation = getConversation(msg);
                    conversation.addMessage(msg, true);
                    if (mRecentMessages.addMessage(msg)) {
                        eventBus.publish(new MessageAddedEvent(conversation, msg));
                    }
                }
            }
        });
    }

    public void requestMessages(final DateTime after, final DateTime before, final Integer max,
            final Integer conversationID, final MessagesFetchedListener handler) {
        if (mRequesting) {
            return;
        }
        mRequesting = true;

        new BackgroundRunner<ArrayList<ChatMessage>>() {
            @Override
            protected ArrayList<ChatMessage> doInBackground() {
                ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>();

                try {
                    String url = "chat?after="+(after.getMillis()/1000);
                    if (before != null) {
                        url += "&before="+(before.getMillis()/1000);
                    }
                    if (max != null) {
                        url += "&max="+max;
                    }
                    if (conversationID != null) {
                        url += "&conversation="+conversationID;
                    }
                    Messages.ChatMessages pb = ApiClient.getProtoBuf(url,
                            Messages.ChatMessages.class);

                    // this comes back most recent first, but we work in the
                    // opposite order...
                    for (int i = pb.getMessagesCount() - 1; i >= 0; i--) {
                        ChatMessage msg = new ChatMessage();
                        msg.fromProtocolBuffer(pb.getMessages(i));
                        msgs.add(msg);
                    }
                } catch (Exception e) {
                    log.error("Error fetching chat messages.", e);
                }

                return msgs;
            }

            @Override
            protected void onComplete(ArrayList<ChatMessage> msgs) {
                handler.onMessagesFetched(msgs);

                mRequesting = false;
            }
        }.execute();
    }

    public static class MessageAddedEvent {
        public ChatConversation conversation;
        public ChatMessage msg;

        public MessageAddedEvent(ChatConversation conversation, ChatMessage msg) {
            this.conversation = conversation;
            this.msg = msg;
        }
    }

    public static class ConversationStartedEvent {
        public ChatConversation conversation;

        public ConversationStartedEvent(ChatConversation conversation) {
            this.conversation = conversation;
        }
    }

    /** Event fired when conversation list updates. */
    public static class ConversationsUpdatedEvent {
    }

    public static class UnreadMessageCountUpdatedEvent {
        public int numUnread;

        public UnreadMessageCountUpdatedEvent(int numUnread) {
            this.numUnread = numUnread;
        }
    }

    public interface MessagesFetchedListener {
        void onMessagesFetched(List<ChatMessage> msgs);
    }
}
