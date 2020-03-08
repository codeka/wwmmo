package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.common.model.BaseChatConversationParticipant;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.ChatController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.BackendUser;
import au.com.codeka.warworlds.server.model.ChatConversation;
import au.com.codeka.warworlds.server.model.Empire;

public class AdminChatHandler extends AdminGenericHandler {
  @Override
  protected void get() throws RequestException {
    if (!isInRole(BackendUser.Role.ChatRead)) {
      render("admin/access-denied.html", null);
      return;
    }
    TreeMap<String, Object> data = new TreeMap<>();

    ArrayList<ChatConversation> conversations = new ChatController().getAllConversations();
    Map<Integer, Empire> empires = new HashMap<>();
    for (ChatConversation conv : conversations) {
      for (BaseChatConversationParticipant participant : conv.getParticipants()) {
        int empireId = participant.getEmpireID();
        if (empires.get(empireId) == null) {
          empires.put(empireId, new EmpireController().getEmpire(empireId));
        }
      }
    }
    data.put("conversations", conversations);
    data.put("empires", empires);

    render("admin/chat/messages.html", data);
  }
}
