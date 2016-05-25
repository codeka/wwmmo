package au.com.codeka.warworlds.server.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.Image;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.planetrender.PlanetRenderer;
import au.com.codeka.warworlds.planetrender.Template;
import au.com.codeka.warworlds.server.world.StarManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * A {@link HttpServlet} which renders images of stars, planets, empire sheilds and so on, on
 * demand.
 */
public class RendererServlet extends HttpServlet {
  private final Log log = new Log("RendererServlet");

  private static final Pattern STAR_URL_PATTERN = Pattern.compile(
      "^/star/(?<star>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$");

  private static final Pattern PLANET_URL_PATTERN = Pattern.compile(
      "^/planet/(?<star>[0-9]+)/(?<planet>[0-9]+)/(?<width>[0-9]+)x(?<height>[0-9]+)/(?<bucket>[a-z]+dpi)\\.png$");

  private static final Map<String, Float> BUCKET_FACTORS = ImmutableMap.<String, Float>builder()
      .put("ldpi", 0.75f)
      .put("mdpi", 1.0f)
      .put("hdpi", 1.5f)
      .put("xhdpi", 2.0f)
      .put("xxhdpi", 3.0f)
      .put("xxxhdpi", 4.0f)
      .build();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String path = request.getPathInfo();
    if (path.startsWith("/star/")) {
      handleStar(request, response);
    } else if (path.startsWith("/planet/")) {
      handlePlanet(request, response);
    } else {
      log.warning("Unknown render request: %s", path);
      response.setStatus(404);
    }
  }

  private void handleStar(HttpServletRequest request, HttpServletResponse response) {
    Matcher matcher = STAR_URL_PATTERN.matcher(request.getPathInfo());
    if (!matcher.matches()) {
      log.warning("Invalid planet URL: %s", request.getPathInfo());
      response.setStatus(404);
      return;
    }

    long starId = Long.parseLong(matcher.group("star"));
    int width = Integer.parseInt(matcher.group("width"));
    int height = Integer.parseInt(matcher.group("height"));
    String bucket = matcher.group("bucket");
    Float factor = BUCKET_FACTORS.get(bucket);
    if (factor == null) {
      log.warning("Invalid bucket: %s", request.getPathInfo());
      response.setStatus(404);
      return;
    }

    WatchableObject<Star> star = StarManager.i.getStar(starId);
    File cacheFile = new File(String.format(Locale.ENGLISH,
        "data/cache/star/%d/%dx%d/%s.png", starId, width, height, bucket));
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile, response);
      return;
    } else {
      cacheFile.getParentFile().mkdirs();
    }

    Random rand = new Random(starId);
    File templateFile = getTemplateFile(rand, "star", star.get().classification.toString());
    if (templateFile == null) {
      response.setStatus(500);
      return;
    }

    long startTime = System.nanoTime();
    if (!generateImage(cacheFile, templateFile, width, height, factor, rand)) {
      response.setStatus(500);
      return;
    }
    long endTime = System.nanoTime();
    log.info("%dms to generate image for %s",
        (endTime - startTime) / 1000000L, request.getPathInfo());

    serveCachedFile(cacheFile, response);
  }

  private void handlePlanet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Matcher matcher = PLANET_URL_PATTERN.matcher(request.getPathInfo());
    if (!matcher.matches()) {
      log.warning("Invalid planet URL: %s", request.getPathInfo());
      response.setStatus(404);
      return;
    }

    long starId = Long.parseLong(matcher.group("star"));
    int planetIndex = Integer.parseInt(matcher.group("planet"));
    int width = Integer.parseInt(matcher.group("width"));
    int height = Integer.parseInt(matcher.group("height"));
    String bucket = matcher.group("bucket");
    Float factor = BUCKET_FACTORS.get(bucket);
    if (factor == null) {
      log.warning("Invalid bucket: %s", request.getPathInfo());
      response.setStatus(404);
      return;
    }

    WatchableObject<Star> star = StarManager.i.getStar(starId);
    if (planetIndex >= star.get().planets.size() || planetIndex < 0) {
      log.warning("PlanetIndex is out of bounds.");
      response.setStatus(404);
      return;
    }
    Planet planet = star.get().planets.get(planetIndex);

    File cacheFile = new File(String.format(Locale.ENGLISH,
        "data/cache/planet/%d/%d/%dx%d/%s.png", starId, planetIndex, width, height, bucket));
    if (cacheFile.exists()) {
      serveCachedFile(cacheFile, response);
      return;
    } else {
      cacheFile.getParentFile().mkdirs();
    }

    Random rand = new Random(starId + planetIndex);
    File templateFile = getTemplateFile(rand, "planet", planet.planet_type.toString());
    long startTime = System.nanoTime();
    if (!generateImage(cacheFile, templateFile, width, height, factor, rand)) {
      response.setStatus(500);
      return;
    }
    long endTime = System.nanoTime();
    log.info("%dms to generate image for %s",
        (endTime - startTime) / 1000000L, request.getPathInfo());

    serveCachedFile(cacheFile, response);
  }

  private boolean generateImage(
      File cacheFile, File templateFile, int width, int height, float factor, Random rand) {
    width = (int) Math.ceil(width * factor);
    height = (int) Math.ceil(height * factor);

    Template tmpl;
    try (InputStream ins = new FileInputStream(templateFile)) {
      tmpl = Template.parse(ins);
    } catch(Exception e) {
      log.warning("Error parsing template: %s", templateFile, e);
      return false;
    }
    PlanetRenderer renderer;
    if (tmpl.getTemplate() instanceof Template.PlanetsTemplate) {
      renderer = new PlanetRenderer((Template.PlanetsTemplate) tmpl.getTemplate(), rand);
    } else if (tmpl.getTemplate() instanceof Template.PlanetTemplate) {
      renderer = new PlanetRenderer((Template.PlanetTemplate) tmpl.getTemplate(), rand);
    } else {
      log.warning("Unknown template: %s", tmpl.getTemplate().getClass().getSimpleName());
      return false;
    }


    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    renderer.render(img);
    try {
      ImageIO.write(img, "png", cacheFile);
    } catch (IOException e) {
      log.warning("Error writing image.", e);
      return false;
    }
    return true;
  }

  private void serveCachedFile(File file, HttpServletResponse response) {
    response.setContentType("image/png");
    response.setHeader("Cache-Control", "max-age=2592000"); // 30 days
    try (InputStream ins = new FileInputStream(file)) {
      ByteStreams.copy(ins, response.getOutputStream());
    } catch(IOException e) {
      log.warning("Exception caught serving file.", e);
      response.setStatus(500);
    }
  }

  /**
   * Gets a {@link File} that refers to a planet renderer template for rendering the given type
   * (i.e. star vs planet) and classification (black hole, swamp, etc).
   *
   * @param rand A {@link Random} that we'll use to select from one of multiple possible templates.
   * @param type The type of the object (one of "star" or "planet").
   * @param classification The classification of the object ("blackhole", "swamp", etc).
   * @return A {@link File} pointing to a template for rendering that object, or null if no template
   *     can be found (e.g. invalid type or classifcation, etc).
   */
  @Nullable
  private File getTemplateFile(Random rand, String type, String classification) {
    File parentDirectory = new File(String.format("data/renderer/%s/%s",
        type.toLowerCase(), classification.toLowerCase()));
    if (!parentDirectory.exists()) {
      log.warning("Could not load template for %s/%s: %s",
          type, classification, parentDirectory.getAbsolutePath());
      return null;
    }

    File[] files = parentDirectory.listFiles((dir, name) -> {
      return name.endsWith(".xml");
    });
    return files[rand.nextInt(files.length)];
  }
}
