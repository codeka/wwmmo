package au.com.codeka.warworlds.model;

import android.content.Context;
import android.graphics.Bitmap;
import au.com.codeka.planetrender.Template;
import au.com.codeka.planetrender.Vector3;

public class StarImageManager extends ImageManager {
    private static StarImageManager sInstance = new StarImageManager();

    public static StarImageManager getInstance() {
        return sInstance;
    }

    /**
     * Gets the \c Bitmap for the given star.
     */
    public Bitmap getBitmap(Context context, Star star, int size) {
        return getBitmap(context, star.getKey(), size, getTemplate(context, star), star);
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    private static Template getTemplate(Context context, Star star) {
        return loadTemplate(context, star.getStarType().getBitmapBasePath(), star.getKey());
    }

    @Override
    protected Vector3 getSunDirection(Object extra) {
        // this is Vector3(100, -100, -100) normalized.
        return Vector3.pool.borrow().reset(0.577350268, -0.577350268, -0.577350268);
    }

    @Override
    protected double getPlanetSize(Object extra) {
        Star star = (Star) extra;
        return star.getStarType().getBaseSize();
    }

}
