package au.com.codeka.warworlds.server.world.chat

import au.com.codeka.warworlds.common.proto.ChatMessage
import com.google.common.collect.Lists

/**
 * A [Participant] represents the empire in a chat room. In the case of the global chat room,
 * this will be transient for as long as the empire is connected to the server. In the case of
 * 1-on-1 chats, this'll be permanent so that we can send them notifications.
 */
class Participant(private val empireId: Long) {
  /** This interface is given to us when the player is online.  */
  interface OnlineCallback {
    /** Called when [ChatMessage]s is sent to a room we're in.  */
    fun onChatMessage(msgs: List<ChatMessage>)
  }

  /**
   * The [OnlineCallback] when the player is online. This will be null if the player is
   * currently offline.
   */
  private var callback: OnlineCallback? = null

  fun setOnlineCallback(callback: OnlineCallback?) {
    this.callback = callback
  }

  fun onMessage(msg: ChatMessage) {
    if (callback != null) {
      callback!!.onChatMessage(Lists.newArrayList(msg))
    }
  }

}