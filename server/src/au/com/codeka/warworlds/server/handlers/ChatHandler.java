package au.com.codeka.warworlds.server.handlers;

import org.joda.time.DateTime;

import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.ChatMessage;

/**
 * Handles the /realms/.../chat URL.
 */
public class ChatHandler extends RequestHandler {

  @Override
  protected void get() throws RequestException {
    DateTime after = DateTime.now().minusDays(14);
    if (getRequest().getParameter("since") != null) { // note: synonym for 'after'
      long epoch = Long.parseLong(getRequest().getParameter("since")) + 1;
      after = new DateTime(epoch * 1000);
    }
    if (getRequest().getParameter("after") != null) {
      long epoch = Long.parseLong(getRequest().getParameter("after")) + 1;
      after = new DateTime(epoch * 1000);
    }

    DateTime before = DateTime.now().plusHours(1);
    if (getRequest().getParameter("before") != null) {
      long epoch = Long.parseLong(getRequest().getParameter("before")) - 1;
      before = new DateTime(epoch * 1000);
    }

    DateTime minDate = DateTime.now().minusDays(14);
    if (after.isBefore(minDate)) {
      after = minDate;
    }
    if (before.isBefore(minDate)) {
      before = minDate;
    }

    Integer conversationID = null;
    if (getRequest().getParameter("conversation") != null) {
      conversationID = Integer.parseInt(getRequest().getParameter("conversation"));
    }

    List<ChatMessage> msgs;
    if (conversationID != null && conversationID > 0) {
      msgs = new ChatController()
          .getMessagesForConversation(conversationID, getSession().getEmpireID(), after, before);
    } else if (conversationID != null && conversationID < 0) {
      msgs = new ChatController()
          .getAllianceMessages(
              getSession().getAllianceID(), getSession().getEmpireID(), after, before);
    } else if (conversationID != null /* && conversationID == 0 */) {
      // Explicitly asking for global chat
      msgs = new ChatController().getGlobalMessages(getSession().getEmpireID(), after, before);
    } else /* if (conversationID == null */ {
      // If it's an admin, return all messages in all conversations
      if (getSession().isAdmin()) {
        msgs = new ChatController().getAllMessages(getSession().getEmpireID(), after, before);
      } else {
        msgs = new ChatController().getGlobalMessages(getSession().getEmpireID(), after, before);
      }
    }

    Messages.ChatMessages.Builder chat_msgs_pb = Messages.ChatMessages.newBuilder();
    for (ChatMessage msg : msgs) {
      Messages.ChatMessage.Builder chat_msg_pb = Messages.ChatMessage.newBuilder();
      msg.toProtocolBuffer(chat_msg_pb, true);
      chat_msgs_pb.addMessages(chat_msg_pb);
    }

    setResponseBody(chat_msgs_pb.build());
  }

  @Override
  protected void post() throws RequestException {
    Messages.ChatMessage chat_msg_pb = getRequestBody(Messages.ChatMessage.class);

    if (chat_msg_pb.hasAllianceKey() && chat_msg_pb.getAllianceKey().length() > 0) {
      // confirm that if they've specified an alliance, that it's actually their
      // own alliance...
      int allianceID = Integer.parseInt(chat_msg_pb.getAllianceKey());
      if (allianceID != getSession().getAllianceID()) {
        throw new RequestException(400);
      }
    }

    // if it's not admin, add the right empire ID
    if (!getSession().isAdmin()) {
      chat_msg_pb = Messages.ChatMessage.newBuilder(chat_msg_pb)
          .setEmpireKey(Integer.toString(getSession().getEmpireID()))
          .build();
    }

    ChatMessage msg = new ChatMessage();
    msg.fromProtocolBuffer(chat_msg_pb);
    new ChatController().postMessage(msg);

    Messages.ChatMessage.Builder chat_msg_builder = Messages.ChatMessage.newBuilder();
    msg.toProtocolBuffer(chat_msg_builder, true);
    setResponseBody(chat_msg_builder.build());
  }
}
