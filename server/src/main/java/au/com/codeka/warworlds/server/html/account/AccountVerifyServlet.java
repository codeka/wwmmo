package au.com.codeka.warworlds.server.html.account;

import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.resource.FileResourceLocater;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.server.ProtobufHttpServlet;

/**
 * This servlet handles /accounts/verify, which is used to verify an email address.
 */
public class AccountVerifyServlet extends ProtobufHttpServlet {
  private static Log log = new Log("AccountAssociateServlet");

  private static final CarrotEngine CARROT_ENGINE = new CarrotEngine();
  static {
    CARROT_ENGINE.getConfig().setResourceLocater(
        new FileResourceLocater(
            CARROT_ENGINE.getConfig(),
            new File("data/html/tmpl").getAbsolutePath()));
    CARROT_ENGINE.getConfig().setEncoding("utf-8");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String emailVerificationCode = request.getParameter("code");
    if (Strings.isNullOrEmpty(emailVerificationCode)) {
      render(response, "account/error-no-code.html", null);
    }
  }

  private void render(
      HttpServletResponse response,
      String tmplName,
      @Nullable Map<String, Object> data) throws ServletException, IOException {
    response.setContentType("text/html");
    response.setHeader("Content-Type", "text/html; charset=utf-8");
    try {
      response.getWriter().write(CARROT_ENGINE.process(tmplName, data));
    } catch (CarrotException e) {
      throw new ServletException(e);
    }
  }
}
