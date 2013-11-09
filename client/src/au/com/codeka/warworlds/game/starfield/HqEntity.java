package au.com.codeka.warworlds.game.starfield;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;

import au.com.codeka.warworlds.model.Star;

/** This entity is added to the HUB when you have a star with a HQ. We'll point towards that star. */
public class HqEntity extends Entity {
    private Star mHomeStar;
    private Camera mCamera;

    private IUpdateHandler mUpdateHandler = new IUpdateHandler() {
        @Override
        public void onUpdate(float pSecondsElapsed) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void reset() {
            // TODO Auto-generated method stub
            
        }
    };

    public HqEntity(Star homeStar, Camera camera) {
        super(0.0f, 0.0f, 1.0f, 1.0f);
        mHomeStar = homeStar;
        mCamera = camera;

        registerUpdateHandler(mUpdateHandler);
    }
}
