package au.com.codeka.warworlds.model;

import android.content.Context;
import android.graphics.Bitmap;
import au.com.codeka.planetrender.Template;
import au.com.codeka.planetrender.Vector3;

/**
 * Manages images of planets, generates images on-the-fly and caches them as
 * required.
 */
public class PlanetImageManager extends ImageManager {
    private static PlanetImageManager sInstance = new PlanetImageManager();

    public static PlanetImageManager getInstance() {
        return sInstance;
    }

    /**
     * Gets the \c Bitmap for the given planet.
     */
    public Bitmap getBitmap(Context context, Planet planet) {
        String key = String.format("%s-%d", planet.getStar().getKey(), planet.getIndex());
        return getBitmap(context, key, 100, new PlanetExtra(context, planet));
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    @Override
    protected Template getTemplate(Object extra) {
        PlanetExtra planetExtra = (PlanetExtra) extra;
        String key = String.format("%s-%d",
                                   planetExtra.planet.getStar().getKey(),
                                   planetExtra.planet.getIndex());
        return loadTemplate(planetExtra.context,
                             planetExtra.planet.getPlanetType().getBitmapBasePath(),
                             key);
    }

    /**
     * We want to adjust the sun position so that shadows look correct when the planet is
     * drawn on the SolarSystem page.
     */
    @Override
    protected Vector3 getSunDirection(Object extra) {
        PlanetExtra planetExtra = (PlanetExtra) extra;
        int numPlanets = planetExtra.planet.getStar().getNumPlanets();
        float angle = (0.5f/(numPlanets + 1));
        angle = (float) ((angle*planetExtra.planet.getIndex()*Math.PI) + angle*Math.PI);

        Vector3 sunDirection = Vector3.pool.borrow().reset(0.0, 1.0, -1.0);
        sunDirection.rotateZ(angle);
        sunDirection.scale(200.0);
        return sunDirection;
    }

    /**
     * Gets the size we render the planet as.
     */
    protected double getPlanetSize(Object extra) {
        PlanetExtra planetExtra = (PlanetExtra) extra;
        return ((planetExtra.planet.getSize() - 10.0) / 8.0) + 4.0;
    }

    private static class PlanetExtra {
        public Planet planet;
        public Context context;

        public PlanetExtra(Context context, Planet planet) {
            this.context = context;
            this.planet = planet;
        }
    }

}
