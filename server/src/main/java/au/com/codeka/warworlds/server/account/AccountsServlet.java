package au.com.codeka.warworlds.server.account;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.NewAccountRequest;

/** Accounts servlet for creating new accounts on the server. */
public class AccountsServlet extends HttpServlet {
  private final Log log = new Log("AccountsServlet");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    NewAccountRequest req = NewAccountRequest.ADAPTER.decode(request.getInputStream());
    // TODO: do it!
    log.debug("Creating new account: %s", req.empire_name);
  }
}
