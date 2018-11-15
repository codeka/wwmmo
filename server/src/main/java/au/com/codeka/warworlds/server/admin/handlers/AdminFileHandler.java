package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.server.admin.Session;
import au.com.codeka.warworlds.server.handlers.FileHandler;
import au.com.codeka.warworlds.server.handlers.RequestException;

/** Simple handler for handling static files (and 'templated' HTML files with no templated data). */
public class AdminFileHandler extends AdminHandler {
  private final Log log = new Log("AdminGenericHandler");

  private final FileHandler fileHandler;

  public AdminFileHandler() {
    fileHandler = new FileHandler("data/admin/static/");
  }

  /**
   * Gets a collection of roles, one of which the current user must be in to access this handler.
   */
  protected Collection<AdminRole> getRequiredRoles() {
    return Lists.newArrayList();
  }

  @Override
  public void setup(
      Matcher routeMatcher,
      String extraOption,
      Session session,
      HttpServletRequest request,
      HttpServletResponse response) {
    super.setup(routeMatcher, extraOption, session, request, response);
    fileHandler.setup(routeMatcher, extraOption, request, response);
  }

  @Override
  public void handle() throws RequestException {
    fileHandler.handle();
  }
}
