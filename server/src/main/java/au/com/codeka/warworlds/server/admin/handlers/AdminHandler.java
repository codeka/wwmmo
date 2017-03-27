package au.com.codeka.warworlds.server.admin.handlers;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.resource.FileResourceLocater;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.handlers.RequestHandler;
import au.com.codeka.warworlds.server.admin.Session;
import au.com.codeka.warworlds.server.store.DataStore;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminHandler extends RequestHandler {
  private final Log log = new Log("AdminHandler");

  private static final CarrotEngine CARROT_ENGINE = new CarrotEngine();
  static {
    CARROT_ENGINE.getConfig().setResourceLocater(
        new FileResourceLocater(
            CARROT_ENGINE.getConfig(),
            new File("data/admin/tmpl").getAbsolutePath()));
    CARROT_ENGINE.getConfig().setEncoding("utf-8");

    CARROT_ENGINE.getGlobalBindings().put("Session", new SessionHelper());
  }

  private Session session;

  /** Set up this {@link RequestHandler}, must be called before any other methods. */
  public void setup(
      Matcher routeMatcher,
      String extraOption,
      Session session,
      HttpServletRequest request,
      HttpServletResponse response) {
    super.setup(routeMatcher, extraOption, request, response);
    this.session = session;
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
          log.warning("User '%s' is not in any required role: %s",
              session.getEmail(), requiredRoles);
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
          .put("e", e)
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

    Session session = getSessionNoError();
    if (session != null) {
      data.put("session", session);
    }
    data.put("num_backend_users", DataStore.i.adminUsers().count());

    getResponse().setContentType("text/html");
    getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
    try {
      getResponse().getWriter().write(CARROT_ENGINE.process(path, data));
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


  protected Session getSession() throws RequestException {
    if (session == null) {
      throw new RequestException(403);
    }

    return session;
  }

  @Nullable
  protected Session getSessionNoError() {
    return session;
  }

  /**
   * Checks whether the current user is in the given role.
   */
  protected boolean isInRole(AdminRole role) throws RequestException {
    if (session == null) {
      return false;
    }

    return session.isInRole(role);
  }

  private static class SessionHelper {
    @SuppressWarnings("unused") // Used by template engine.
    public boolean isInRole(Session session, String roleName) throws CarrotException {
      AdminRole role = AdminRole.valueOf(roleName);
      return session.isInRole(role);
    }
  }
}
