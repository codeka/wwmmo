package au.com.codeka.warworlds.model;

import java.util.Random;

import au.com.codeka.common.Vector3;
import au.com.codeka.common.model.Star;
import au.com.codeka.planetrender.Template;

public class StarImageManager extends ImageManager {
    private static StarImageManager sInstance = new StarImageManager();

    public static StarImageManager getInstance() {
        return sInstance;
    }

    /**
     * Gets the \c Bitmap for the given star.
     */
    public Sprite getSprite(Star starSummary, int size, boolean forceGeneric) {
        StarExtra starExtra = new StarExtra(starSummary);
        Sprite sprite = null;
        if (size > 0 && !forceGeneric) {
            sprite = getSprite(starSummary.key, size, starExtra);
        }

        if (sprite == null) {
            StarType starType = StarType.get(starSummary);
            double realSize = size * starType.getImageScale();
            String spriteName = "star."+(realSize > 250 ? "big." : "small.")+starType.getInternalName();
            sprite = SpriteManager.i.getSprite(spriteName);

            if (sprite.getNumFrames() > 1) {
                int frameNo = new Random(starSummary.hashCode()).nextInt(sprite.getNumFrames());
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
        StarType starType = StarType.get(starExtra.starSummary);
        return loadTemplate(starType.getBitmapBasePath(), starExtra.starSummary.key);
    }

    @Override
    protected Vector3 getSunDirection(Object extra) {
        // this is Vector3(100, -100, -100) normalized.
        return Vector3.pool.borrow().reset(0.577350268, -0.577350268, -0.577350268);
    }

    @Override
    protected double getPlanetSize(Object extra) {
        StarExtra starExtra = (StarExtra) extra;
        StarType starType = StarType.get(starExtra.starSummary);
        return starType.getBaseSize();
    }

    private static class StarExtra {
        public Star starSummary;

        public StarExtra(Star starSummary) {
            this.starSummary = starSummary;
        }
    }
}
