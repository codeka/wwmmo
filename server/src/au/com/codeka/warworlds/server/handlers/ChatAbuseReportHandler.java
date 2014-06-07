package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ChatAbuseController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.ChatMessage;
import au.com.codeka.warworlds.server.model.Empire;

/**
 * Handles the /realms/.../chat/<id>/abuse-reports URL.
 */
public class ChatAbuseReportHandler extends RequestHandler {
    private static boolean sAbuseEnabled = false;

    static {
        String s = System.getProperty("au.com.codeka.warworlds.server.abuseEnabled");
        if (s != null) {
            sAbuseEnabled = Boolean.parseBoolean(s);
        }
    }

    @Override
    protected void post() throws RequestException {
        if (!sAbuseEnabled) {
            throw new RequestException(400, "This feature has been disabled.");
        }

        Messages.ChatAbuseReport chat_abuse_report_pb = getRequestBody(Messages.ChatAbuseReport.class);

        int chatMsgID = Integer.parseInt(getUrlParameter("msg_id"));
        if (chatMsgID != chat_abuse_report_pb.getChatMsgId()) {
            throw new RequestException(400);
        }

        ChatMessage msg;

        String sql = "SELECT * FROM chat_messages WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, chatMsgID);
            SqlResult res = stmt.select();
            if (!res.next()) {
                throw new RequestException(404);
            }

            msg = new ChatMessage(res);
        } catch (Exception e) {
            throw new RequestException(e);
        }

        Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());

        new ChatAbuseController().reportAbuse(msg, empire);
    }

}
