package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import au.com.codeka.common.Colour;
import au.com.codeka.common.ColourGradient;
import au.com.codeka.common.PerlinNoise;
import au.com.codeka.common.Vector3;

/**
 * This class will generate an atmosphere around a planet.
 */
public class Atmosphere {

    protected Atmosphere() {
    }

    public static List<Atmosphere> getAtmospheres(Template.AtmosphereTemplate tmpl, Random rand) {
        ArrayList<Atmosphere> atmospheres = new ArrayList<Atmosphere>();
        getAtmospheres(atmospheres, tmpl, rand);
        return atmospheres;
    }

    public static void getAtmospheres(List<Atmosphere> atmospheres,
                                        Template.AtmosphereTemplate tmpl,
                                        Random rand) {
        if (tmpl.getInnerTemplate() != null) {
            atmospheres.add(new InnerAtmosphere(tmpl.getInnerTemplate(), rand));
        }
        if (tmpl.getOuterTemplate() != null) {
            atmospheres.add(new OuterAtmosphere(tmpl.getOuterTemplate(), rand));
        }
        if (tmpl.getStarTemplate() != null) {
            atmospheres.add(new StarAtmosphere(tmpl.getStarTemplate(), rand));
        }
    }

    public Template.AtmosphereTemplate.BlendMode getBlendMode() {
        return Template.AtmosphereTemplate.BlendMode.Alpha;
    }

    public Colour getOuterPixelColour(double u, double v, Vector3 normal,
                                       double distanceToSurface, Vector3 sunDirection,
                                       Vector3 north) {
        return Colour.pool.borrow().reset(Colour.TRANSPARENT);
    }

    public Colour getInnerPixelColour(double u, double v, Vector3 pt,
            Vector3 normal, Vector3 sunDirection, Vector3 north) {
        return Colour.pool.borrow().reset(Colour.TRANSPARENT);
    }

    protected static double getSunShadowFactor(double dot, double sunStartShadow,
                                                  double sunFactor) {
        if (dot < 0.0) {
            dot = Math.abs(dot);

            // normally, the dot product will be 1.0 if we're on the exact opposite side of
            // the planet to the sun, and 0.0 when we're 90 degrees to the sun. We want to swap
            // that around.
            dot = 1.0 - dot;
        } else {
            // if it's positive, then it's on the sun side of the planet. We'll still allow you to
            // start chopping off the atmosphere on the sun side of the planet if you want.
            dot += 1.0;
        }

        if (dot < sunStartShadow) {
            final double min = sunStartShadow * sunFactor;
            if (dot < min) {
                dot = 0.0;
            } else {
                dot = (dot - min) / (sunStartShadow - min);
            }

            return dot;
        }

        return 1.0;
    }

    protected static double getNoiseFactor(double u, double v, PerlinNoise perlin,
                                              double noisiness) {
        double noise = perlin.getNoise(u, v);
        return 1.0 - (noise * noisiness);
    }

    public static class InnerAtmosphere extends Atmosphere {
        private ColourGradient mColourGradient;
        private double mSunShadowFactor;
        private double mSunStartShadow;
        private double mNoisiness;
        private Template.AtmosphereTemplate.BlendMode mBlendMode;
        private PerlinNoise mPerlin;

        public InnerAtmosphere(Template.AtmosphereTemplate.InnerOuterTemplate tmpl, Random rand) {
            mColourGradient = tmpl.getParameter(Template.ColourGradientTemplate.class).getColourGradient();
            mSunShadowFactor = tmpl.getSunShadowFactor();
            mSunStartShadow = tmpl.getSunStartShadow();

            Template.PerlinNoiseTemplate perlinTemplate = tmpl.getParameter(
                    Template.PerlinNoiseTemplate.class);
            if (perlinTemplate != null) {
                mPerlin = new TemplatedPerlinNoise(perlinTemplate, rand);
                mNoisiness = tmpl.getNoisiness();
            }
            mBlendMode = tmpl.getBlendMode();
        }

        @Override
        public Template.AtmosphereTemplate.BlendMode getBlendMode() {
            return mBlendMode;
        }

