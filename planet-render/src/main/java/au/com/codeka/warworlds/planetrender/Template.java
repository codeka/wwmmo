package au.com.codeka.warworlds.planetrender;

import au.com.codeka.warworlds.common.Colour;
import au.com.codeka.warworlds.common.ColourGradient;
import au.com.codeka.warworlds.common.Vector3;
import au.com.codeka.warworlds.common.XmlIterator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A template is used to create an image (usually a complete planet, but it doesn't have to be).
 */
public class Template {
  private BaseTemplate data;
  private int version;
  private String name;

  public BaseTemplate getTemplate() {
    return data;
  }

  public int getTemplateVersion() {
    return version;
  }

  /*
   * The name of the template is whatever you want it to be. We don't care...
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Parses the given string input and returns the \c Template it represents.
   */
  public static Template parse(InputStream inp) throws TemplateException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setValidating(false);

    Document xmldoc;
    try {
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      xmldoc = builder.parse(inp);
    } catch (Exception e) {
      throw new TemplateException(e);
    }

    Template tmpl = new Template();
    String version = xmldoc.getDocumentElement().getAttribute("version");
    if (version != null && version.length() > 0) {
      tmpl.version = Integer.parseInt(version);
    } else {
      tmpl.version = 1;
    }
    tmpl.data = parseElement(xmldoc.getDocumentElement());
    return tmpl;
  }

  private static BaseTemplate parseElement(Element elem) throws TemplateException {
    TemplateFactory factory = null;
    if (elem.getTagName().equals("planets")) {
      factory = new PlanetsTemplate.PlanetsTemplateFactory();
    } else if (elem.getTagName().equals("planet")) {
      factory = new PlanetTemplate.PlanetTemplateFactory();
    } else if (elem.getTagName().equals("texture")) {
      factory = new TextureTemplate.TextureTemplateFactory();
    } else if (elem.getTagName().equals("point-cloud")) {
      factory = new PointCloudTemplate.PointCloudTemplateFactory();
    } else if (elem.getTagName().equals("voronoi")) {
      factory = new VoronoiTemplate.VoronoiTemplateFactory();
    } else if (elem.getTagName().equals("colour")) {
      factory = new ColourGradientTemplate.ColourGradientTemplateFactory();
    } else if (elem.getTagName().equals("perlin")) {
      factory = new PerlinNoiseTemplate.PerlinNoiseTemplateFactory();
    } else if (elem.getTagName().equals("atmosphere")) {
      factory = new AtmosphereTemplate.AtmosphereTemplateFactory();
    } else if (elem.getTagName().equals("warp")) {
      factory = new WarpTemplate.WarpTemplateFactory();
    } else {
      throw new TemplateException("Unknown element: " + elem.getTagName());
    }

    return factory.parse(elem);
  }

  private static abstract class TemplateFactory {
    public abstract BaseTemplate parse(Element elem) throws TemplateException;

    protected Vector3 parseVector3(String val) throws TemplateException {
      String[] parts = val.split(" ");
      if (parts.length == 3) {
        return new Vector3(
            Double.parseDouble(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2]));
      } else {
        throw new TemplateException("Invalid vector: " + val);
      }
    }
  }

  public static class BaseTemplate {
    private List<BaseTemplate> parameters;

    public BaseTemplate() {
      parameters = new ArrayList<>();
    }

    public List<BaseTemplate> getParameters() {
      return parameters;
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseTemplate> List<T> getParameters(Class<T> classFactory) {
      List<T> params = new ArrayList<T>();
      for (BaseTemplate bt : parameters) {
        if (bt.getClass().isAssignableFrom(classFactory)) {
          params.add((T) bt);
        }
      }
      return params;
    }

    public <T extends BaseTemplate> T getParameter(Class<T> classFactory) {
      List<T> params = getParameters(classFactory);
      if (params.size() > 0) {
        return params.get(0);
      }
      return null;
    }
  }

  public static class PlanetsTemplate extends BaseTemplate {
    public static class PlanetsTemplateFactory extends TemplateFactory {
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        PlanetsTemplate tmpl = new PlanetsTemplate();

        for (Element child : XmlIterator.childElements(elem)) {
          tmpl.getParameters().add(parseElement(child));
        }

        return tmpl;
      }
    }
  }

  public static class PlanetTemplate extends BaseTemplate {
    private Vector3 northFrom;
    private Vector3 northTo;
    private double planetSize;
    private double ambient;
    private Vector3 sunLocation;
    private Vector3 originFrom;
    private Vector3 originTo;

    public Vector3 getNorthFrom() {
      return northFrom;
    }

    public Vector3 getNorthTo() {
      return northTo;
    }

    public double getPlanetSize() {
      return planetSize;
    }

    public double getAmbient() {
      return ambient;
    }

    public Vector3 getSunLocation() {
      return sunLocation;
    }

    public Vector3 getOriginFrom() {
      return originFrom;
    }

    public Vector3 getOriginTo() {
      return originTo;
    }

    public void setPlanetSize(double size) {
      planetSize = size;
    }

    public void setSunLocation(Vector3 location) {
      sunLocation = location;
    }

    private static class PlanetTemplateFactory extends TemplateFactory {
      /**
       * Parses a <planet> node and returns the corresponding \c PlanetTemplate.
       */
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        PlanetTemplate tmpl = new PlanetTemplate();
        tmpl.originFrom = new Vector3(0.0f, 0.0f, 30.0f);
        tmpl.originTo = new Vector3(0.0f, 0.0f, 30.0f);
        if (elem.getAttribute("origin") != null && !elem.getAttribute("origin").equals("")) {
          Vector3 other = parseVector3(elem.getAttribute("origin"));
          tmpl.originFrom.reset(other);
          tmpl.originTo.reset(other);
        }
        if (elem.getAttribute("originFrom") != null
            && !elem.getAttribute("originFrom").equals("")) {
          Vector3 other = parseVector3(elem.getAttribute("originFrom"));
          tmpl.originFrom.reset(other);
        }
        if (elem.getAttribute("originTo") != null && !elem.getAttribute("originTo").equals("")) {
          Vector3 other = parseVector3(elem.getAttribute("originTo"));
          tmpl.originTo.reset(other);
        }

        tmpl.northFrom = new Vector3(0.0, 1.0, 0.0);
        tmpl.northTo = new Vector3(0.0, 1.0, 0.0);
        if (elem.getAttribute("northFrom") != null && !elem.getAttribute("northFrom").equals("")) {
          Vector3 other = parseVector3(elem.getAttribute("northFrom"));
          tmpl.northFrom.reset(other);
        }
        if (elem.getAttribute("northTo") != null && !elem.getAttribute("northTo").equals("")) {
          Vector3 other = parseVector3(elem.getAttribute("northTo"));
          tmpl.northTo.reset(other);
        }

        tmpl.planetSize = 10.0;
        if (elem.getAttribute("size") != null && !elem.getAttribute("size").equals("")) {
          tmpl.planetSize = Double.parseDouble(elem.getAttribute("size"));
        }

        tmpl.ambient = 0.1;
        if (elem.getAttribute("ambient") != null && !elem.getAttribute("ambient").equals("")) {
          tmpl.ambient = Double.parseDouble(elem.getAttribute("ambient"));
        }

        tmpl.sunLocation = new Vector3(100.0, 100.0, -150.0);
        if (elem.getAttribute("sun") != null && !elem.getAttribute("sun").equals("")) {
          Vector3 other = parseVector3(elem.getAttribute("sun"));
          tmpl.sunLocation.reset(other);
        }

        for (Element child : XmlIterator.childElements(elem)) {
          tmpl.getParameters().add(parseElement(child));
        }

        return tmpl;
      }
    }
  }

  public static class TextureTemplate extends BaseTemplate {
    public enum Generator {
      VoronoiMap,
      PerlinNoise
    }

    private Generator generator;
    private double noisiness;
    private double scaleX;
    private double scaleY;

    public Generator getGenerator() {
      return generator;
    }

    public double getNoisiness() {
      return noisiness;
    }

    public double getScaleX() {
      return scaleX;
    }

    public double getScaleY() {
      return scaleY;
    }

    private static class TextureTemplateFactory extends TemplateFactory {
      /**
       * Parses a <texture> node and returns the corresponding \c TextureTemplate.
       */
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        TextureTemplate tmpl = new TextureTemplate();

        String generator = elem.getAttribute("generator");
        if (generator.equals("voronoi-map")) {
          tmpl.generator = Generator.VoronoiMap;
        } else if (generator.equals("perlin-noise")) {
          tmpl.generator = Generator.PerlinNoise;
        } else {
          throw new TemplateException("Unknown <texture generator> attribute: " + generator);
        }

        String noisiness = elem.getAttribute("noisiness");
        if (noisiness == null || noisiness.equals("")) {
          tmpl.noisiness = 0.5;
        } else {
          tmpl.noisiness = Double.parseDouble(noisiness);
        }

        tmpl.scaleX = 1.0;
        tmpl.scaleY = 1.0;
        if (elem.getAttribute("scaleX") != null && !elem.getAttribute("scaleX").equals("")) {
          tmpl.scaleX = Double.parseDouble(elem.getAttribute("scaleX"));
        }
        if (elem.getAttribute("scaleY") != null && !elem.getAttribute("scaleY").equals("")) {
          tmpl.scaleY = Double.parseDouble(elem.getAttribute("scaleY"));
        }

        for (Element child : XmlIterator.childElements(elem)) {
          tmpl.getParameters().add(parseElement(child));
        }

        return tmpl;
      }
    }
  }

  public static class VoronoiTemplate extends BaseTemplate {
    private static class VoronoiTemplateFactory extends TemplateFactory {
      /**
       * Parses a <voronoi> node and returns the corresponding \c VoronoiTemplate.
       */
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        VoronoiTemplate tmpl = new VoronoiTemplate();

        for (Element child : XmlIterator.childElements(elem)) {
          tmpl.getParameters().add(parseElement(child));
        }

        return tmpl;
      }
    }
  }

  public static class PointCloudTemplate extends BaseTemplate {
    public enum Generator {
      Random,
      Poisson
    }

    private Generator generator;
    private double density;
    private double randomness;

    public Generator getGenerator() {
      return generator;
    }

    public double getDensity() {
      return density;
    }

    public double getRandomness() {
      return randomness;
    }

    private static class PointCloudTemplateFactory extends TemplateFactory {
      /**
       * Parses a <point-cloud> node and returns the corresponding \c PointCloudTemplate.
       */
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        PointCloudTemplate tmpl = new PointCloudTemplate();

        String val = elem.getAttribute("generator");
        if (val.equals("random")) {
          tmpl.generator = PointCloudTemplate.Generator.Random;
        } else if (val.equals("poisson")) {
          tmpl.generator = PointCloudTemplate.Generator.Poisson;
        } else {
          throw new TemplateException("Unknown <point-cloud> 'generator' attribute: " + val);
        }

        tmpl.density = Double.parseDouble(elem.getAttribute("density"));
        tmpl.randomness = Double.parseDouble(elem.getAttribute("randomness"));
        return tmpl;
      }
    }
  }

  public static class ColourGradientTemplate extends BaseTemplate {
    private ColourGradient mColourGradient;

    public ColourGradient getColourGradient() {
      return mColourGradient;
    }

    /**
     * Parses a <colour> node and returns the corresponding \c ColourGradient.
     */
    private static class ColourGradientTemplateFactory extends TemplateFactory {
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        ColourGradientTemplate tmpl = new ColourGradientTemplate();
        tmpl.mColourGradient = new ColourGradient();

        for (Element child : XmlIterator.childElements(elem, "node")) {
          double n = Double.parseDouble(child.getAttribute("n"));
          int argb = (int) Long.parseLong(child.getAttribute("colour"), 16);
          tmpl.mColourGradient.addNode(n, new Colour(argb));
        }

        return tmpl;
      }
    }
  }

  public static class PerlinNoiseTemplate extends BaseTemplate {
    public enum Interpolation {
      None,
      Linear,
      Cosine
    }

    private double persistence;
    private Interpolation interpolation;
    private int startOctave;
    private int endOctave;

    public double getPersistence() {
      return persistence;
    }

    public Interpolation getInterpolation() {
      return interpolation;
    }

    public int getStartOctave() {
      return startOctave;
    }

    public int getEndOctave() {
      return endOctave;
    }

    private static class PerlinNoiseTemplateFactory extends TemplateFactory {
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        PerlinNoiseTemplate tmpl = new PerlinNoiseTemplate();

        String val = elem.getAttribute("interpolation");
        if (val == null || val.equals("linear")) {
          tmpl.interpolation = Interpolation.Linear;
        } else if (val.equals("cosine")) {
          tmpl.interpolation = Interpolation.Cosine;
        } else if (val.equals("none")) {
          tmpl.interpolation = Interpolation.None;
        } else {
          throw new TemplateException("Unknown <perlin> 'interpolation' attribute: " + val);
        }

        tmpl.persistence = Double.parseDouble(elem.getAttribute("persistence"));
        if (elem.getAttribute("startOctave") == null) {
          tmpl.startOctave = 1;
        } else {
          tmpl.startOctave = Integer.parseInt(elem.getAttribute("startOctave"));
        }
        if (elem.getAttribute("endOctave") == null) {
          tmpl.endOctave = 10;
        } else {
          tmpl.endOctave = Integer.parseInt(elem.getAttribute("endOctave"));
        }

        return tmpl;
      }
    }
  }

  public static class AtmosphereTemplate extends BaseTemplate {
    public enum BlendMode {
      Alpha,
      Additive,
      Multiply
    }

    private InnerOuterTemplate innerTemplate;
    private InnerOuterTemplate outerTemplate;
    private StarTemplate starTemplate;

    public InnerOuterTemplate getInnerTemplate() {
      return innerTemplate;
    }

    public InnerOuterTemplate getOuterTemplate() {
      return outerTemplate;
    }

    public StarTemplate getStarTemplate() {
      return starTemplate;
    }

    public static class InnerOuterTemplate extends BaseTemplate {
      private double sunStartShadow;
      private double sunShadowFactor;
      private double size;
      private double noisiness;
      private BlendMode blendMode;

      public double getSunStartShadow() {
        return sunStartShadow;
      }

      public double getSunShadowFactor() {
        return sunShadowFactor;
      }

      public double getSize() {
        return size;
      }

      public double getNoisiness() {
        return noisiness;
      }

      public BlendMode getBlendMode() {
        return blendMode;
      }
    }

    public static class StarTemplate extends InnerOuterTemplate {
      public int numPoints;
      public double baseWidth;
      public double slope;

      public int getNumPoints() {
        return numPoints;
      }

      public double getBaseWidth() {
        return baseWidth;
      }

      public double getSlope() {
        return slope;
      }
    }

    private static class AtmosphereTemplateFactory extends TemplateFactory {
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        AtmosphereTemplate tmpl = new AtmosphereTemplate();

        for (Element child : XmlIterator.childElements(elem)) {
          if (child.getTagName().equals("inner")) {
            tmpl.innerTemplate = new InnerOuterTemplate();
            parseInnerOuterTemplate(tmpl.innerTemplate, child);
          } else if (child.getTagName().equals("outer")) {
            tmpl.outerTemplate = new InnerOuterTemplate();
            parseInnerOuterTemplate(tmpl.outerTemplate, child);
          } else if (child.getTagName().equals("star")) {
            tmpl.starTemplate = new StarTemplate();
            parseStarTemplate(tmpl.starTemplate, child);
          }
        }

        return tmpl;
      }

      private void parseInnerOuterTemplate(InnerOuterTemplate tmpl, Element elem)
          throws TemplateException {
        if (elem.getAttribute("sunStartShadow") != null
            && !elem.getAttribute("sunStartShadow").equals("")) {
          tmpl.sunStartShadow = Double.parseDouble(elem.getAttribute("sunStartShadow"));
        }

        if (elem.getAttribute("sunShadowFactor") != null
            && !elem.getAttribute("sunShadowFactor").equals("")) {
          tmpl.sunShadowFactor = Double.parseDouble(elem.getAttribute("sunShadowFactor"));
        }

        tmpl.size = 1.0;
        if (elem.getAttribute("size") != null && !elem.getAttribute("size").equals("")) {
          tmpl.size = Double.parseDouble(elem.getAttribute("size"));
        }

        tmpl.blendMode = BlendMode.Alpha;
        if (elem.getAttribute("blend") != null && !elem.getAttribute("blend").equals("")) {
          if (elem.getAttribute("blend").equalsIgnoreCase("alpha")) {
            tmpl.blendMode = BlendMode.Alpha;
          } else if (elem.getAttribute("blend").equalsIgnoreCase("additive")) {
            tmpl.blendMode = BlendMode.Additive;
          } else if (elem.getAttribute("blend").equalsIgnoreCase("multiply")) {
            tmpl.blendMode = BlendMode.Multiply;
          } else {
            throw new TemplateException("Unknown 'blend' value: " + elem.getAttribute("blend"));
          }
        }

        if (elem.getAttribute("noisiness") != null && !elem.getAttribute("noisiness").equals("")) {
          tmpl.noisiness = Double.parseDouble(elem.getAttribute("noisiness"));
        }

        for (Element child : XmlIterator.childElements(elem)) {
          tmpl.getParameters().add(parseElement(child));
        }
      }

      private void parseStarTemplate(StarTemplate tmpl, Element elem)
          throws TemplateException {
        parseInnerOuterTemplate(tmpl, elem);

        tmpl.numPoints = 5;
        if (elem.getAttribute("points") != null && !elem.getAttribute("points").equals("")) {
          tmpl.numPoints = Integer.parseInt(elem.getAttribute("points"));
        }

        tmpl.baseWidth = 1.0;
        if (elem.getAttribute("baseWidth") != null && !elem.getAttribute("baseWidth").equals("")) {
          tmpl.baseWidth = Double.parseDouble(elem.getAttribute("baseWidth"));
        }

        tmpl.slope = 0.0;
        if (elem.getAttribute("slope") != null && !elem.getAttribute("slope").equals("")) {
          tmpl.slope = Double.parseDouble(elem.getAttribute("slope"));
        }
      }
    }
  }

  public static class WarpTemplate extends BaseTemplate {
    public enum NoiseGenerator {
      Perlin,
      Spiral
    }

    private NoiseGenerator noiseGenerator;
    private double warpFactor;

    public NoiseGenerator getNoiseGenerator() {
      return noiseGenerator;
    }

    public double getWarpFactor() {
      return warpFactor;
    }

    private static class WarpTemplateFactory extends TemplateFactory {
      /**
       * Parses a <warp> node and returns the corresponding \c ImageWarpTemplate.
       */
      @Override
      public BaseTemplate parse(Element elem) throws TemplateException {
        WarpTemplate tmpl = new WarpTemplate();

        if (elem.getAttribute("generator").equals("perlin-noise")) {
          tmpl.noiseGenerator = NoiseGenerator.Perlin;
        } else if (elem.getAttribute("generator").equals("spiral")) {
          tmpl.noiseGenerator = NoiseGenerator.Spiral;
        } else {
          throw new TemplateException("Unknown generator type: " + elem.getAttribute("generator"));
        }

        tmpl.warpFactor = 0.1;
        if (elem.getAttribute("factor") != null && elem.getAttribute("factor").length() > 0) {
          tmpl.warpFactor = Double.parseDouble(elem.getAttribute("factor"));
        }

        for (Element child : XmlIterator.childElements(elem)) {
          tmpl.getParameters().add(parseElement(child));
        }

        return tmpl;
      }
    }
  }
}

