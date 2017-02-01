package au.com.codeka.warworlds.server.world.chat;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.ChatMessage;

/**
 * A {@link Participant} represents the empire in a chat room. In the case of the global chat room,
 * this will be transient for as long as the empire is connected to the server. In the case of
 * 1-on-1 chats, this'll be permanent so that we can send them notifications.
 */
public class Participant {
  /** This interface is given to us when the player is online. */
  public interface OnlineCallback {
    /** Called when a {@link ChatMessage} is sent to a room we're in. */
    void onChatMessage(ChatMessage msg);
  }

  private final long empireId;

  /**
   * The {@link OnlineCallback} when the player is online. This will be null if the player is
   * currently offline.
   */
  @Nullable private OnlineCallback callback;

  public Participant(long empireId) {
    this.empireId = empireId;
  }

  public void setOnlineCallback(@Nullable OnlineCallback callback) {
    this.callback = callback;
  }

  public void onMessage(ChatMessage msg) {
    if (callback != null) {
      callback.onChatMessage(msg);
    }
  }
}
