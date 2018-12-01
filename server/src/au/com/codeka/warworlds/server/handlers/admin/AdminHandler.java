package au.com.codeka.warworlds.server.handlers.admin;

import com.google.common.base.Throwables;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.handlers.RenderingHandler;

public class AdminHandler extends RenderingHandler {
  private final Log log = new Log("AdminHandler");

  @Override
  public void onBeforeHandle() {
    if (!(this instanceof AdminLoginHandler)) {
      // if we're not the Login handler and we're not yet authed, auth now
      if (getSessionNoError() == null || !getSessionNoError().isAdmin()) {
        // if they're not authenticated yet, we'll have to redirect them to the
        // authentication
        // page first.
        authenticate();
        return;
      }
    }
  }

  @Override
  protected void handleException(RequestException e) {
    try {
      TreeMap<String, Object> data = new TreeMap<>();
      data.put("exception", e);
      data.put("stack_trace", Throwables.getStackTraceAsString(e));
      render("exception.html", data);
    } catch (Exception e2) {
      setResponseBody(e.getGenericError());
    }
  }

  protected void writeJson(JsonElement json) {
    getResponse().setContentType("text/json");
    getResponse().setHeader("Content-Type", "text/json; charset=utf-8");
    try {
      getResponse().getWriter().write(json.toString());
    } catch (IOException e) {
      log.error("Error writing output!", e);
    }
  }

  protected void authenticate() {
    URI requestUrl;
    try {
      requestUrl = new URI(getRequestUrl());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    String finalUrl = requestUrl.getPath();
    String redirectUrl = requestUrl.resolve("/realms/" + getRealm() + "/admin/login").toString();
    try {
      redirectUrl += "?continue=" + URLEncoder.encode(finalUrl, "utf-8");
    } catch (UnsupportedEncodingException e) {
      // should never happen
    }

    redirect(redirectUrl);
  }
}
