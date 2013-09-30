package au.com.codeka.warworlds.model;

/**
 * This is a special {@see ChatConversation} that holds the last 10 messages
 * from all conversations, to display in the mini chat view.
 */
public class ChatRecentConversation extends ChatConversation
                                    implements ChatManager.MessageAddedListener {
    public static final int MAX_MESSAGES = 10;

    public ChatRecentConversation(int id) {
        super(id);

        // make sure we get notified of all new messages, too.
        ChatManager.i.addMessageAddedListener(this);
    }

    /** No older messages for the 'recent' conversation. */
    @Override
    public void fetchOlderMessages(final ChatManager.MessagesFetchedListener handler) {
    }

    @Override
    public void addMessage(int index, ChatMessage msg, boolean fireListeners) {
        synchronized(mMessages) {
            while (mMessages.size() >= MAX_MESSAGES) {
                mMessages.removeFirst();
            }
        }

        super.addMessage(index, msg, fireListeners);
    }

    @Override
    public void onMessageAdded(ChatMessage msg) {
        addMessage(mMessages.size(), msg, true);
    }
}
