package au.com.codeka.planetrender;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * A template is used to create an image (usually a complete planet, but it doesn't have to be).
 */
public class Template {
    private BaseTemplate mData;
    private int mVersion;
    private String mName;

    public BaseTemplate getTemplate() {
        return mData;
    }

    public int getTemplateVersion() {
        return mVersion;
    }

    /*
     * The name of the template is whatever you want it to be. We don't care...
     */
    public String getName() {
        return mName;
    }
    public void setName(String name) {
        mName = name;
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
        } catch (ParserConfigurationException e) {
            throw new TemplateException(e);
        } catch (IllegalStateException e) {
            throw new TemplateException(e);
        } catch (SAXException e) {
            throw new TemplateException(e);
        } catch (IOException e) {
            throw new TemplateException(e);
        }

        Template tmpl = new Template();
        String version = xmldoc.getDocumentElement().getAttribute("version");
        if (version != null && version.length() > 0) {
            tmpl.mVersion = Integer.parseInt(version);
        } else {
            tmpl.mVersion = 1;
        }
        tmpl.mData = parseElement(xmldoc.getDocumentElement());
        return tmpl;
    }

    private static BaseTemplate parseElement(Element elem) throws TemplateException {
        TemplateFactory factory = null;
        if (elem.getTagName().equals("planet")) {
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
        } else {
            throw new TemplateException("Unknown element: "+elem.getTagName());
        }

        return (BaseTemplate) factory.parse(elem);
    }

    private static abstract class TemplateFactory {
        public abstract BaseTemplate parse(Element elem) throws TemplateException;

        protected Vector3 parseVector3(String val) throws TemplateException {
            String[] parts = val.split(" ");
            if (parts.length == 3) {
                return Vector3.pool.borrow().reset(Double.parseDouble(parts[0]),
                                                    Double.parseDouble(parts[1]),
                                                    Double.parseDouble(parts[2]));
            } else {
                throw new TemplateException("Invalid vector: "+val);
            }
        }
    }

    public static class BaseTemplate {
        private List<BaseTemplate> mParameters;

        public BaseTemplate() {
            mParameters = new ArrayList<BaseTemplate>();
        }

        public List<BaseTemplate> getParameters() {
            return mParameters;
        }

        @SuppressWarnings("unchecked")
        public <T extends BaseTemplate> List<T> getParameters(Class<T> classFactory) {
            List<T> params = new ArrayList<T>();
            for (BaseTemplate bt : mParameters) {
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

    public static class PlanetTemplate extends BaseTemplate {
        private Vector3 mNorthFrom;
        private Vector3 mNorthTo;
        private double mPlanetSize;
        private double mAmbient;
        private Vector3 mSunLocation;

        public Vector3 getNorthFrom() {
            return mNorthFrom;
        }
        public Vector3 getNorthTo() {
            return mNorthTo;
        }
        public double getPlanetSize() {
            return mPlanetSize;
        }
        public double getAmbient() {
            return mAmbient;
        }
        public Vector3 getSunLocation() {
            return mSunLocation;
        }

        public void setPlanetSize(double size) {
            mPlanetSize = size;
        }
        public void setSunLocation(Vector3 location) {
            mSunLocation = location;
        }

        private static class PlanetTemplateFactory extends TemplateFactory {
            /**
             * Parses a <planet> node and returns the corresponding \c PlanetTemplate.
             */
            @Override
            public BaseTemplate parse(Element elem) throws TemplateException {
                PlanetTemplate tmpl = new PlanetTemplate();

                tmpl.mNorthFrom = Vector3.pool.borrow().reset(0.0, 1.0, 0.0);
                tmpl.mNorthTo = Vector3.pool.borrow().reset(0.0, 1.0, 0.0);
                if (elem.getAttribute("northFrom") != null && !elem.getAttribute("northFrom").equals("")) {
                    Vector3 other = parseVector3(elem.getAttribute("northFrom"));
                    tmpl.mNorthFrom.reset(other);
                    Vector3.pool.release(other);
                }
                if (elem.getAttribute("northTo") != null && !elem.getAttribute("northTo").equals("")) {
                    Vector3 other = parseVector3(elem.getAttribute("northTo"));
                    tmpl.mNorthTo.reset(other);
                    Vector3.pool.release(other);
                }

                tmpl.mPlanetSize = 10.0;
                if (elem.getAttribute("size") != null && !elem.getAttribute("size").equals("")) {
                    tmpl.mPlanetSize = Double.parseDouble(elem.getAttribute("size"));
                }

                tmpl.mAmbient = 0.1;
                if (elem.getAttribute("ambient") != null && !elem.getAttribute("ambient").equals("")) {
                    tmpl.mAmbient = Double.parseDouble(elem.getAttribute("ambient"));
                }

                tmpl.mSunLocation = Vector3.pool.borrow().reset(100.0, 100.0, -150.0);
                if (elem.getAttribute("sun") != null && !elem.getAttribute("sun").equals("")) {
                    Vector3 other = parseVector3(elem.getAttribute("sun"));
                    tmpl.mSunLocation.reset(other);
                    Vector3.pool.release(other);
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

        private Generator mGenerator;
        private double mNoisiness;
        private double mScaleX;
        private double mScaleY;

        public Generator getGenerator() {
            return mGenerator;
        }
        public double getNoisiness() {
            return mNoisiness;
        }
        public double getScaleX() {
            return mScaleX;
        }
        public double getScaleY() {
            return mScaleY;
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
                    tmpl.mGenerator = Generator.VoronoiMap;
                } else if (generator.equals("perlin-noise")) {
                    tmpl.mGenerator = Generator.PerlinNoise;
                } else {
                    throw new TemplateException("Unknown <texture generator> attribute: "+generator);
                }

                String noisiness = elem.getAttribute("noisiness");
                if (noisiness == null || noisiness.equals("")) {
                    tmpl.mNoisiness = 0.5;
                } else {
                    tmpl.mNoisiness = Double.parseDouble(noisiness);
                }

                tmpl.mScaleX = 1.0;
                tmpl.mScaleY = 1.0;
                if (elem.getAttribute("scaleX") != null && !elem.getAttribute("scaleX").equals("")) {
                    tmpl.mScaleX = Double.parseDouble(elem.getAttribute("scaleX"));
                }
                if (elem.getAttribute("scaleY") != null && !elem.getAttribute("scaleY").equals("")) {
                    tmpl.mScaleY = Double.parseDouble(elem.getAttribute("scaleY"));
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

        private Generator mGenerator;
        private double mDensity;
        private double mRandomness;

        public Generator getGenerator() {
            return mGenerator;
        }
        public double getDensity() {
            return mDensity;
        }
        public double getRandomness() {
            return mRandomness;
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
                    tmpl.mGenerator = PointCloudTemplate.Generator.Random;
                } else if (val.equals("poisson")) {
                    tmpl.mGenerator = PointCloudTemplate.Generator.Poisson;
                } else {
                    throw new TemplateException("Unknown <point-cloud> 'generator' attribute: "+val);
                }

                tmpl.mDensity = Double.parseDouble(elem.getAttribute("density"));
                tmpl.mRandomness = Double.parseDouble(elem.getAttribute("randomness"));
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
                    tmpl.mColourGradient.addNode(n, Colour.pool.borrow().reset(argb));
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

        private double mPersistence;
        private Interpolation mInterpolation;
        private int mStartOctave;
        private int mEndOctave;

        public double getPersistence() {
            return mPersistence;
        }
        public Interpolation getInterpolation() {
            return mInterpolation;
        }
        public int getStartOctave() {
            return mStartOctave;
        }
        public int getEndOctave() {
            return mEndOctave;
        }

        private static class PerlinNoiseTemplateFactory extends TemplateFactory {
            @Override
            public BaseTemplate parse(Element elem) throws TemplateException {
                PerlinNoiseTemplate tmpl = new PerlinNoiseTemplate();

                String val = elem.getAttribute("interpolation");
                if (val == null || val.equals("linear")) {
                    tmpl.mInterpolation = Interpolation.Linear;
                } else if (val.equals("cosine")) {
                    tmpl.mInterpolation = Interpolation.Cosine;
                } else if (val.equals("none")) {
                    tmpl.mInterpolation = Interpolation.None;
                } else {
                    throw new TemplateException("Unknown <perlin> 'interpolation' attribute: " + val);
                }

                tmpl.mPersistence = Double.parseDouble(elem.getAttribute("persistence"));
                if (elem.getAttribute("startOctave") == null) {
                    tmpl.mStartOctave = 1;
                } else {
                    tmpl.mStartOctave = Integer.parseInt(elem.getAttribute("startOctave"));
                }
                if (elem.getAttribute("endOctave") == null) {
                    tmpl.mEndOctave = 10;
                } else {
                    tmpl.mEndOctave = Integer.parseInt(elem.getAttribute("endOctave"));
                }

                return tmpl;
            }
        }
    }

    public static class AtmosphereTemplate extends BaseTemplate {
        private InnerOuterTemplate mInnerTemplate;
        private InnerOuterTemplate mOuterTemplate;

        public InnerOuterTemplate getInnerTemplate() {
            return mInnerTemplate;
        }
        public InnerOuterTemplate getOuterTemplate() {
            return mOuterTemplate;
        }

        public static class InnerOuterTemplate extends BaseTemplate {
            private double mSunStartShadow;
            private double mSunShadowFactor;
            private double mSize;
            private double mNoisiness;

            public double getSunStartShadow() {
                return mSunStartShadow;
            }
            public double getSunShadowFactor() {
                return mSunShadowFactor;
            }
            public double getSize() {
                return mSize;
            }
            public double getNoisiness() {
                return mNoisiness;
            }
        }

        private static class AtmosphereTemplateFactory extends TemplateFactory {
            @Override
            public BaseTemplate parse(Element elem) throws TemplateException {
                AtmosphereTemplate tmpl = new AtmosphereTemplate();

                for (Element child : XmlIterator.childElements(elem)) {
                    if (child.getTagName().equals("inner")) {
                        tmpl.mInnerTemplate = parseInnerOuterTemplate(child);
                    } else if (child.getTagName().equals("outer")) {
                        tmpl.mOuterTemplate = parseInnerOuterTemplate(child);
                    }
                }

                return tmpl;
            }

            private InnerOuterTemplate parseInnerOuterTemplate(Element elem)
                    throws TemplateException {
                InnerOuterTemplate tmpl = new InnerOuterTemplate();

                if (elem.getAttribute("sunStartShadow") != null && !elem.getAttribute("sunStartShadow").equals("")) {
                    tmpl.mSunStartShadow = Double.parseDouble(elem.getAttribute("sunStartShadow"));
                }

                if (elem.getAttribute("sunShadowFactor") != null && !elem.getAttribute("sunShadowFactor").equals("")) {
                    tmpl.mSunShadowFactor = Double.parseDouble(elem.getAttribute("sunShadowFactor"));
                }

                tmpl.mSize = 1.0;
                if (elem.getAttribute("size") != null && !elem.getAttribute("size").equals("")) {
                    tmpl.mSize = Double.parseDouble(elem.getAttribute("size"));
                }

                if (elem.getAttribute("noisiness") != null && !elem.getAttribute("noisiness").equals("")) {
                    tmpl.mNoisiness = Double.parseDouble(elem.getAttribute("noisiness"));
                }

                for (Element child : XmlIterator.childElements(elem)) {
                    tmpl.getParameters().add(parseElement(child));
                }

                return tmpl;
            }
        }
    }
}

