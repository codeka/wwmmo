package au.com.codeka.warworlds.game.starfield;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.ctrl.BannerAdView;
import au.com.codeka.warworlds.model.Sector;

/** This entity is added to the HUB when you have a star with a HQ. We'll point towards that star. */
public class HqEntity extends Entity {
    private StarfieldSceneManager mStarfield;
    private BaseStar mHomeStar;
    private Camera mCamera;
    private Sprite mSprite;

    private IUpdateHandler mUpdateHandler = new IUpdateHandler() {
        @Override
        public void onUpdate(float pSecondsElapsed) {
            float zoomFactor = ((ZoomCamera) mCamera).getZoomFactor();
            Vector2 direction = mStarfield.getSectorOffset(mHomeStar.getSectorX(), mHomeStar.getSectorY());
            direction.add(mHomeStar.getOffsetX(), Sector.SECTOR_SIZE - mHomeStar.getOffsetY());
            Vector2 cam = Vector2.pool.borrow().reset(mCamera.getCenterX(), mCamera.getCenterY());
            direction.subtract(cam);
            direction.scale(zoomFactor);

            Vector2 pos = Vector2.pool.borrow().reset(direction);
            boolean show = false;
            float halfWidth = mCamera.getCameraSceneWidth() / 2.0f - mSprite.getWidth() / 2.0f;
            if (pos.x < -halfWidth) {
                show = true;
                pos.x = -halfWidth;
            } else if (pos.x > halfWidth) {
                show = true;
                pos.x = halfWidth;
            }
            float halfHeight = mCamera.getCameraSceneHeight() / 2.0f - mSprite.getHeight() / 2.0f;
            float top = halfHeight;
            if (BannerAdView.isAdVisible()) {
                top -= 50 * mStarfield.getActivity().getResources().getDisplayMetrics().density; // don't go under the ad
            }
            if (pos.y > top) {
                show = true;
                pos.y = top;
            }
            float bottom = -halfHeight;
            if (mStarfield.getActivity() instanceof StarfieldActivity) {
                bottom += ((StarfieldActivity) mStarfield.getActivity()).getBottomPaneHeight();
            }
            if (pos.y < bottom) {
                show = true;
                pos.y = bottom;
            }

            if (show) {
                Vector2 up = Vector2.pool.borrow().reset(0.0f, 1.0f);
                direction.normalize();
                float angle = Vector2.angleBetween(direction, up);
    
                mSprite.setRotation(angle * 180.0f / (float) Math.PI);
                mSprite.setPosition((float) pos.x, (float) pos.y);
                mSprite.setVisible(true);
    
                Vector2.pool.release(up);
            } else {
                mSprite.setVisible(false);
            }

            Vector2.pool.release(direction);
            Vector2.pool.release(pos);
            Vector2.pool.release(cam);
        }

        @Override
        public void reset() {
            mSprite.setVisible(false);
        }
    };

    public HqEntity(StarfieldSceneManager starfield, BaseStar homeStar, Camera camera,
                    VertexBufferObjectManager vertexBufferObjectManager) {
        super(camera.getCameraSceneWidth() / 2.0f, camera.getCameraSceneHeight() / 2.0f, 1.0f, 1.0f);
        mHomeStar = homeStar;
        mCamera = camera;
        mStarfield = starfield;

        ITextureRegion texture = starfield.getArrowTexture();
        mSprite = new Sprite(0.0f, 0.0f, 32.f, 32.0f, texture, vertexBufferObjectManager);
        mSprite.setVisible(false);
        attachChild(mSprite);

        registerUpdateHandler(mUpdateHandler);
    }
}
