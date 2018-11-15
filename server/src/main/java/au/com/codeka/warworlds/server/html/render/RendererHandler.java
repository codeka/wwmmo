package au.com.codeka.warworlds.server.html.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector3;
import au.com.codeka.warworlds.planetrender.PlanetRenderer;
import au.com.codeka.warworlds.planetrender.Template;
import au.com.codeka.warworlds.server.handlers.RequestHandler;

/**
 * Base class for the handlers that render images.
 */
public class RendererHandler extends RequestHandler {
  private final Log log = new Log("RendererHandler");

  protected static final Map<String, Float> BUCKET_FACTORS = ImmutableMap.<String, Float>builder()
      .put("ldpi", 0.75f)
      .put("mdpi", 1.0f)
      .put("hdpi", 1.5f)
      .put("xhdpi", 2.0f)
      .put("xxhdpi", 3.0f)
      .put("xxxhdpi", 4.0f)
      .build();


  protected boolean generateImage(
      File cacheFile,
      File templateFile,
      @Nullable Vector3 sunDirection,
      int width,
      int height,
      float factor,
      Random rand) {
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
      Template.PlanetsTemplate planetsTemplate = (Template.PlanetsTemplate) tmpl.getTemplate();
      if (sunDirection != null) {
        for (Template.BaseTemplate child : planetsTemplate.getParameters()) {
          ((Template.PlanetTemplate) child).setSunLocation(sunDirection);
        }
      }
      renderer = new PlanetRenderer(planetsTemplate, rand);
    } else if (tmpl.getTemplate() instanceof Template.PlanetTemplate) {
      Template.PlanetTemplate planetTemplate = (Template.PlanetTemplate) tmpl.getTemplate();
      if (sunDirection != null) {
        planetTemplate.setSunLocation(sunDirection);
      }
      renderer = new PlanetRenderer(planetTemplate, rand);
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

  protected void serveCachedFile(File file) {
    getResponse().setContentType("image/png");
    getResponse().setHeader("Cache-Control", "max-age=2592000"); // 30 days
    try (InputStream ins = new FileInputStream(file)) {
      ByteStreams.copy(ins, getResponse().getOutputStream());
    } catch(IOException e) {
      log.warning("Exception caught serving file.", e);
      getResponse().setStatus(500);
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
  protected File getTemplateFile(Random rand, String type, String classification) {
    File parentDirectory = new File(String.format("data/renderer/%s/%s",
        type.toLowerCase(), classification.toLowerCase()));
    if (!parentDirectory.exists()) {
      log.warning("Could not load template for %s/%s: %s",
          type, classification, parentDirectory.getAbsolutePath());
      return null;
    }

    File[] files = parentDirectory.listFiles((dir, name) -> name.endsWith(".xml"));
    if (files == null) {
      return null;
    }
    return files[rand.nextInt(files.length)];
  }
}
