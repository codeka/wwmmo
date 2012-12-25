package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.Util;

/**
 * This class keeps track of chats and what-not.
 */
public class ChatManager implements BackgroundDetector.BackgroundChangeHandler {
    private static ChatManager sInstance = new ChatManager();

    public static ChatManager getInstance() {
        return sInstance;
    }

    public static int MAX_CHAT_HISTORY = 1000;

    private LinkedList<ChatMessage> mMessages;
    private ArrayList<MessageAddedListener> mMessageAddedListeners;

    private ChatManager() {
        mMessages = new LinkedList<ChatMessage>();
        mMessageAddedListeners = new ArrayList<MessageAddedListener>();
    }

    /**
     * Called when the game starts up, we need to register with the channel and get
     * ready to start receiving chat messages.
     */
    public void setup() {
        BackgroundDetector.getInstance().addBackgroundChangeHandler(this);

        String chatEnabledProperty = Util.getProperties().getProperty("chat.enabled");
        if (chatEnabledProperty == null || !chatEnabledProperty.equals("false")) {
            addMessage(new ChatMessage("Welcome to War Worlds!"));
        } else {
            addMessage(new ChatMessage("Chat has been disabled."));
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
    }

    public ChatMessage getLastMessage() {
        ChatMessage[] msgs = getLastMessages(1);
        return msgs[0];
    }

    /**
     * Returns the last {c n} messages.
     */
    public ChatMessage[] getLastMessages(int n) {
        ChatMessage[] msgs = new ChatMessage[n];

        Iterator<ChatMessage> iter = mMessages.iterator();
        for(int i = 0; i < n; i++) {
            if (iter.hasNext()) {
                msgs[i] = iter.next();
            } else {
                break;
            }
        }

        return msgs;
    }

    /**
     * Adds a new message to the chat list.
     */
    public void addMessage(ChatMessage msg) {
        synchronized(mMessages) {
            while (mMessages.size() > MAX_CHAT_HISTORY) {
                mMessages.removeLast();
            }
            mMessages.addFirst(msg);
        }
        fireMessageAddedListeners(msg);
    }

    public int getNumMessages() {
        return mMessages.size();
    }

    /**
     * Returns the nth message, where 0 is the {i most recent} message.
     */
    public ChatMessage getMessage(int n) {
        synchronized(mMessages) {
            return mMessages.get(n);
        }
    }

    public interface MessageAddedListener {
        void onMessageAdded(ChatMessage msg);
    }

    @Override
    public void onBackgroundChange(boolean isInBackground) {
        // TODO Auto-generated method stub
        
    }
}
