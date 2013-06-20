package au.com.codeka.warworlds.model;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import au.com.codeka.common.Vector3;
import au.com.codeka.planetrender.Template;

public class StarImageManager extends ImageManager {
    private static StarImageManager sInstance = new StarImageManager();

    public static StarImageManager getInstance() {
        return sInstance;
    }

    /**
     * Gets the \c Bitmap for the given star.
     */
    public Sprite getSprite(Context context, StarSummary star, int size, boolean forceGeneric) {
        StarExtra starExtra = new StarExtra(context, star);
        Bitmap bmp = null;
        if (size > 0 && !forceGeneric) {
            bmp = getBitmap(context, star.getKey(), size, starExtra);
        }

        if (bmp == null) {
            double realSize = size * star.getStarType().getImageScale();
            String spriteName = "star."+(realSize > 250 ? "big." : "small.")+star.getStarType().getInternalName();
            Sprite s = SpriteManager.i.getSprite(spriteName);

            if (s.getNumFrames() <= 1) {
                return s;
            }

            int frameNo = new Random(star.hashCode()).nextInt(s.getNumFrames());
            s = s.extractFrame(frameNo);
            return s;
        } else {
            return Sprite.createSimpleSprite(bmp);
        }
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    protected Template getTemplate(Object extra) {
        StarExtra starExtra = (StarExtra) extra;
        return loadTemplate(starExtra.context,
                             starExtra.star.getStarType().getBitmapBasePath(),
                             starExtra.star.getKey());
    }

    @Override
    protected Vector3 getSunDirection(Object extra) {
        // this is Vector3(100, -100, -100) normalized.
        return Vector3.pool.borrow().reset(0.577350268, -0.577350268, -0.577350268);
    }

    @Override
    protected double getPlanetSize(Object extra) {
        StarExtra starExtra = (StarExtra) extra;
        return starExtra.star.getStarType().getBaseSize();
    }

    private static class StarExtra {
        public StarSummary star;
        public Context context;

        public StarExtra(Context context, StarSummary star) {
            this.context = context;
            this.star = star;
        }
    }
}
