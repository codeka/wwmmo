package au.com.codeka.warworlds.game.starfield;

import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import au.com.codeka.warworlds.model.Star;

/** An andengine sprite that represents a star. */
public class StarSprite extends Sprite {
    private StarfieldSceneManager mStarfield;
    private Star mStar;
    private Text mStarName;

    public StarSprite(StarfieldSceneManager starfield, Star star,
                      float x, float y,
                      ITextureRegion textureRegion,
                      VertexBufferObjectManager vertexBufferObjectManager) {
        super(x, y,
              calculateSpriteSize(star), 
              calculateSpriteSize(star),
              textureRegion, vertexBufferObjectManager);
        mStar = star;
        mStarfield = starfield;

        float starSize = calculateSpriteSize(star);
        mStarName = new Text(starSize / 2.0f, -starSize / 4.0f,
                             mStarfield.getFont(), star.getName(), mStarfield.getActivity().getVertexBufferObjectManager());
        attachChild(mStarName);
    }

    private static float calculateSpriteSize(Star star) {
        return (float)(star.getSize() * star.getStarType().getImageScale() * 2.0f);
    }

    public Star getStar() {
        return mStar;
    }

    @Override
    public boolean onAreaTouched(final TouchEvent sceneTouchEvent,
                                 final float touchAreaLocalX,
                                 final float touchAreaLocalY) {
        if (sceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
            mStarfield.setSelectingSprite(this);
        } else if (sceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
            Sprite selectingSprite = mStarfield.getSelectingSprite();
            if (selectingSprite == this) {
                mStarfield.selectStar(this);
            }
        }
        return true;
    }
}
