package au.com.codeka.planetrender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This is actually a very simple ray-tracing engine. The simplicity comes from the fact that
 * we assume there's only one object in the scene (the planet) and one light source (the sun).
 */
public class PlanetRenderer {

    private double mPlanetRadius;
    private Vector3 mPlanetOrigin;
    private double mAmbient;
    private Vector3 mSunOrigin;
    private TextureGenerator mTexture;
    private Vector3 mNorth;
    private List<Atmosphere> mAtmospheres;

    public PlanetRenderer(Template.PlanetTemplate tmpl, Random rand) {
        mPlanetOrigin = new Vector3(0.0, 0.0, 30.0);
        mTexture = new TextureGenerator(tmpl.getParameter(Template.TextureTemplate.class), rand);
        mSunOrigin = tmpl.getSunLocation();
        mAmbient = tmpl.getAmbient();
        mPlanetRadius = tmpl.getPlanetSize();

        List<Template.AtmosphereTemplate> atmosphereTemplates = tmpl.getParameters(Template.AtmosphereTemplate.class);
        if (atmosphereTemplates != null && atmosphereTemplates.size() > 0) {
            mAtmospheres = new ArrayList<Atmosphere>();
            for (Template.AtmosphereTemplate atmosphereTemplate : atmosphereTemplates) {
                mAtmospheres.add(new Atmosphere(atmosphereTemplate, rand));
            }
        }

        Vector3 northFrom = tmpl.getNorthFrom();
        Vector3 northTo = tmpl.getNorthTo();
        mNorth = Vector3.interpolate(northFrom, northTo, rand.nextDouble()).normalized();
    }

    /**
     * Renders a planet into the given \c Image.
     */
    public void render(Image img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                double nx = ((double) x / (double) img.getWidth()) - 0.5;
                double ny = ((double) y / (double) img.getHeight()) - 0.5;
                Colour c = getPixelColour(nx, ny);
                img.setPixelColour(x, y, c);
            }
        }
    }

    /**
     * Computes the colour of the pixel at (x,y) where each coordinate is defined to
     * be in the range (-0.5, +0.5).
     * 
     * @param x The x-coordinate, between -0.5 and +0.5.
     * @param y The y-coordinate, between -0.5 and +0.5.
     * @return The colour at the given pixel.
     */
    private Colour getPixelColour(double x, double y) {
        Colour c = new Colour();

        Vector3 ray = new Vector3(x, -y, 1.0).normalized();
        Vector3 intersection = raytrace(ray);
        if (intersection != null) {
            // we intersected with the planet. Now we need to work out the colour at this point
            // on the planet.
            Colour t = queryTexture(intersection);

            double intensity = lightSphere(intersection);
            c.setAlpha(1.0);
            c.setRed(t.getRed() * intensity);
            c.setGreen(t.getGreen() * intensity);
            c.setBlue(t.getBlue() * intensity);

            if (mAtmospheres != null) {
                Vector3 surfaceNormal = Vector3.subtract(intersection, mPlanetOrigin).normalized();
                Vector3 sunDirection = Vector3.subtract(mSunOrigin, intersection).normalized();
                for (Atmosphere atmosphere : mAtmospheres) {
                    Colour atmosphereColour = atmosphere.getInnerPixelColour(x + 0.5, y + 0.5,
                                                                             intersection,
                                                                             surfaceNormal,
                                                                             sunDirection);
                    c = Colour.blend(c, atmosphereColour);
                }
            }
        } else if (mAtmospheres != null) {
            // if we're rendering an atmosphere, we need to work out the distance of this ray
            // to the planet's surface
            double u = Vector3.dot(mPlanetOrigin, ray);
            Vector3 closest = Vector3.scale(ray, u);
            double distance = (Vector3.subtract(closest, mPlanetOrigin).length() - mPlanetRadius);

            Vector3 normal = Vector3.subtract(closest, mPlanetOrigin).normalized();
            Vector3 sunDirection = Vector3.subtract(mSunOrigin, closest).normalized();

            for (Atmosphere atmosphere : mAtmospheres) {
                Colour atmosphereColour = atmosphere.getOuterPixelColour(x + 0.5, y + 0.5, normal,
                                                                         distance,
                                                                         sunDirection);
                c = Colour.blend(c, atmosphereColour);
            }
        }

        return c;
    }

    /**
     * Query the texture for the colour at the given intersection (in 3D space).
     */
    private Colour queryTexture(Vector3 intersection) {
        intersection = Vector3.subtract(intersection, mPlanetOrigin);

        Vector3 Vn = mNorth.normalized();
        Vector3 Ve = Vector3.cross(Vn, new Vector3(0.0, 0.0, 1.0)).normalized();
        Vector3 Vp = intersection.normalized();

        double phi = Math.acos(-1.0 * Vector3.dot(Vn, Vp));
        double v = phi / Math.PI;

        double theta = (Math.acos(Vector3.dot(Vp, Ve) / Math.sin(phi))) / (Math.PI * 2.0);
        double u;
        if (Vector3.dot(Vector3.cross(Vn,  Ve), Vp) > 0) {
            u = theta;
        } else {
            u = 1.0 - theta;
        }

        return mTexture.getTexel(u, v);
    }

    /**
     * Calculates light intensity from the sun.
     * 
     * @param intersection Point where the ray we're currently tracing intersects with the planet.
     */
    private double lightSphere(Vector3 intersection) {
        Vector3 surfaceNormal = Vector3.subtract(intersection, mPlanetOrigin).normalized();

        double intensity = diffuse(surfaceNormal, intersection);
        return Math.max(mAmbient, Math.min(1.0, intensity));
    }

    /**
     * Calculates diffuse lighting at the given point with the given normal.
     *
     * @param normal Normal at the point we're calculating diffuse lighting for.
     * @param point The point at which we're calculating diffuse lighting.
     * @return The diffuse factor of lighting.
     */
    private double diffuse(Vector3 normal, Vector3 point) {
        Vector3 directionToLight = Vector3.subtract(mSunOrigin, point).normalized();
        return Vector3.dot(normal, directionToLight);
    }

    /**
     * Traces a ray along the given direction. We assume the origin is (0,0,0) (i.e. the eye).
     * 
     * @param direction The direction of the ray we're going to trace.
     * @return A \c Vector3 representing the point in space where we intersect with the planet,
     * or \c null if there's no intersection.
     */
    private Vector3 raytrace(Vector3 direction) {
        // intsection of a sphere and a line
        final double a = Vector3.dot(direction, direction);
        final double b = 2.0 * Vector3.dot(direction, Vector3.scale(mPlanetOrigin, -1.0));
        final double c = Vector3.dot(mPlanetOrigin, mPlanetOrigin) - (mPlanetRadius * mPlanetRadius);
        final double d = (b * b) - (4.0 * a * c);

        if (d > 0.0) {
            double sign = (c < -0.00001) ? 1.0 : -1.0;
            double distance = (-b + (sign * Math.sqrt(d))) / (2.0 * a);
            return direction.scaled(distance);
        } else {
            return null;
        }
    }
}
