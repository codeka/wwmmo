package au.com.codeka.warworlds.server.handlers;

import com.google.common.base.Strings;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.bindings.MapBindings;
import au.com.codeka.carrot.resource.FileResourceLocator;
import au.com.codeka.carrot.util.SafeString;
import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.AdminController;
import au.com.codeka.warworlds.server.model.BackendUser;

/** Base class for handlers that want to render HTML with Carrot. */
public class RenderingHandler extends RequestHandler {
  private static final Log log = new Log("RenderingHandler");

  private static final CarrotEngine CARROT_ENGINE = new CarrotEngine(
      new au.com.codeka.carrot.Configuration.Builder()
          .setResourceLocator(new FileResourceLocator.Builder()
              .setBasePath(new File(Configuration.i.getDataDirectory(), "tmpl").getAbsolutePath()))
          .setEncoding("utf-8")
          .build(),
      new MapBindings.Builder()
          .set("Users", new UsersHelper())
          .set("Format", new FormatHelper()));

  /**
   * Render the template at the given path, with the given data.
   */
  protected void render(String path, Map<String, Object> data) throws RequestException {
    if (data == null) {
      data = new TreeMap<>();
    }

    data.put("realm", getRealm());
    Session session = getSessionNoError();
    if (session != null) {
      data.put("logged_in_user", session.getActualEmail());
      data.put("backend_user", new AdminController().getBackendUser(session.getActualEmail()));

      // If there's no admins, then everyone is an admin, so we'll want to warn about that.
      data.put("num_backend_users", new AdminController().getNumBackendUsers());
    }

    if (Strings.isNullOrEmpty(getResponse().getContentType())) {
      getResponse().setContentType("text/html");
      getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
    }
    try {
      getResponse().getWriter().write(CARROT_ENGINE.process(path, new MapBindings(data)));
    } catch (CarrotException | IOException e) {
      log.error("Error rendering template!", e);
    }
  }

  protected void write(String text) {
    getResponse().setContentType("text/plain");
    getResponse().setHeader("Content-Type", "text/plain; charset=utf-8");
    try {
      getResponse().getWriter().write(text);
    } catch (IOException e) {
      log.error("Error writing output!", e);
    }
  }

  private static class FormatHelper {
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0");

    public SafeString date(DateTime dt) {
      return new SafeString(String.format(Locale.ENGLISH, "<script>(function() {"
          + " var dt = new Date(\"%s\");" + " +document.write(dt.toLocaleString());"
          + "})();</script>", dt));
    }

    public String number(long n) {
      return NUMBER_FORMAT.format(n);
    }

    public String number(double d) {
      return NUMBER_FORMAT.format(d);
    }
  }


  private static class UsersHelper {
    public boolean isInRole(BackendUser user, String roleName) {
      BackendUser.Role role = BackendUser.Role.valueOf(roleName);
      return user.isInRole(role);
    }
  }}
