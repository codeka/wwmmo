package au.com.codeka.warworlds.server.handlers;

import org.joda.time.DateTime;

import java.util.List;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.model.ChatBlock;

/**
 * Handles requests for /chat/blocks, blocking empires from chat.
 */
public class ChatBlocksHandler extends RequestHandler {
  private static final Log log = new Log("ChatBlocksHandler");

  @Override
  public void post() throws RequestException {
    Messages.ChatBlockRequest chat_block_request = getRequestBody(Messages.ChatBlockRequest.class);

    Session session = getSession();
    chat_block_request = chat_block_request.toBuilder()
        .setEmpireId(session.getEmpireID())
        .setBlockTime(new DateTime().getMillis())
        .build();

    if (chat_block_request.getBlockedEmpireId() == chat_block_request.getEmpireId()) {
      throw new RequestException(400, "You can't block yourself.");
    }

    List<ChatBlock> exitingBlocks =
        new ChatController().getBlocksForEmpire(chat_block_request.getEmpireId());
    for (ChatBlock existingBlock : exitingBlocks) {
      if (existingBlock.getBlockedEmpireID() == chat_block_request.getBlockedEmpireId()) {
        log.info("Empire is already blocked, nothing to do.");
        return;
      }
    }

    ChatBlock block = new ChatBlock();
    block.fromProtocolBuffer(chat_block_request);
    new ChatController().addBlock(block);
  }
}
