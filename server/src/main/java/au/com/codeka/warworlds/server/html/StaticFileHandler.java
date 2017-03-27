package au.com.codeka.warworlds.server.html;

import au.com.codeka.warworlds.server.handlers.FileHandler;

/**
 * Implemenation of {@link FileHandler} that serves files out of html/static.
 */
public class StaticFileHandler extends FileHandler {
  public StaticFileHandler() {
    super("data/html/static/");
  }
}
