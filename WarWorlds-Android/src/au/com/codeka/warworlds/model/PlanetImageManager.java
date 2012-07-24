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
        return getBitmap(context, key, 100, getTemplate(context, planet), planet);
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    private static Template getTemplate(Context context, Planet planet) {
        String key = String.format("%s-%d", planet.getStar().getKey(), planet.getIndex());
        return loadTemplate(context, planet.getPlanetType().getBitmapBasePath(), key);
    }

    /**
     * We want to adjust the sun position so that shadows look correct when the planet is
     * drawn on the SolarSystem page.
     */
    @Override
    protected Vector3 getSunDirection(Object extra) {
        Planet planet = (Planet) extra;
        int numPlanets = planet.getStar().getNumPlanets();
        float angle = (0.5f/(numPlanets + 1));
        angle = (float) ((angle*planet.getIndex()*Math.PI) + angle*Math.PI);

        Vector3 sunDirection = Vector3.pool.borrow().reset(0.0, 1.0, -1.0);
        sunDirection.rotateZ(angle);
        sunDirection.scale(200.0);
        return sunDirection;
    }

    /**
     * Gets the size we render the planet as.
     */
    protected double getPlanetSize(Object extra) {
        Planet planet = (Planet) extra;
        return ((planet.getSize() - 10.0) / 8.0) + 4.0;
    }
}
