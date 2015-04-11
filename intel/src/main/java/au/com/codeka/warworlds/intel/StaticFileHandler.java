package au.com.codeka.warworlds.intel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import au.com.codeka.common.Log;

/** Handler for static files (html, css, js, etc). */
public class StaticFileHandler extends RequestHandler {
  private static final Log log = new Log("StaticFileHandler");

  @Override
  public void get() throws RequestException {
    String path = getUrlParameter("path");
    if (path.isEmpty()) {
      path = "html/index.html";
    }

    File file = new File(Configuration.i.getDataDirectory());
    file = new File(file, path);
    if (!file.exists()) {
      throw new RequestException(404);
    }

    String contentType;
    if (path.endsWith(".html")) {
      contentType = "text/html";
    } else if (path.endsWith(".css")) {
      contentType = "text/css";
    } else if (path.endsWith(".js")) {
      contentType = "text/javascript";
    } else if (path.endsWith(".png")) {
      contentType = "image/png";
    } else if (path.endsWith(".ico")) {
      contentType = "image/x-icon";
    } else {
      contentType = "text/plain";
    }
    getResponse().setContentType(contentType);
    getResponse().setHeader("Content-Type", contentType);

    try {
      OutputStream outs = getResponse().getOutputStream();
      InputStream ins = new FileInputStream(file);

      byte[] buffer = new byte[1024];
      int bytes;
      while ((bytes = ins.read(buffer, 0, 1024)) > 0) {
        outs.write(buffer, 0, bytes);
      }

      ins.close();
    } catch (IOException e) {
      log.error("Error sending static file!", e);
    }
  }
}
