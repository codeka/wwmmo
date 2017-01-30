package au.com.codeka.warworlds.server.world.chat;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Manages chat rooms and stuff.
 */
public class ChatManager {
  private static final Log log = new Log("ChatManager");
  public static final ChatManager i = new ChatManager();

  private final Map<Long, WatchableObject<ChatRoom>> rooms = new HashMap<>();
  private final WatchableObject<ChatRoom> globalRoom;

  public ChatManager() {
    globalRoom = new WatchableObject<>(new ChatRoom.Builder().name("Global").build());
  }

  /**
   * Gets the {@link ChatRoom} with the given identifier.
   *
   * @param id the identifier of the room, or null for the global room.
   * @return the room, or null if no room with that ID exists.
   */
  @Nullable
  public WatchableObject<ChatRoom> getRoom(@Nullable Long id) {
    if (id == null) {
      return globalRoom;
    }

    synchronized (rooms) {
      WatchableObject<ChatRoom> room = rooms.get(id);
      if (room == null) {
        //TODO room = new WatchableObject<>(DataStore.i.chat().getRoom(id));
      }
      return room;
    }
  }

  /** "Send" the given message to the given room. */
  public void send(WatchableObject<ChatRoom> room, ChatMessage msg) {
    // TODO: validate the action, message_en, etc etc.

    msg = msg.newBuilder()
        .date_posted(System.currentTimeMillis())
        .id(DataStore.i.seq().nextIdentifier())
        .build();

    DataStore.i.chat().send(room.get(), msg);
  }
}
