package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatRoom;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.world.chat.ChatManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Handler for /admin/ajax/chat which lets us send messages from the special 'server' user.
 */
public class AjaxChatHandler extends AjaxHandler {
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
    WatchableObject<ChatRoom> room = ChatManager.i.getRoom(null);
    ChatManager.i.send(room, new ChatMessage.Builder()
        .action(ChatMessage.MessageAction.Normal)
        .message(msg)
        .build());
  }
}
