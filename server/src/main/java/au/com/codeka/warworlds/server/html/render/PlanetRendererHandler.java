package au.com.codeka.warworlds.server.html.render;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector3;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.WatchableObject;
import java.io.File;
import java.util.Locale;
import java.util.Random;

/**
 * {@link RendererHandler} for rendering planets.
 */
public class PlanetRendererHandler extends RendererHandler {
  private static final Log log = new Log("PlanetRendererHandler");

  @Override
  protected void get() throws RequestException {
    long starId = Long.parseLong(getUrlParameter("star"));
    int planetIndex = Integer.parseInt(getUrlParameter("planet"));
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
    if (planetIndex >= star.get().planets.size() || planetIndex < 0) {
      log.warning("PlanetIndex is out of bounds.");
      getResponse().setStatus(404);
      return;
    }
    Planet planet = star.get().planets.get(planetIndex);

    File cacheFile = new File(String.format(Locale.ENGLISH,
        "data/cache/planet/%d/%d/%dx%d/%s.png", starId, planetIndex, width, height, bucket));
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile);
      return;
    } else {
      cacheFile.getParentFile().mkdirs();
    }

    Random rand = new Random(starId + planetIndex);
    File templateFile = getTemplateFile(rand, "planet", planet.planet_type.toString());
    long startTime = System.nanoTime();
    Vector3 sunDirection = getSunDirection(star.get(), planetIndex);
    if (!generateImage(cacheFile, templateFile, sunDirection, width, height, factor, rand)) {
      getResponse().setStatus(500);
      return;
    }
    long endTime = System.nanoTime();
    log.info("%dms to generate image for %s",
        (endTime - startTime) / 1000000L, getRequest().getPathInfo());

    serveCachedFile(cacheFile);
  }

  private Vector3 getSunDirection(Star star, int planetIndex) {
    int numPlanets = star.planets.size();
    float angle = (0.5f/(numPlanets + 1));
    angle = (float) ((angle * planetIndex * Math.PI) + (angle * Math.PI));

    Vector3 sunDirection = new Vector3(0.0, 1.0, -1.0);
    sunDirection.rotateZ(angle);
    sunDirection.scale(200.0);
    return sunDirection;
  }

}
