package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.ChatMessage
import au.com.codeka.warworlds.common.proto.ChatMessagesPacket
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.world.chat.ChatManager

/**
 * Handler for /admin/ajax/chat which lets us send messages from the special 'server' user.
 */
class AjaxChatHandler : AjaxHandler() {
  @Throws(RequestException::class)
  public override fun get() {
    when (request.getParameter("action")) {
      "recv" -> {
        var roomId: Long? = null
        if (request.getParameter("roomId") != null) {
          roomId = request.getParameter("roomId").toLong()
        }
        val lastMsgTime = request.getParameter("lastMsgTime").toLong()
        handleRecvRequest(roomId, lastMsgTime)
      }
      else -> throw RequestException(400, "Unknown action: " + request.getParameter("action"))
    }
  }

  @Throws(RequestException::class)
  public override fun post() {
    when (request.getParameter("action")) {
      "send" -> {
        val msg = request.getParameter("msg")
        handleSendRequest(msg)
      }
      else -> throw RequestException(400, "Unknown action: " + request.getParameter("action"))
    }
  }

  private fun handleSendRequest(msg: String) {
    ChatManager.Companion.i.send(null, ChatMessage.Builder()
        .action(ChatMessage.MessageAction.Normal)
        .message(msg)
        .build())
  }

  private fun handleRecvRequest(roomId: Long?, lastMsgId: Long) {
    val messages = ChatManager.i.getMessages(roomId, lastMsgId, System.currentTimeMillis())
    if (messages.isEmpty()) {
      // TODO: wait for a message before returning...
    }
    setResponseJson(ChatMessagesPacket.Builder()
        .messages(messages)
        .build())
  }
}