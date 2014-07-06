package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseChatConversation;
import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.GlobalOptions;
import au.com.codeka.warworlds.RealmContext;

public class ChatConversation extends BaseChatConversation {
    private static final Log log = new Log("ChatConversation");
    protected LinkedList<ChatMessage> mMessages = new LinkedList<ChatMessage>();
    private DateTime mMostRecentMsg;
    private boolean mNeedUpdate;

    public ChatConversation(int id) {
        mID = id;
        mNeedUpdate = true;
        if (id > 0) {
            mParticipants = new ArrayList<BaseChatConversationParticipant>();
        }
    }

    public void update(ChatConversation conversation) {
        if (conversation.getParticipants() != null) {
            mParticipants = new ArrayList<BaseChatConversationParticipant>(conversation.getParticipants());
        }
        mNeedUpdate = false;
    }

    public ChatMessage getLastMessage() {
        return getMessage(mMessages.size() - 1);
    }

    /** Returns the nth message, where 0 is the {i most recent} message. */
    public ChatMessage getMessage(int n) {
        synchronized(mMessages) {
            return mMessages.get(mMessages.size() - n - 1);
        }
    }

    public boolean isMuted() {
        if (mID <= 0) {
            return false;
        }
        return new GlobalOptions().isConversationMuted(mID);
    }

    /**
     * If this returned true, it means an empty conversation was created and it needs to
     * be updated from the server to add the participants and so on.
     */
    public boolean needUpdate() {
        return mNeedUpdate;
    }

    /** Returns the last {c n} messages. */
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

    /** Fetches from the server another page of older messages. */
    public void fetchOlderMessages(final ChatManager.MessagesFetchedListener handler) {
        DateTime before = DateTime.now();
        if (mMessages.size() > 0) {
            before = mMessages.get(0).getDatePosted();
        }
        DateTime after = before.minusDays(7);

        ChatManager.i.requestMessages(after, before, 100, getID(),
                new ChatManager.MessagesFetchedListener() {
            @Override
            public void onMessagesFetched(List<ChatMessage> msgs) {
                for (int i = msgs.size() - 1; i >= 0; i--) {
                    addMessage(0, msgs.get(i));
                }

                handler.onMessagesFetched(msgs);
            }
        });
    }

    public List<ChatMessage> getAllMessages() {
        return mMessages;
    }

    public int getNumMessages() {
        return mMessages.size();
    }

    /** Adds a new message to the chat list. */
    public void addMessage(ChatMessage msg, boolean blah) {
        addMessage(mMessages.size(), msg);
    }

    /** Adds a new message to the chat list at the given index. */
    public void addMessage(int index, ChatMessage msg) {
        synchronized(mMessages) {
            // make sure we don't have this chat already...
            for (ChatMessage existing : mMessages) {
                if (existing.getID() == msg.getID()) {
                    return;
                }
            }

            // also make sure it's actually for us!
            if (msg.getConversationID() == null) {
                if (msg.getAllianceKey() == null) {
                    if (mID != ChatManager.GLOBAL_CONVERSATION_ID) {
                        return;
                    }
                } else {
                    if (mID != ChatManager.ALLIANCE_CONVERSATION_ID) {
                        return;
                    }
                }
            } else if (msg.getConversationID() != mID) {
                return;
            }

            mMessages.add(index, msg);

            if (mMostRecentMsg == null) {
                mMostRecentMsg = msg.getDatePosted();
            } else if (msg.getDatePosted() != null) {
                if (msg.getDatePosted().isAfter(mMostRecentMsg)) {
                    mMostRecentMsg = msg.getDatePosted();
                }
            }
        }
    }

    public int getUnreadCount() {
        if (mID <= 0) {
            return 0;
        }

        DateTime lastReadCount = new ChatStore().getLastReadDate(mID);
        if (lastReadCount == null) {
            return mMessages.size();
        }

        int numUnread = 0;
        for (ChatMessage msg : mMessages) {
            if (msg.getDatePosted().isAfter(lastReadCount)) {
                numUnread ++;
            }
        }
        return numUnread;
    }

    public void markAllRead() {
        new ChatStore().setLastReadDate(mID, mMostRecentMsg);
        ChatManager.eventBus.publish(new ChatManager.UnreadMessageCountUpdatedEvent(0));
    }

    @Override
    public void fromProtocolBuffer(Messages.ChatConversation pb) {
        super.fromProtocolBuffer(pb);
        mNeedUpdate = false;
    }

    @Override
    protected BaseChatConversationParticipant createChatConversationParticipant(
            Messages.ChatConversationParticipant pb) {
        ChatConversationParticipant participant = new ChatConversationParticipant();
        if (pb != null) {
            participant.fromProtocolBuffer(pb);
        }
        return participant;
    }

    private static class ChatStore extends SQLiteOpenHelper {
        private static Object sLock = new Object();

        public ChatStore() {
            super(App.i, "chat.db", null, 1);
        }

        /**
         * This is called the first time we open the database, in order to create the required
         * tables, etc.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE read_counter ("
                      +"  conversation_id INTEGER PRIMARY KEY,"
                      +"  realm_id INTEGER,"
                      +"  last_read_date INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        public void setLastReadDate(int conversationID, DateTime dt) {
            if (conversationID <= 0) {
                return;
            }

            synchronized(sLock) {
                SQLiteDatabase db = getWritableDatabase();
                try {
                    // delete any old cached values first
                    db.delete("read_counter", getWhereClause(conversationID), null);

                    // insert a new cached value
                    ContentValues values = new ContentValues();
                    values.put("conversation_id", conversationID);
                    values.put("realm_id", RealmContext.i.getCurrentRealm().getID());
                    values.put("last_read_date", dt.getMillis());
                    db.insert("read_counter", null, values);
                } catch(Exception e) {
                    // ignore errors... todo: log them
                } finally {
                    db.close();
                }
            }
        }

        public DateTime getLastReadDate(int conversationID) {
            if (conversationID <= 0) {
                return DateTime.now();
            }

            synchronized(sLock) {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = null;
                try {
                    cursor = db.query("read_counter", new String[] {"conversation_id", "last_read_date"},
                            getWhereClause(conversationID),
                            null, null, null, null);
                    if (!cursor.moveToFirst()) {
                        cursor.close();
                        return null;
                    }

                    // if it's too old, we'll want to refresh it anyway from the server
                    long epoch = cursor.getLong(1);
                    return new DateTime(epoch, DateTimeZone.UTC);
                } catch (Exception e) {
                    // todo: log errors
                    return null;
                } finally {
                    if (cursor != null) cursor.close();
                    db.close();
                }
            }
        }

        private String getWhereClause(int conversationID) {
            return "conversation_id = '"+conversationID+"' AND realm_id="+RealmContext.i.getCurrentRealm().getID();
        }
    }
}
