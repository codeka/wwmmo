package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseChatConversation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.model.ChatManager.MessageAddedListener;
import au.com.codeka.warworlds.model.ChatManager.MessageUpdatedListener;

public class ChatConversation extends BaseChatConversation {
    public static int MAX_CHAT_HISTORY = 1000;

    private LinkedList<ChatMessage> mMessages = new LinkedList<ChatMessage>();
    private ArrayList<MessageAddedListener> mMessageAddedListeners = new ArrayList<MessageAddedListener>();
    private ArrayList<MessageUpdatedListener> mMessageUpdatedListeners = new ArrayList<MessageUpdatedListener>();
    private DateTime mMostRecentMsg;
    private boolean mNeedUpdate;

    public ChatConversation(int id) {
        mID = id;
        mNeedUpdate = true;
        if (id > 0) {
            mEmpireIDs = new ArrayList<Integer>();
        }
    }

    public void addMessageAddedListener(MessageAddedListener listener) {
        if (!mMessageAddedListeners.contains(listener)) {
            mMessageAddedListeners.add(listener);
        }
    }
    public void removeMessageAddedListener(MessageAddedListener listener) {
        mMessageAddedListeners.remove(listener);
    }
    private void fireMessageAddedListeners(ChatMessage msg) {
        for(MessageAddedListener listener : mMessageAddedListeners) {
            listener.onMessageAdded(msg);
        }
        ChatManager.i.fireMessageAddedListeners(msg);
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

    public void update(ChatConversation conversation) {
        if (conversation.getEmpireIDs() != null) {
            mEmpireIDs = new ArrayList<Integer>(conversation.getEmpireIDs());
        }
        mNeedUpdate = false;
    }

    public ChatMessage getLastMessage() {
        return getMessage(mMessages.size() - 1);
    }

    /**
     * Returns the nth message, where 0 is the {i most recent} message.
     */
    public ChatMessage getMessage(int n) {
        synchronized(mMessages) {
            return mMessages.get(mMessages.size() - n - 1);
        }
    }

    /**
     * If this returned true, it means an empty conversation was created and it needs to
     * be updated from the server to add the participants and so on.
     */
    public boolean needUpdate() {
        return mNeedUpdate;
    }

    /**
     * Returns the last {c n} messages.
     */
    public List<ChatMessage> getLastMessages(int n) {
        ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>();

        int startIndex = mMessages.size() - 1 - n;
        if (startIndex < 0) {
            startIndex = 0;
        }

        for(int i = startIndex; i < mMessages.size(); i++) {
            msgs.add(mMessages.get(i));
        }

        return msgs;
    }

    public List<ChatMessage> getAllMessages() {
        return mMessages;
    }

    public int getNumMessages() {
        return mMessages.size();
    }

    /**
     * Adds a new message to the chat list.
     */
    public void addMessage(final ChatMessage msg) {
        synchronized(mMessages) {
            // make sure we don't have this chat already...
            for (ChatMessage existing : mMessages) {
                if (existing.getID() == msg.getID()) {
                    return;
                }
            }
            while (mMessages.size() > MAX_CHAT_HISTORY) {
                mMessages.removeFirst();
            }
            mMessages.add(msg);

            if (mMostRecentMsg == null) {
                mMostRecentMsg = msg.getDatePosted();
            } else if (msg.getDatePosted() != null) {
                if (msg.getDatePosted().compareTo(mMostRecentMsg) > 0) {
                    mMostRecentMsg = msg.getDatePosted();
                }
            }

            if (msg.getEmpire() == null && msg.getEmpireKey() != null) {
                Empire emp = EmpireManager.i.getEmpire(msg.getEmpireKey());
                if (emp != null) {
                    msg.setEmpire(emp);
                } else {
                    ChatManager.i.queueRefreshEmpire(msg.getEmpireKey());
                }
            }
        }
        fireMessageAddedListeners(msg);
    }

    public void onEmpireRefreshed(Empire empire) {
        for (ChatMessage msg : mMessages) {
            if (msg.getEmpireKey() == null) {
                continue;
            }
            if (msg.getEmpireKey().equals(empire.getKey())) {
                msg.setEmpire(empire);
                fireMessageUpdatedListeners(msg);
            }
        }
    }

    @Override
    public void fromProtocolBuffer(Messages.ChatConversation pb) {
        super.fromProtocolBuffer(pb);
        mNeedUpdate = false;
    }
}
