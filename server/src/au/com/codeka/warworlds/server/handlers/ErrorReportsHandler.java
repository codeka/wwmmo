package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ErrorReportsController;

/**
 * This handler is where error reports from the client are posted.
 */
public class ErrorReportsHandler extends RequestHandler {
  @Override
  public void post() throws RequestException {
    Messages.ErrorReports.Builder error_reports_pb =
        getRequestBody(Messages.ErrorReports.class).toBuilder();

    // Make sure it has the correct source set.
    for (int i = 0; i < error_reports_pb.getReportsCount(); i++) {
        error_reports_pb.setReports(
            i,
            error_reports_pb.getReports(i).toBuilder()
                .setSource(Messages.ErrorReport.Source.CLIENT));
    }

    new ErrorReportsController().saveErrorReports(error_reports_pb.build());
  }
}
