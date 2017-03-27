package au.com.codeka.warworlds.server.html.account;

import com.google.common.base.Strings;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.html.HtmlRequestHandler;

/**
 * This servlet handles /accounts/verify, which is used to verify an email address.
 */
public class AccountVerifyHandler extends HtmlRequestHandler {
  private static Log log = new Log("AccountAssociateHandler");

  @Override
  public void get() throws RequestException {
    String emailVerificationCode = getRequest().getParameter("code");
    if (Strings.isNullOrEmpty(emailVerificationCode)) {
      render(getResponse(), "account/error-no-code.html", null);
    }
  }
}
