package au.com.codeka.warworlds.server.admin.handlers;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.carrot.base.CarrotException;
import au.com.codeka.carrot.resource.FileResourceLocater;
import au.com.codeka.carrot.interpret.CarrotInterpreter;
import au.com.codeka.carrot.interpret.InterpretException;
import au.com.codeka.carrot.lib.Filter;
import au.com.codeka.carrot.template.TemplateEngine;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.admin.RequestHandler;
import au.com.codeka.warworlds.server.admin.Session;
import au.com.codeka.warworlds.server.store.DataStore;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;

public class AdminHandler extends RequestHandler {
  private final Log log = new Log("AdminHandler");

  private static final TemplateEngine TEMPLATE_ENGINE;
  static {
    TEMPLATE_ENGINE = new TemplateEngine();

    TEMPLATE_ENGINE.getConfiguration().setResourceLocater(
        new FileResourceLocater(
            TEMPLATE_ENGINE.getConfiguration(),
            new File("data/admin/tmpl").getAbsolutePath()));
    TEMPLATE_ENGINE.getConfiguration().setEncoding("utf-8");

    TEMPLATE_ENGINE.getConfiguration().getFilterLibrary().register(new NumberFilter());
    TEMPLATE_ENGINE.getConfiguration().getFilterLibrary().register(new AttrEscapeFilter());
    TEMPLATE_ENGINE.getConfiguration().getFilterLibrary().register(new LocalDateFilter());
    TEMPLATE_ENGINE.getConfiguration().getFilterLibrary().register(new IsInRoleFilter());
  }

  @Override
  public boolean onBeforeHandle() {
    Collection<AdminRole> requiredRoles = getRequiredRoles();
    if (requiredRoles != null) {
      Session session = getSessionNoError();
      if (session == null) {
        authenticate();
        return false;
      } else {
        boolean inRoles = false;
        for (AdminRole role : requiredRoles) {
          inRoles = inRoles || session.isInRole(role);
        }
        if (!inRoles) {
          // you're not in a required role.
          redirect("/admin");
          return false;
        }
      }
    }

    return true;
  }

  @Override
  protected void handleException(RequestException e) {
    try {
      render("exception.html", ImmutableMap.<String, Object>builder()
          .put("exception", e)
          .put("stack_trace", Throwables.getStackTraceAsString(e))
          .build());
      e.populate(getResponse());
    } catch (Exception e2) {
      log.error("Error loading exception.html template.", e2);
      setResponseText(e2.toString());
    }
  }

  /**
   * Gets a collection of roles, one of which the current user must be in to access this handler.
   */
  protected Collection<AdminRole> getRequiredRoles() {
    return Lists.newArrayList(AdminRole.ADMINISTRATOR);
  }

  protected void render(String path, @Nullable Map<String, Object> data) throws RequestException {
    if (data == null) {
      data = new TreeMap<>();
    }
    if (data instanceof ImmutableMap) {
      data = new TreeMap<>(data); // make it mutable again...
    }

    data.put("realm", getRealm());
    Session session = getSessionNoError();
    if (session != null) {
      data.put("session", session);
    }
    data.put("num_backend_users", DataStore.i.adminUsers().count());

    getResponse().setContentType("text/html");
    getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
    try {
      getResponse().getWriter().write(TEMPLATE_ENGINE.process(path, data));
    } catch (CarrotException | IOException e) {
      log.error("Error rendering template!", e);
      throw new RequestException(e);
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

  protected void authenticate() {
    URI requestUrl = null;
    try {
      requestUrl = new URI(getRequestUrl());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    String finalUrl = requestUrl.getPath();
    String redirectUrl = requestUrl.resolve("/admin/login").toString();
    try {
      redirectUrl += "?continue=" + URLEncoder.encode(finalUrl, "utf-8");
    } catch (UnsupportedEncodingException e) {
      // should never happen
    }

    redirect(redirectUrl);
  }

  private static class NumberFilter implements Filter {
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0");

    @Override
    public String getName() {
      return "number";
    }

    @Override
    public Object filter(Object object, CarrotInterpreter interpreter, String... args)
        throws InterpretException {
      if (object == null) {
        return null;
      }

      if (object instanceof Integer) {
        int n = (int) object;
        return FORMAT.format(n);
      }
      if (object instanceof Long) {
        long n = (long) object;
        return FORMAT.format(n);
      }
      if (object instanceof Float) {
        float n = (float) object;
        return FORMAT.format(n);
      }
      if (object instanceof Double) {
        double n = (double) object;
        return FORMAT.format(n);
      }

      throw new InterpretException("Expected a number.");
    }
  }

  private static class AttrEscapeFilter implements Filter {
    @Override
    public String getName() {
      return "attr-escape";
    }

    @Override
    public Object filter(Object object, CarrotInterpreter interpreter, String... args)
        throws InterpretException {
      return object.toString().replace("\"", "&quot;").replace("'", "&squot;");
    }
  }

  private static class LocalDateFilter implements Filter {
    @Override
    public String getName() {
      return "local-date";
    }

    @Override
    public Object filter(Object object, CarrotInterpreter interpreter, String... args)
        throws InterpretException {
      if (object instanceof DateTime) {
        DateTime dt = (DateTime) object;
        return String.format(Locale.ENGLISH, "<script>(function() {"
            + " var dt = new Date(\"%s\");" + " +document.write(dt.toLocaleString());"
            + "})();</script>", dt);
      }

      throw new InterpretException("Expected a DateTime.");
    }
  }

  private static class IsInRoleFilter implements Filter {
    @Override
    public String getName() {
      return "isinrole";
    }

    @Override
    public Object filter(Object object, CarrotInterpreter interpreter, String... args)
        throws InterpretException {
      if (object instanceof Session) {
        AdminRole role = AdminRole.valueOf(args[0]);
        return ((Session) object).isInRole(role);
      }

      throw new InterpretException("Expected a Session, not " + object);
    }
  }
}
