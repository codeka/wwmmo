package au.com.codeka.warworlds.model;

import java.util.Random;

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
    public Sprite getSprite(Star star, int size, boolean forceGeneric) {
        StarExtra starExtra = new StarExtra(star);
        Sprite sprite = null;
        if (size > 0 && !forceGeneric) {
            sprite = getSprite(star.getKey(), size, starExtra);
        }

        if (sprite == null) {
            double realSize = size * star.getStarType().getImageScale();
            String spriteName = "star."+(realSize > 250 ? "big." : "small.")+star.getStarType().getInternalName();
            sprite = SpriteManager.i.getSprite(spriteName);
            if (sprite == null) {
                throw new RuntimeException("No such sprite: "+spriteName);
            }

            if (sprite.getNumFrames() > 1) {
                int frameNo = new Random(star.hashCode()).nextInt(sprite.getNumFrames());
                sprite = sprite.extractFrame(frameNo);
            }
        }

        return sprite;
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    protected Template getTemplate(Object extra) {
        StarExtra starExtra = (StarExtra) extra;
        return loadTemplate(starExtra.star.getStarType().getBitmapBasePath(), starExtra.star.getKey());
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
        public Star star;

        public StarExtra(Star star) {
            this.star = star;
        }
    }
}
