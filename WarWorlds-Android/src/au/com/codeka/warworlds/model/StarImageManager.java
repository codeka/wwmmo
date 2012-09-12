package au.com.codeka.warworlds.model;

import java.util.Random;

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
    public Sprite getSprite(Context context, Star star, int size) {
        StarExtra starExtra = new StarExtra(context, star);
        Bitmap bmp = getBitmap(context, star.getKey(), size, starExtra);
        if (bmp == null) {
            SpriteManager spriteManager = SpriteManager.getInstance();

            String spriteName = "star."+(size > 250 ? "big." : "small.")+star.getStarType().getInternalName();
            Sprite s = spriteManager.getSprite(spriteName);

            if (s.getNumFrames() <= 1) {
                return s;
            }

            int frameNo = new Random(star.hashCode()).nextInt(s.getNumFrames());
            s = s.extractFrame(frameNo);
            s.setScale((float) getPlanetSize(starExtra) / 8.0f);
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
        public Star star;
        public Context context;

        public StarExtra(Context context, Star star) {
            this.context = context;
            this.star = star;
        }
    }
}
