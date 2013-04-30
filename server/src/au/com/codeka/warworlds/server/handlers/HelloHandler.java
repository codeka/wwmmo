package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;

public class HelloHandler extends RequestHandler {

    @Override
    protected void put() throws RequestException {
        //Messages.HelloRequest pbHelloRequest = getRequestBody(Messages.HelloRequest.class);

        Messages.HelloResponse.Builder hello_response_pb = Messages.HelloResponse.newBuilder();

        // TODO: this could be cached...
        String motd = "";
        try (SqlStmt stmt = DB.prepare("SELECT motd FROM motd")) {
            motd = stmt.selectFirstValue(String.class);
        } catch (Exception e) {
            throw new RequestException(e);
        }
        hello_response_pb.setMotd(Messages.MessageOfTheDay.newBuilder()
                                          .setMessage(motd)
                                          .setLastUpdate(""));

        int empireID = getSession().getEmpireID();
        if (empireID > 0) {
            Empire empire = new EmpireController().getEmpire(empireID);
            if (empire != null) {
                Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
                empire.toProtocolBuffer(empire_pb);
                hello_response_pb.setEmpire(empire_pb);
            }
        }

        setResponseBody(hello_response_pb.build());
    }
}
