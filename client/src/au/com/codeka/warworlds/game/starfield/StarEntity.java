package au.com.codeka.warworlds.game.starfield;

import org.andengine.entity.Entity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import au.com.codeka.warworlds.model.Star;

/** An entity that represents a star. */
public class StarEntity extends Entity {
    private StarfieldSceneManager mStarfield;
    private StarSprite mStarSprite;
    private Star mStar;
    private Text mStarName;

    public StarEntity(StarfieldSceneManager starfield, Star star,
                      float x, float y,
                      ITextureRegion textureRegion,
                      VertexBufferObjectManager vertexBufferObjectManager) {
        super(x, y, star.getSize() * 3.0f, star.getSize() * 3.0f);
        mStar = star;
        mStarfield = starfield;

        final float starSize = star.getSize() * 3.0f;
        mStarSprite = new StarSprite(starSize, textureRegion, vertexBufferObjectManager);
        if (star.getStarType().getImageScale() > 1.0) {
            mStarSprite.setScale((float) star.getStarType().getImageScale());
        }
        mStarName = new Text(0.0f, -starSize * 0.6f,
                             mStarfield.getFont(), star.getName(),
                             mStarfield.getActivity().getVertexBufferObjectManager());

        attachChild(mStarSprite);
        attachChild(mStarName);
    }

    public Entity getTouchEntity() {
        return mStarSprite;
    }

    public Star getStar() {
        return mStar;
    }

    private class StarSprite extends Sprite {
        public StarSprite(float size, ITextureRegion textureRegion, VertexBufferObjectManager vertexBufferObjectManager) {
            super(0.0f, 0.0f, size, size, textureRegion, vertexBufferObjectManager);
        }

        @Override
        public boolean onAreaTouched(final TouchEvent sceneTouchEvent,
                                     final float touchAreaLocalX,
                                     final float touchAreaLocalY) {
            if (sceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
                mStarfield.setSelectingSprite(StarEntity.this);
            } else if (sceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
                StarEntity selectingSprite = mStarfield.getSelectingSprite();
                if (selectingSprite == StarEntity.this) {
                    mStarfield.selectStar(StarEntity.this);
                }
            }
            return true;
        }
    }
}
