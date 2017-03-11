package au.com.codeka.warworlds.server.admin.handlers;

import java.util.List;

import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatMessagesPacket;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.world.chat.ChatManager;

/**
 * Handler for /admin/ajax/chat which lets us send messages from the special 'server' user.
 */
public class AjaxChatHandler extends AjaxHandler {
  @Override
  public void get() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "recv":
        Long roomId = null;
        if (getRequest().getParameter("roomId") != null) {
          roomId = Long.parseLong(getRequest().getParameter("roomId"));
        }
        long lastMsgTime = Long.parseLong(getRequest().getParameter("lastMsgTime"));

        handleRecvRequest(roomId, lastMsgTime);
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  @Override
  public void post() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "send":
        String msg = getRequest().getParameter("msg");
        handleSendRequest(msg);
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  private void handleSendRequest(String msg) {
    ChatManager.i.send(null, new ChatMessage.Builder()
        .action(ChatMessage.MessageAction.Normal)
        .message(msg)
        .build());
  }

  private void handleRecvRequest(Long roomId, long lastMsgId) {
    List<ChatMessage> messages =
        ChatManager.i.getMessages(roomId, lastMsgId, System.currentTimeMillis());
    if (messages.isEmpty()) {
      // TODO: wait for a message before returning...
    }

    setResponseJson(new ChatMessagesPacket.Builder()
        .messages(messages)
        .build());
  }
}