        @Override
        public Colour getInnerPixelColour(double u, double v, Vector3 pt, Vector3 normal,
                                           Vector3 sunDirection, Vector3 north) {
            if (mColourGradient == null) {
                return Colour.pool.borrow().reset(Colour.TRANSPARENT);
            }

            Vector3 cameraDirection = Vector3.pool.borrow().reset(0, 0, 0);
            cameraDirection.subtract(pt);
            cameraDirection.normalize();
            double dot = Vector3.dot(cameraDirection, normal);
            Vector3.pool.release(cameraDirection);

            Colour baseColour = mColourGradient.getColour(1.0 - dot);

            // if we've on the dark side of the planet, we'll want to factor in the shadow
            dot = Vector3.dot(normal, sunDirection);
            double sunFactor = getSunShadowFactor(dot, mSunStartShadow, mSunShadowFactor);
            baseColour.reset(baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b);

            if (mPerlin != null) {
                double noiseFactor = getNoiseFactor(u, v, mPerlin, mNoisiness);
                baseColour.reset(baseColour.a * noiseFactor,
                                 baseColour.r * noiseFactor,
                                 baseColour.g * noiseFactor,
                                 baseColour.b * noiseFactor);
            }

            return baseColour;
        }
    }

    public static class OuterAtmosphere extends Atmosphere {
        private ColourGradient mColourGradient;
        private double mSunShadowFactor;
        private double mSunStartShadow;
        private double mAtmosphereSize;
        private double mNoisiness;
        private PerlinNoise mPerlin;
        private Template.AtmosphereTemplate.BlendMode mBlendMode;

        public OuterAtmosphere(Template.AtmosphereTemplate.InnerOuterTemplate tmpl, Random rand) {
            mColourGradient = tmpl.getParameter(
                    Template.ColourGradientTemplate.class).getColourGradient();
            mSunShadowFactor = tmpl.getSunShadowFactor();
            mSunStartShadow = tmpl.getSunStartShadow();
            mAtmosphereSize = tmpl.getSize();

            Template.PerlinNoiseTemplate perlinTemplate = tmpl.getParameter(
                    Template.PerlinNoiseTemplate.class);
            if (perlinTemplate != null) {
                mPerlin = new TemplatedPerlinNoise(perlinTemplate, rand);
                mNoisiness = tmpl.getNoisiness();
            }
            mBlendMode = tmpl.getBlendMode();
        }

        @Override
        public Template.AtmosphereTemplate.BlendMode getBlendMode() {
            return mBlendMode;
        }

        @Override
        public Colour getOuterPixelColour(double u, double v, Vector3 normal,
                                           double distanceToSurface, Vector3 sunDirection,
                                           Vector3 north) {
            if (mColourGradient == null) {
                return Colour.pool.borrow().reset(Colour.TRANSPARENT);
            }

            distanceToSurface /= mAtmosphereSize;
            Colour baseColour = mColourGradient.getColour(distanceToSurface);

            double dot = Vector3.dot(normal, sunDirection);
            double sunFactor = getSunShadowFactor(dot, mSunStartShadow, mSunShadowFactor);
            baseColour.reset(baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b);

            if (mPerlin != null) {
                double noiseFactor = getNoiseFactor(u, v, mPerlin, mNoisiness);
                baseColour.reset(baseColour.a * noiseFactor,
                                 baseColour.r * noiseFactor,
                                 baseColour.g * noiseFactor,
                                 baseColour.b * noiseFactor);
            }

            return baseColour;
        }
    }

    public static class StarAtmosphere extends OuterAtmosphere {
        private int mNumPoints;
        private double mBaseWidth;
       // private double mSlope;

        public StarAtmosphere(Template.AtmosphereTemplate.StarTemplate tmpl, Random rand) {
            super(tmpl, rand);

            mNumPoints = tmpl.getNumPoints();
            mBaseWidth = tmpl.getBaseWidth();
          //  mSlope = tmpl.getSlope();
        }

        @Override
        public Colour getOuterPixelColour(double u, double v, Vector3 normal,
                                           double distanceToSurface, Vector3 sunDirection,
                                           Vector3 north) {
            Colour baseColour = super.getOuterPixelColour(u, v, normal,
                                                          distanceToSurface, sunDirection,
                                                          north);

            normal.z = 0;
            normal.normalize();
            double dot = Vector3.dot(north, normal);
            double angle = Math.acos(dot);

            double pointAngle = (Math.PI * 2.0) / (double) mNumPoints;
            while (angle > pointAngle) {
                angle -= pointAngle;
            }
            angle /= pointAngle;
            double distanceToPoint = angle;
            if (distanceToPoint > 0.5) {
                distanceToPoint = 1.0 - distanceToPoint;
            }
            distanceToPoint *= 2.0;

            distanceToPoint *= mBaseWidth;
            if (distanceToPoint > 1.0)
                distanceToPoint = 1.0;

            baseColour.reset(baseColour.a * (1.0 - distanceToPoint), baseColour.r, baseColour.g, baseColour.b);
            return baseColour;
        }
    }
}
