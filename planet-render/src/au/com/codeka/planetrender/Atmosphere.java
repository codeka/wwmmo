package au.com.codeka.planetrender;

import java.util.Random;

/**
 * This class will generate an atmosphere around a planet.
 */
public class Atmosphere {
    private ColourGradient mInnerColourGradient;
    private double mInnerSunShadowFactor;
    private double mInnerSunStartShadow;
    private double mInnerNoisiness;
    private Template.AtmosphereTemplate.BlendMode mInnerBlendMode;
    private PerlinNoise mInnerPerlin;
    private ColourGradient mOuterColourGradient;
    private double mOuterSunShadowFactor;
    private double mOuterSunStartShadow;
    private double mOuterAtmosphereSize;
    private double mOuterNoisiness;
    private PerlinNoise mOuterPerlin;
    private Template.AtmosphereTemplate.BlendMode mOuterBlendMode;

    public Atmosphere(Template.AtmosphereTemplate tmpl, Random rand) {
        if (tmpl.getInnerTemplate() != null) {
            mInnerColourGradient = tmpl.getInnerTemplate().getParameter(
                    Template.ColourGradientTemplate.class).getColourGradient();
            mInnerSunShadowFactor = tmpl.getInnerTemplate().getSunShadowFactor();
            mInnerSunStartShadow = tmpl.getInnerTemplate().getSunStartShadow();

            Template.PerlinNoiseTemplate perlinTemplate = tmpl.getInnerTemplate().getParameter(
                    Template.PerlinNoiseTemplate.class);
            if (perlinTemplate != null) {
                mInnerPerlin = new PerlinNoise(perlinTemplate, rand);
                mInnerNoisiness = tmpl.getInnerTemplate().getNoisiness();
            }
            mInnerBlendMode = tmpl.getInnerTemplate().getBlendMode();
        }
        if (tmpl.getOuterTemplate() != null) {
            mOuterColourGradient = tmpl.getOuterTemplate().getParameter(
                    Template.ColourGradientTemplate.class).getColourGradient();
            mOuterSunShadowFactor = tmpl.getOuterTemplate().getSunShadowFactor();
            mOuterSunStartShadow = tmpl.getOuterTemplate().getSunStartShadow();
            mOuterAtmosphereSize = tmpl.getOuterTemplate().getSize();

            Template.PerlinNoiseTemplate perlinTemplate = tmpl.getOuterTemplate().getParameter(
                    Template.PerlinNoiseTemplate.class);
            if (perlinTemplate != null) {
                mOuterPerlin = new PerlinNoise(perlinTemplate, rand);
                mOuterNoisiness = tmpl.getOuterTemplate().getNoisiness();
            }
            mOuterBlendMode = tmpl.getOuterTemplate().getBlendMode();
        }
    }

    public Template.AtmosphereTemplate.BlendMode getInnerBlendMode() {
        return mInnerBlendMode;
    }

    public Template.AtmosphereTemplate.BlendMode getOuterBlendMode() {
        return mOuterBlendMode;
    }

    public Colour getOuterPixelColour(double u, double v, Vector3 normal, double distanceToSurface, Vector3 sunDirection) {
        if (mOuterColourGradient == null) {
            return Colour.pool.borrow().reset(Colour.TRANSPARENT);
        }

        distanceToSurface /= mOuterAtmosphereSize;
        Colour baseColour = mOuterColourGradient.getColour(distanceToSurface);

        double dot = Vector3.dot(normal, sunDirection);
        double sunFactor = getSunShadowFactor(dot, mOuterSunStartShadow, mOuterSunShadowFactor);
        baseColour.reset(baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b);

        if (mOuterPerlin != null) {
            double noiseFactor = getNoiseFactor(u, v, mOuterPerlin, mOuterNoisiness);
            baseColour.reset(baseColour.a * noiseFactor, baseColour.r, baseColour.g, baseColour.b);
        }

        return baseColour;
    }

    public Colour getInnerPixelColour(double u, double v, Vector3 pt, Vector3 normal, Vector3 sunDirection) {
        if (mInnerColourGradient == null) {
            return Colour.pool.borrow().reset(Colour.TRANSPARENT);
        }

        Vector3 cameraDirection = Vector3.pool.borrow().reset(0, 0, 0);
        cameraDirection.subtract(pt);
        cameraDirection.normalize();
        double dot = Vector3.dot(cameraDirection, normal);
        Vector3.pool.release(cameraDirection);

        Colour baseColour = mInnerColourGradient.getColour(1.0 - dot);

        // if we've on the dark side of the planet, we'll want to factor in the shadow
        dot = Vector3.dot(normal, sunDirection);
        double sunFactor = getSunShadowFactor(dot, mInnerSunStartShadow, mInnerSunShadowFactor);
        baseColour.reset(baseColour.a * sunFactor, baseColour.r, baseColour.g, baseColour.b);

        if (mInnerPerlin != null) {
            double noiseFactor = getNoiseFactor(u, v, mInnerPerlin, mInnerNoisiness);
            baseColour.reset(baseColour.a * noiseFactor, baseColour.r, baseColour.g, baseColour.b);
        }

        return baseColour;
    }

    private double getSunShadowFactor(double dot, double sunStartShadow, double sunFactor) {
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

    private double getNoiseFactor(double u, double v, PerlinNoise perlin, double noisiness) {
        double noise = perlin.getNoise(u, v);
        return 1.0 - (noise * noisiness);
    }
}
