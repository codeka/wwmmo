package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;

public class AdminGenericHandler extends AdminHandler {
  private static Log log = new Log("AdminGenericHandler");

  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    String path = getUrlParameter("path");

    String contentType;
    if (path.endsWith(".css")) {
      contentType = "text/css";
    } else if (path.endsWith(".js")) {
      contentType = "text/javascript";
    } else {
      path = path + ".html";
      contentType = "text/html";
    }
    getResponse().setContentType(contentType);
    getResponse().setHeader("Content-Type", contentType + "; charset=utf-8");

    path = getExtraOption() + path;
    if (path.equals(getExtraOption() + ".html")) {
      path = getExtraOption() + "index.html";
    }

    log.info("rendering: %s", path);
    render(path, new TreeMap<>());
  }
}
