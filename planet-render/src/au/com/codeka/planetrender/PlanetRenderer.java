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
        mPlanetOrigin = Vector3.pool.borrow().reset(0.0, 0.0, 30.0);
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

        mNorth = Vector3.pool.borrow().reset(tmpl.getNorthFrom());
        Vector3.interpolate(mNorth, tmpl.getNorthTo(), rand.nextDouble());
        mNorth.normalize();
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
                Colour.pool.release(c);
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
        Colour c = Colour.pool.borrow().reset(Colour.TRANSPARENT);

        Vector3 ray = Vector3.pool.borrow().reset(x, -y, 1.0);
        ray.normalize();

        Vector3 intersection = raytrace(ray);
        if (intersection != null) {
            // we intersected with the planet. Now we need to work out the colour at this point
            // on the planet.
            Colour t = queryTexture(intersection);
            double intensity = lightSphere(intersection);
            c.reset(1.0, t.r * intensity, t.g * intensity, t.b * intensity);
            Colour.pool.release(t);

            if (mAtmospheres != null) {
                Vector3 surfaceNormal = Vector3.pool.borrow().reset(intersection);
                surfaceNormal.subtract(mPlanetOrigin);
                surfaceNormal.normalize();

                Vector3 sunDirection = Vector3.pool.borrow().reset(mSunOrigin);
                sunDirection.subtract(intersection);
                sunDirection.normalize();

                final int numAtmospheres = mAtmospheres.size();
                for (int i = 0; i < numAtmospheres; i++) {
                    final Atmosphere atmosphere = mAtmospheres.get(i);
                    Colour atmosphereColour = atmosphere.getInnerPixelColour(x + 0.5, y + 0.5,
                                                                             intersection,
                                                                             surfaceNormal,
                                                                             sunDirection);
                    Colour.blend(c, atmosphereColour);
                    Colour.pool.release(atmosphereColour);
                }

                Vector3.pool.release(surfaceNormal);
                Vector3.pool.release(sunDirection);
            }
        } else if (mAtmospheres != null) {
            // if we're rendering an atmosphere, we need to work out the distance of this ray
            // to the planet's surface
            double u = Vector3.dot(mPlanetOrigin, ray);
            Vector3 closest = Vector3.pool.borrow().reset(ray);
            closest.scale(u);

            double distance = (Vector3.distanceBetween(closest, mPlanetOrigin) - mPlanetRadius);

            Vector3 surfaceNormal = Vector3.pool.borrow().reset(closest);
            surfaceNormal.subtract(mPlanetOrigin);
            surfaceNormal.normalize();

            Vector3 sunDirection = Vector3.pool.borrow().reset(mSunOrigin);
            sunDirection.subtract(closest);
            sunDirection.normalize();

            final int numAtmospheres = mAtmospheres.size();
            for (int i = 0; i < numAtmospheres; i++) {
                final Atmosphere atmosphere = mAtmospheres.get(i);
                Colour atmosphereColour = atmosphere.getOuterPixelColour(x + 0.5, y + 0.5,
                                                                         surfaceNormal,
                                                                         distance,
                                                                         sunDirection);
                Colour.blend(c, atmosphereColour);
                Colour.pool.release(atmosphereColour);
            }

            Vector3.pool.release(closest);
            Vector3.pool.release(surfaceNormal);
            Vector3.pool.release(sunDirection);
        }

        Vector3.pool.release(ray);
        Vector3.pool.release(intersection);
        return c;
    }

    /**
     * Query the texture for the colour at the given intersection (in 3D space).
     */
    private Colour queryTexture(Vector3 intersection) {
        Vector3 Vn = mNorth;
        Vector3 Ve = Vector3.pool.borrow().reset(Vn.y, -Vn.x, 0.0); // (AKA Vn.cross(0, 0, 1))
        Vector3 Vp = Vector3.pool.borrow().reset(intersection);
        Vp.subtract(mPlanetOrigin);

        Ve.normalize();
        Vp.normalize();

        double phi = Math.acos(-1.0 * Vector3.dot(Vn, Vp));
        double v = phi / Math.PI;

        double theta = (Math.acos(Vector3.dot(Vp, Ve) / Math.sin(phi))) / (Math.PI * 2.0);
        double u;

        Vector3 c = Vector3.cross(Vn, Ve);
        if (Vector3.dot(c, Vp) > 0) {
            u = theta;
        } else {
            u = 1.0 - theta;
        }

        Vector3.pool.release(c);
        Vector3.pool.release(Ve);
        Vector3.pool.release(Vp);

        return mTexture.getTexel(u, v);
    }

    /**
     * Calculates light intensity from the sun.
     * 
     * @param intersection Point where the ray we're currently tracing intersects with the planet.
     */
    private double lightSphere(Vector3 intersection) {
        Vector3 surfaceNormal = Vector3.pool.borrow().reset(intersection);
        surfaceNormal.subtract(mPlanetOrigin);
        surfaceNormal.normalize();

        double intensity = diffuse(surfaceNormal, intersection);
        intensity = Math.max(mAmbient, Math.min(1.0, intensity));

        Vector3.pool.release(surfaceNormal);
        return intensity;
    }

    /**
     * Calculates diffuse lighting at the given point with the given normal.
     *
     * @param normal Normal at the point we're calculating diffuse lighting for.
     * @param point The point at which we're calculating diffuse lighting.
     * @return The diffuse factor of lighting.
     */
    private double diffuse(Vector3 normal, Vector3 point) {
        Vector3 directionToLight = Vector3.pool.borrow().reset(mSunOrigin);
        directionToLight.subtract(point);
        directionToLight.normalize();

        double factor =Vector3.dot(normal, directionToLight);
        Vector3.pool.release(directionToLight);
        return factor;
    }

    /**
     * Traces a ray along the given direction. We assume the origin is (0,0,0) (i.e. the eye).
     * 
     * @param direction The direction of the ray we're going to trace.
     * @return A \c Vector3 representing the point in space where we intersect with the planet,
     * or \c null if there's no intersection.
     */
    private Vector3 raytrace(Vector3 direction) {
        // intersection of a sphere and a line
        final double a = Vector3.dot(direction, direction);
        Vector3 blah = Vector3.pool.borrow().reset(mPlanetOrigin);
        blah.scale(-1);
        final double b = 2.0 * Vector3.dot(direction, blah);
        Vector3.pool.release(blah);
        final double c = Vector3.dot(mPlanetOrigin, mPlanetOrigin) - (mPlanetRadius * mPlanetRadius);
        final double d = (b * b) - (4.0 * a * c);

        if (d > 0.0) {
            double sign = (c < -0.00001) ? 1.0 : -1.0;
            double distance = (-b + (sign * Math.sqrt(d))) / (2.0 * a);
            Vector3 intersection = Vector3.pool.borrow().reset(direction);
            intersection.scale(distance);
            return  intersection;
        } else {
            return null;
        }
    }
}
