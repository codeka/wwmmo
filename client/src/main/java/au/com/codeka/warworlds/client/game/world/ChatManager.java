package au.com.codeka.warworlds.client.game.world;

import com.google.common.collect.Lists;

import java.util.List;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.Time;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatMessagesPacket;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.common.proto.Packet;

/**
 * Manages the chat rooms we're in and so on.
 */
public class ChatManager {
  public static final ChatManager i = new ChatManager();

  private ChatManager() {
    App.i.getEventBus().register(eventListener);
  }

  public void create() {
    // TODO?
  }

  /**
   * Get the time of the most recent chat we have stored. When we connect to the server, we'll send
   * this value over so that it can update us on any chats that have happened since then.
   *
   * <p>So that we're not overwhelmed, we'll never return a value that's more than three days old.
   * If our most recent chat was more than three days ago, we'll need to clear out our cache and
   * rebuild it on-demand as you scroll back.
   */
  public long getLastChatTime() {
    long threeDaysAgo = System.currentTimeMillis() - (3 * Time.DAY);

    long lastChatTime = App.i.getDataStore().chat().getLastChatTime();
    if (lastChatTime == 0) {
      return threeDaysAgo;
    }

    if (lastChatTime < threeDaysAgo) {
      App.i.getDataStore().chat().removeHistory();
      return threeDaysAgo;
    }

    return lastChatTime;
  }

  /** Get the list of rooms that we're in. */
  public List<ChatRoom> getRooms() {
    // TODO: get all the rooms, not just the global one.
    return Lists.newArrayList(new ChatRoom.Builder()
        .id(null)
        .name("Global")
        .build());
  }

  /** Gets count messages starting from startTime and going back in time. */
  public List<ChatMessage> getMessages(ChatRoom room, long startTime, int count) {
    // TODO: if we don't have any, ask some from the server.
    return App.i.getDataStore().chat().getMessages(room.id, startTime, count);
  }

  /** Gets all messages newer than time. */
  public List<ChatMessage> getMessagesAfter(ChatRoom room, long time) {
    // TODO: if we don't have any, ask some from the server.
    return App.i.getDataStore().chat().getMessagesAfter(room.id, time);
  }

  /** Gets all messages, regardless of room, from the given start time. */
  public List<ChatMessage> getMessages(long startTime, int count) {
    return App.i.getDataStore().chat().getMessages(startTime, count);
  }

  /** Send the given {@link ChatMessage} to the server. */
  public void sendMessage(ChatMessage msg) {
    App.i.getTaskRunner().runTask(() -> {
      App.i.getServer().send(new Packet.Builder()
          .chat_msgs(new ChatMessagesPacket.Builder()
              .messages(Lists.newArrayList(msg))
              .build())
          .build());
    }, Threads.BACKGROUND);
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onChatMessagesPacket(ChatMessagesPacket pkt) {
      App.i.getDataStore().chat().addMessages(pkt.messages);
      App.i.getEventBus().publish(new ChatMessagesUpdatedEvent());
    }
  };
}
