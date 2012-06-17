package au.com.codeka.planetrender;

import java.util.Random;

/**
 * This class will generate an atmosphere around a planet.
 */
public class Atmosphere {
    private ColourGradient mInnerColourGradient;
    private ColourGradient mOuterColourGradient;
    private double mPlanetRadius;

    public Atmosphere(Template.AtmosphereTemplate tmpl, Random rand) {
        if (tmpl.getInnerTemplate() != null) {
            mInnerColourGradient = tmpl.getInnerTemplate().getParameter(
                    Template.ColourGradientTemplate.class).getColourGradient();
        }
        if (tmpl.getOuterTemplate() != null) {
            mOuterColourGradient = tmpl.getOuterTemplate().getParameter(
                    Template.ColourGradientTemplate.class).getColourGradient();
        }
    }

    /**
     * Sets the "apparent" radius of the planet, in the final image. A radius of 0.5 would mean
     * the planet is touching the edges of the image. A radius of 0.25 would imply it's taking
     * up half of the image.
     */
    public void setPlanetRadius(double radius) {
        mPlanetRadius = radius;
    }

    public Colour getOuterPixelColour(double distanceToSurface) {
        if (mOuterColourGradient == null) {
            return Colour.TRANSPARENT;
        }

        return mOuterColourGradient.getColour(distanceToSurface);
    }

    public Colour getInnerPixelColour(Vector3 pt, Vector3 normal, Vector3 sunDirection) {
        if (mInnerColourGradient == null) {
            return Colour.TRANSPARENT;
        }

        Vector3 cameraDirection = Vector3.subtract(new Vector3(0, 0, 0), pt).normalized();
        double dot = Vector3.dot(cameraDirection, normal);

        return mInnerColourGradient.getColour(1.0 - dot);
    }
}
