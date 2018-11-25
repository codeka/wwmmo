package au.com.codeka.warworlds.server.handlers;

import com.google.api.client.util.ByteStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import au.com.codeka.warworlds.common.Log;

/** Simple handler for handling static files (and 'templated' HTML files with no templated data). */
public class FileHandler extends RequestHandler {
  private final Log log = new Log("AdminGenericHandler");

  private final String basePath;

  public FileHandler(String basePath) {
    this.basePath = basePath;
  }

  @Override
  protected void get() throws RequestException {
    String path = getExtraOption();
    if (path == null) {
      path = "";
    }
    path += getUrlParameter("path");

    String contentType;
    if (path.endsWith(".css")) {
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
      InputStream ins = new FileInputStream(new File(basePath + path));
      OutputStream outs = getResponse().getOutputStream();
      ByteStreams.copy(ins, outs);
      ins.close();
    } catch (FileNotFoundException e) {
      log.error("Error", e);
      throw new RequestException(404, e.getMessage());
    } catch (IOException e) {
      log.error("Error sending static file!", e);
    }
  }

  @Override
  public void post() throws RequestException {
    String path = getExtraOption();
    if (path == null) {
      path = "";
    }
    path += getUrlParameter("path");
    throw new RequestException(405, path);
  }
}
