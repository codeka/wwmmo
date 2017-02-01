package au.com.codeka.warworlds.client.game.world;

import com.google.common.collect.Lists;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.util.eventbus.EventHandler;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatMessagesPacket;
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
      for (ChatMessage msg : pkt.messages) {

      }
    }
  };
}
