package au.com.codeka.warworlds.game.wormhole;

import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An entity that represents a star. */
public class WormholeEntity extends Entity {
    private static Logger log = LoggerFactory.getLogger(WormholeEntity.class);
    private WormholeSceneManager mSceneManager;
    private AnimatedSprite mWormholeSprite;

    public WormholeEntity(WormholeSceneManager sceneManager, ITiledTextureRegion textureRegion,
                          VertexBufferObjectManager vertexBufferObjectManager) {
        super(0.0f, 0.0f, 1.0f, 1.0f);
        mSceneManager = sceneManager;

        float margin = sceneManager.getActivity().getWidth() * 0.5f;
        float size = sceneManager.getActivity().getWidth() * 0.999f;

        float x = margin;
        float y = sceneManager.getActivity().getHeight() - margin;

        mWormholeSprite = new AnimatedSprite(x, y, size, size, textureRegion, vertexBufferObjectManager);
        mWormholeSprite.animate(1000);
        attachChild(mWormholeSprite);

        mSceneManager.getActivity().getEngine().registerUpdateHandler(mWormholeUpdateHandler);
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
