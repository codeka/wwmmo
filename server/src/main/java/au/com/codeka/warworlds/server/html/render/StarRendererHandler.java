package au.com.codeka.warworlds.server.html.render;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.WatchableObject;
import java.io.File;
import java.util.Locale;
import java.util.Random;

/**
 * {@link RendererHandler} for rendering stars.
 */

public class StarRendererHandler extends RendererHandler {
  private static final Log log = new Log("StarRendererHandler");

  @Override
  public void get() throws RequestException {
    long starId = Long.parseLong(getUrlParameter("star"));
    int width = Integer.parseInt(getUrlParameter("width"));
    int height = Integer.parseInt(getUrlParameter("height"));
    String bucket = getUrlParameter("bucket");
    Float factor = BUCKET_FACTORS.get(bucket);
    if (factor == null) {
      log.warning("Invalid bucket: %s", getRequest().getPathInfo());
      getResponse().setStatus(404);
      return;
    }

    WatchableObject<Star> star = StarManager.i.getStar(starId);
    File cacheFile = new File(String.format(Locale.ENGLISH,
        "data/cache/star/%d/%dx%d/%s.png", starId, width, height, bucket));
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile);
      return;
    } else {
      cacheFile.getParentFile().mkdirs();
    }

    Random rand = new Random(starId);
    File templateFile = getTemplateFile(rand, "star", star.get().classification.toString());
    if (templateFile == null) {
      getResponse().setStatus(500);
      return;
    }

    long startTime = System.nanoTime();
    if (!generateImage(cacheFile, templateFile, null, width, height, factor, rand)) {
      getResponse().setStatus(500);
      return;
    }
    long endTime = System.nanoTime();
    log.info("%dms to generate image for %s",
        (endTime - startTime) / 1000000L, getRequest().getPathInfo());

    serveCachedFile(cacheFile);
  }

}
