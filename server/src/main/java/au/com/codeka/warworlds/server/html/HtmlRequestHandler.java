package au.com.codeka.warworlds.server.html;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import au.com.codeka.carrot.CarrotEngine;
import au.com.codeka.carrot.CarrotException;
import au.com.codeka.carrot.Configuration;
import au.com.codeka.carrot.bindings.MapBindings;
import au.com.codeka.carrot.resource.FileResourceLocator;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.handlers.RequestHandler;

/**
 * Handler for requests out of the html directory.
 */
public class HtmlRequestHandler extends RequestHandler {
  private static final CarrotEngine CARROT = new CarrotEngine(new Configuration.Builder()
      .setResourceLocator(
          new FileResourceLocator.Builder(new File("data/html/tmpl").getAbsolutePath()))
      .build());

  protected void render(
      String tmplName,
      @Nullable Map<String, Object> data) throws RequestException {
    getResponse().setContentType("text/html");
    getResponse().setHeader("Content-Type", "text/html; charset=utf-8");
    try {
      getResponse().getWriter().write(CARROT.process(tmplName, new MapBindings(data)));
    } catch (CarrotException | IOException e) {
      throw new RequestException(e);
    }
  }
}
