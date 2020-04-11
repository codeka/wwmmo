package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;

public class AdminGenericHandler extends AdminHandler {
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
    getResponse().setHeader("Content-Type", contentType);

    path = getExtraOption() + path;
    if (path.equals(getExtraOption() + ".html")) {
      path = getExtraOption() + "index.html";
    }

    render(path, new TreeMap<>());
  }
}
