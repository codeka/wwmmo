package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;

import android.content.Context;
import android.os.Handler;
import au.com.codeka.BackgroundRunner;
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
    private boolean mRequesting;
    private TreeSet<String> mEmpiresToRefresh;
    private Handler mHandler;

    private ChatManager() {
        mMessages = new LinkedList<ChatMessage>();
        mMessageAddedListeners = new ArrayList<MessageAddedListener>();
        mMessageUpdatedListeners = new ArrayList<MessageUpdatedListener>();
        mEmpiresToRefresh = new TreeSet<String>();
    }

    /**
     * Called when the game starts up, we need to register with the channel and get
     * ready to start receiving chat messages.
     */
    public void setup(Context context) {
        BackgroundDetector.getInstance().addBackgroundChangeHandler(this);

        mHandler = new Handler();
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
        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
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
            protected void onComplete(Boolean success) {
                if (success) {
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
            // make sure we don't have this chat already...
            for (ChatMessage existing : mMessages) {
                if (existing.getEmpireKey() != null &&
                    existing.getDatePosted().equals(msg.getDatePosted()) &&
                    existing.getEmpireKey().equals(msg.getEmpireKey())) {
                    return;
                }
            }
            while (mMessages.size() > MAX_CHAT_HISTORY) {
                mMessages.removeFirst();
            }
            mMessages.add(msg);

            if (msg.getDatePosted() != null) {
                if (msg.getDatePosted().compareTo(mMostRecentMsg) > 0) {
                    mMostRecentMsg = msg.getDatePosted();
                }
            }

            if (msg.getEmpire() == null && msg.getEmpireKey() != null && context != null) {
                synchronized(mEmpiresToRefresh) {
                    if (!mEmpiresToRefresh.contains(msg.getEmpireKey())) {
                        mEmpiresToRefresh.add(msg.getEmpireKey());
                        if (mEmpiresToRefresh.size() == 1) {
                            // first one, so schedule the function to actually fetch them
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    refreshEmpires(context);
                                }
                            }, 100);
                        }
                    }
                }
            }
        }
        fireMessageAddedListeners(msg);
    }

    private void refreshEmpires(final Context context) {
        List<String> empireKeys;
        synchronized(mEmpiresToRefresh) {
            empireKeys = new ArrayList<String>(mEmpiresToRefresh);
            mEmpiresToRefresh.clear();
        }

        EmpireManager.getInstance().fetchEmpires(context, empireKeys,
                new EmpireManager.EmpireFetchedHandler() {
                    @Override
                    public void onEmpireFetched(Empire empire) {
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
                });
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
                        ChatMessage msg = ChatMessage.fromProtocolBuffer(pb.getMessages(i));
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
                    addMessage(context, msg);
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
}
