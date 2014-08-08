package au.com.codeka.warworlds.game.wormhole;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

/** An entity that represents a star. */
public class WormholeEntity extends Entity {
    private WormholeSceneManager mSceneManager;
    private AnimatedSprite mWormholeSprite;

    public WormholeEntity(WormholeSceneManager sceneManager, ITiledTextureRegion textureRegion,
                          VertexBufferObjectManager vertexBufferObjectManager) {
        super(0.0f, 0.0f, 1.0f, 1.0f);
        mSceneManager = sceneManager;

        int activityWidth = sceneManager.getFragment().getWidth();
        int activityHeight = sceneManager.getFragment().getHeight();
        int activitySize = Math.min(activityWidth, activityHeight);
        float size = activitySize * 0.999f;

        float x = activityWidth * 0.5f;
        float y = activityHeight - (activityHeight * 0.5f);

        mWormholeSprite = new AnimatedSprite(x, y, size, size, textureRegion, vertexBufferObjectManager);
        mWormholeSprite.animate(1000);
        attachChild(mWormholeSprite);

        mSceneManager.getFragment().getEngine().registerUpdateHandler(mWormholeUpdateHandler);
    }

    private IUpdateHandler mWormholeUpdateHandler = new IUpdateHandler() {
        @Override
        public void onUpdate(float dt) {
            float rotation = mWormholeSprite.getRotation() + 15.0f * dt;
            while (rotation > 360.0f) {
                rotation -= 360.0f;
            }
            mWormholeSprite.setRotation(rotation);
        }

        @Override
        public void reset() {
        }
    };
}
