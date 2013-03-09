package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;

import android.content.Context;
import android.os.AsyncTask;
import au.com.codeka.warworlds.BackgroundDetector;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.model.protobuf.Messages;

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
    private ArrayList<MessageUpdatedListener> mMessageUpdatedListeners;
    private DateTime mMostRecentMsg;

    private ChatManager() {
        mMessages = new LinkedList<ChatMessage>();
        mMessageAddedListeners = new ArrayList<MessageAddedListener>();
        mMessageUpdatedListeners = new ArrayList<MessageUpdatedListener>();
    }

    /**
     * Called when the game starts up, we need to register with the channel and get
     * ready to start receiving chat messages.
     */
    public void setup(Context context) {
        BackgroundDetector.getInstance().addBackgroundChangeHandler(this);

        addMessage(context, new ChatMessage("Welcome to War Worlds!"));

        // fetch all chats from the last 24 hours
        mMostRecentMsg = (new DateTime()).minusDays(1);
        requestMessages(context, mMostRecentMsg);
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

    /**
     * Posts a message from us to the server.
     */
    public void postMessage(final Context context, final ChatMessage msg) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... arg0) {
                try {
                    Messages.ChatMessage pb = Messages.ChatMessage.newBuilder()
                            .setMessage(msg.getMessage())
                            .build();
                    ApiClient.postProtoBuf("chat", pb);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                   // msg.
                    addMessage(context, msg);
                }
            }
        }.execute();
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

    /**
     * Adds a new message to the chat list.
     */
    public void addMessage(final Context context, final ChatMessage msg) {
        synchronized(mMessages) {
            while (mMessages.size() > MAX_CHAT_HISTORY) {
                mMessages.removeFirst();
            }
            mMessages.add(msg);

            if (msg.getDatePosted() != null) {
                mMostRecentMsg = msg.getDatePosted();
            }

            if (msg.getEmpire() == null && msg.getEmpireKey() != null && context != null) {
                EmpireManager.getInstance().fetchEmpire(context, msg.getEmpireKey(),
                        new EmpireManager.EmpireFetchedHandler() {
                            @Override
                            public void onEmpireFetched(Empire empire) {
                                msg.setEmpire(empire);
                                fireMessageUpdatedListeners(msg);
                            }
                        });
            }
        }
        fireMessageAddedListeners(msg);
    }

    public int getNumMessages() {
        return mMessages.size();
    }

    @Override
    public void onBackgroundChange(Context context, boolean isInBackground) {
        if (!isInBackground) {
            requestMessages(context, mMostRecentMsg);
        }
    }

    private void requestMessages(final Context context, final DateTime since) {
        new AsyncTask<Void, Void, ArrayList<ChatMessage>>() {
            @Override
            protected ArrayList<ChatMessage> doInBackground(Void... arg0) {
                ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>();

                try {
                    String url = "chat?since="+(since.getMillis()/1000);
                    Messages.ChatMessages pb = ApiClient.getProtoBuf(url,
                            Messages.ChatMessages.class);

                    // this comes back most recent first, but we work in the
                    // opposite order...
                    for (int i = pb.getMessagesCount() - 1; i >= 0; i--) {
                        ChatMessage msg = ChatMessage.fromProtocolBuffer(pb.getMessages(i));
                        msgs.add(msg);
                    }
                } catch (Exception e) {
                    // TODO: errors?
                }

                return msgs;
            }

            @Override
            protected void onPostExecute(ArrayList<ChatMessage> msgs) {
                for (ChatMessage msg : msgs) {
                    addMessage(context, msg);
                }
            }
        }.execute();
    }

    public interface MessageAddedListener {
        void onMessageAdded(ChatMessage msg);
    }
    public interface MessageUpdatedListener {
        void onMessageUpdated(ChatMessage msg);
    }
}
