package au.com.codeka.warworlds.server.cron.jobs;

import com.google.common.base.Strings;

import au.com.codeka.common.model.BaseChatMessage;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.model.ChatMessage;

/**
 * This is a cron job that simply sends a message to all players from [SERVER].
 */
@CronJob(
    name = "Send Message",
    desc = "Sends a message from '[Server]'. Useful for announcing upcoming blitz reset, etc.")
public class SendMessageCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    if (Strings.isNullOrEmpty(extra)) {
      return "~~ No message, skipped";
    }

    ChatMessage msg = new ChatMessage();
    msg.setAction(BaseChatMessage.MessageAction.Normal);
    msg.setMessage(extra);
    new ChatController().postMessage(msg);

    return "\"" + extra + "\"";
  }
}
