package au.com.codeka.warworlds.server.world.chat;

import java.util.HashMap;
import java.util.List;
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

  private final Map<Long, Room> rooms = new HashMap<>();
  private final Room globalRoom;

  public ChatManager() {
    globalRoom = new Room(new ChatRoom.Builder().name("Global").build());
  }

  /** "Send" the given message to the given room. */
  public void send(@Nullable Long roomId, ChatMessage msg) {
    Room room = getRoom(roomId);
    if (room == null) {
      log.error("No room with id %d", roomId);
      return;
    }

    // TODO: validate the action, message_en, etc etc.

    msg = msg.newBuilder()
        .date_posted(System.currentTimeMillis())
        .id(DataStore.i.seq().nextIdentifier())
        .build();

    room.send(msg);
  }

  /** Get the history of all messages in the given room, between the given start and end time. */
  public List<ChatMessage> getMessages(@Nullable Long roomId, long startTime, long endTime) {
    Room room = getRoom(roomId);
    return room.getMessages(startTime, endTime);
  }

  /**
   * Gets the {@link ChatRoom} with the given identifier.
   *
   * @param id the identifier of the room, or null for the global room.
   * @return the room, or null if no room with that ID exists.
   */
  @Nullable
  private Room getRoom(@Nullable Long id) {
    if (id == null) {
      return globalRoom;
    }

    synchronized (rooms) {
      Room room = rooms.get(id);
      if (room == null) {
        //TODO room = new WatchableObject<>(DataStore.i.chat().getRoom(id));
      }
      return room;
    }
  }
}
