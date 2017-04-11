package au.com.codeka.warworlds.server.html;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.resource.FileResourceLocater;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.handlers.RequestHandler;

/**
 * Handler for requests out of the html directory.
 */
public class HtmlRequestHandler extends RequestHandler {
  private static final CarrotEngine CARROT_ENGINE = new CarrotEngine();
  static {
    CARROT_ENGINE.getConfig().setResourceLocater(
        new FileResourceLocater(
            CARROT_ENGINE.getConfig(),
            new File("data/html/tmpl").getAbsolutePath()));
    CARROT_ENGINE.getConfig().setEncoding("utf-8");
  }

  protected void render(
      String tmplName,
      @Nullable Map<String, Object> data) throws RequestException {
    getResponse().setContentType("text/html");
    getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
    try {
      getResponse().getWriter().write(CARROT_ENGINE.process(tmplName, data));
    } catch (CarrotException | IOException e) {
      throw new RequestException(e);
    }
  }
}
