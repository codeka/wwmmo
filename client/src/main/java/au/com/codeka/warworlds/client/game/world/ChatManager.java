package au.com.codeka.warworlds.client.game.world;

import com.google.common.collect.Lists;

import java.util.List;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
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

  /** Get the list of rooms that we're in. */
  public List<ChatRoom> getRooms() {
    // TODO: get all the rooms, not just the global one.
    return Lists.newArrayList(new ChatRoom.Builder()
        .id(null)
        .name("Global")
        .build());
  }

  public List<ChatMessage> getMessages(ChatRoom room, long startTime, long endTime) {
    // TODO: if we don't have any, ask some from the server.
    return App.i.getDataStore().chat().getMessages(room.id, startTime, endTime);
  }

  /** Send the given {@link ChatMessage} to the server. */
  public void sendMessage(ChatMessage msg) {
    App.i.getServer().send(new Packet.Builder()
        .chat_msgs(new ChatMessagesPacket.Builder()
            .messages(Lists.newArrayList(msg))
            .build())
        .build());
  }

  private final Object eventListener = new Object() {
    @EventHandler
    public void onChatMessagesPacket(ChatMessagesPacket pkt) {
      App.i.getDataStore().chat().addMessages(pkt.messages);
      App.i.getEventBus().publish(new ChatMessagesUpdatedEvent());
    }
  };
}
