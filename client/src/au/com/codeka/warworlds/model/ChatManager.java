package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Handler;
import android.util.SparseArray;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.Notifications;
import au.com.codeka.warworlds.api.ApiClient;

/**
 * This class keeps track of chats and what-not.
 */
public class ChatManager implements BackgroundDetector.BackgroundChangeHandler {
    public static ChatManager i = new ChatManager();
    private static Logger log = LoggerFactory.getLogger(ChatManager.class);

    private ArrayList<MessageAddedListener> mMessageAddedListeners;
    private ArrayList<MessageUpdatedListener> mMessageUpdatedListeners;
    private DateTime mMostRecentMsg;
    private boolean mRequesting;
    private TreeSet<String> mEmpiresToRefresh;
    private Handler mHandler;
    private SparseArray<ChatConversation> mConversations;

    private ChatManager() {
        mMessageAddedListeners = new ArrayList<MessageAddedListener>();
        mMessageUpdatedListeners = new ArrayList<MessageUpdatedListener>();
        mEmpiresToRefresh = new TreeSet<String>();
        mConversations = new SparseArray<ChatConversation>();
    }

    /**
     * Called when the game starts up, we need to register with the channel and get
     * ready to start receiving chat messages.
     */
    public void setup(Context context) {
        BackgroundDetector.getInstance().addBackgroundChangeHandler(this);

        mHandler = new Handler();
        mConversations.clear();
        mConversations.append(0, new ChatConversation(0));
        mConversations.append(-1, new ChatConversation(-1));
        refreshConversations();

        // fetch all chats from the last 24 hours -- todo: by day? or by number?
        mMostRecentMsg = (new DateTime()).minusDays(1);
        requestMessages(mMostRecentMsg);
    }

    public void addMessageAddedListener(MessageAddedListener listener) {
        if (!mMessageAddedListeners.contains(listener)) {
            mMessageAddedListeners.add(listener);
        }
    }
    public void removeMessageAddedListener(MessageAddedListener listener) {
        mMessageAddedListeners.remove(listener);
    }
    public void fireMessageAddedListeners(ChatMessage msg) {
        for(MessageAddedListener listener : mMessageAddedListeners) {
            listener.onMessageAdded(msg);
        }
    }

    public void addMessageUpdatedListener(MessageUpdatedListener listener) {
        if (!mMessageUpdatedListeners.contains(listener)) {
            mMessageUpdatedListeners.add(listener);
        }
    }
    public void removeMessageUpdatedListener(MessageUpdatedListener listener) {
        mMessageUpdatedListeners.remove(listener);
    }
    private void fireMessageUpdatedListeners(ChatMessage msg) {
        for(MessageUpdatedListener listener : mMessageUpdatedListeners) {
            listener.onMessageUpdated(msg);
        }
    }

    /** Posts a message from us to the server. */
    public void postMessage(final ChatMessage msg) {
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
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
                    return true;
                } catch (Exception e) {
                    log.error("Error posting chat!", e);
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
            }
        }.execute();
    }

    /**
     * This is called by the conversation when a message is added an the empire needs to be refreshed.
     * @param empireKey
     */
    public void queueRefreshEmpire(String empireKey) {
        synchronized(mEmpiresToRefresh) {
            if (!mEmpiresToRefresh.contains(empireKey)) {
                mEmpiresToRefresh.add(empireKey);
                if (mEmpiresToRefresh.size() == 1) {
                    // first one, so schedule the function to actually fetch them
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshEmpires();
                        }
                    }, 100);
                }
            }
        }
    }

    public ChatConversation getConversation(ChatMessage msg) {
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
        return mConversations.get(0);
    }
    public ChatConversation getAllianceConversation() {
        return mConversations.get(-1);
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
                mConversations.append(conversationID, new ChatConversation(conversationID));
            }
        }
        return mConversations.get(conversationID);
    }

    /** Start a new conversation with the given empireID (can be null to start an empty conversation). */
    public void startConversation(final String empireID, final ConversationStartedListener handler) {
        // if we already have a conversation going with this guy, just reuse that one.
        if (empireID != null) {
            for (int index = 0; index < mConversations.size(); index++) {
                ChatConversation conversation = mConversations.valueAt(index);
                List<Integer> empireIDs = conversation.getEmpireIDs();
                if (empireIDs.size() != 2) {
                    continue;
                }
                if (empireIDs.get(0) == Integer.parseInt(empireID) ||
                    empireIDs.get(1) == Integer.parseInt(empireID)) {
                    handler.onConversationStarted(conversation);
                    return;
                }
            }
        }

        new BackgroundRunner<ChatConversation>() {
            @Override
            protected ChatConversation doInBackground() {
                try {
                    Messages.ChatConversation.Builder conversation_pb_builder = Messages.ChatConversation.newBuilder()
                            .addEmpireIds(Integer.parseInt(EmpireManager.i.getEmpire().getKey()));
                    if (empireID != null) {
                        conversation_pb_builder.addEmpireIds(Integer.parseInt(empireID));
                    }
                    Messages.ChatConversation conversation_pb = conversation_pb_builder.build();
                    conversation_pb = ApiClient.postProtoBuf("chat/conversations", conversation_pb, Messages.ChatConversation.class);

                    ChatConversation conversation = new ChatConversation(conversation_pb.getId());
                    conversation.fromProtocolBuffer(conversation_pb);
                    return conversation;
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onComplete(ChatConversation conversation) {
                if (conversation != null) {
                    mConversations.put(conversation.getID(), conversation);
                    handler.onConversationStarted(conversation);
                }
            }
        }.execute();
    }

    private void refreshEmpires() {
        List<String> empireKeys;
        synchronized(mEmpiresToRefresh) {
            empireKeys = new ArrayList<String>(mEmpiresToRefresh);
            mEmpiresToRefresh.clear();
        }

        EmpireManager.i.fetchEmpires(empireKeys,
                new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
                        for (int i = 0; i < mConversations.size(); i++) {
                            ChatConversation conversation = mConversations.valueAt(i);
                            conversation.onEmpireRefreshed(empire);
                        }
                    }
                });
    }

    @Override
    public void onBackgroundChange(Context context, boolean isInBackground) {
        if (!isInBackground) {
            requestMessages(mMostRecentMsg);
        }
    }

    private void refreshConversations() {
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
                // TODO
            }
        }.execute();
    }

    private void requestMessages(final DateTime since) {
        if (mRequesting) {
            return;
        }
        mRequesting = true;

        new BackgroundRunner<ArrayList<ChatMessage>>() {
            @Override
            protected ArrayList<ChatMessage> doInBackground() {
                ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>();

                try {
                    String url = "chat?since="+(since.getMillis()/1000);
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
                    // TODO: errors?
                }

                return msgs;
            }

            @Override
            protected void onComplete(ArrayList<ChatMessage> msgs) {
                for (ChatMessage msg : msgs) {
                    ChatConversation conversation = getConversation(msg);
                    conversation.addMessage(msg);
                }

                mRequesting = false;
            }
        }.execute();
    }

    public interface MessageAddedListener {
        void onMessageAdded(ChatMessage msg);
    }
    public interface MessageUpdatedListener {
        void onMessageUpdated(ChatMessage msg);
    }
    public interface ConversationStartedListener {
        void onConversationStarted(ChatConversation conversation);
    }
}
