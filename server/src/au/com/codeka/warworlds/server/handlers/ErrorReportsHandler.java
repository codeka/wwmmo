package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.ErrorReports;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.ErrorReportsController;

/** This handler is where error reports from the client are posted. */
public class ErrorReportsHandler extends RequestHandler {
  @Override
  public void post() throws RequestException {
    ErrorReports error_reports_pb = getRequestBody(ErrorReports.class);
    new ErrorReportsController().saveErrorReports(error_reports_pb);
  }
}
