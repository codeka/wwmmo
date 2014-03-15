package au.com.codeka.warworlds.game.wormhole;

import java.util.Random;

import org.andengine.engine.Engine;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.ITextureAtlas.ITextureAtlasStateListener;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.warworlds.BaseGlActivity;
import au.com.codeka.warworlds.game.starfield.RadarIndicatorEntity;
import au.com.codeka.warworlds.game.starfield.SectorSceneManager;
import au.com.codeka.warworlds.model.Sector;

public class WormholeSceneManager {
    private static final Logger log = LoggerFactory.getLogger(SectorSceneManager.class);
    private Scene mScene;
    private BaseGlActivity mActivity;
    private String mWormholeStarKey;
    private boolean mWasStopped;

    private BitmapTextureAtlas mWormholeTextureAtlas;
    private TiledTextureRegion mWormholeTextureRegion;

    private BitmapTextureAtlas mBackgroundGasTextureAtlas;
    private TiledTextureRegion mBackgroundGasTextureRegion;
    private BitmapTextureAtlas mBackgroundStarsTextureAtlas;
    private TiledTextureRegion mBackgroundStarsTextureRegion;

    public WormholeSceneManager(BaseGlActivity activity, String starKey) {
        mActivity = activity;
        mWormholeStarKey = starKey;
    }

    public BaseGlActivity getActivity() {
        return mActivity;
    }

    public void onLoadResources() {
        mWormholeTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mWormholeTextureAtlas.setTextureAtlasStateListener(new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());

        mWormholeTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mWormholeTextureAtlas, mActivity,
                "stars/wormhole_big.png", 0, 0, 2, 2);

        mBackgroundGasTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 512, 512,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mBackgroundGasTextureAtlas.setTextureAtlasStateListener(new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());

        mBackgroundGasTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBackgroundGasTextureAtlas,
                mActivity, "decoration/gas.png", 0, 0, 4, 4);
        mBackgroundStarsTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(), 512, 512,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mBackgroundStarsTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBackgroundStarsTextureAtlas,
                mActivity, "decoration/starfield.png", 0, 0, 4, 4);

        mActivity.getShaderProgramManager().loadShaderProgram(RadarIndicatorEntity.getShaderProgram());
        mActivity.getTextureManager().loadTexture(mWormholeTextureAtlas);
        mActivity.getTextureManager().loadTexture(mBackgroundGasTextureAtlas);
        mActivity.getTextureManager().loadTexture(mBackgroundStarsTextureAtlas);
    }

    public void onStart() {
/*
        mActivity.getEngine().setErrorHandler(new Engine.EngineErrorHandler() {
            @Override
            public void onRenderThreadException(Exception e) {
                refreshScene();
            }
        });
*/

        if (mWasStopped) {
            log.debug("We were stopped, refreshing the scene...");
            refreshScene();
        }
    }

    public void onStop() {
        mWasStopped = true;
    }

    protected void refreshScene() {
        new BackgroundRunner<Scene>() {
            @Override
            protected Scene doInBackground() {
                try {
                    return createScene();
                } catch(Exception e) {
                    return null;
                }
            }

            @Override
            protected void onComplete(final Scene scene) {
                if (scene == null) {
                    return;
                }

                mActivity.getEngine().runOnUpdateThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.getEngine().setScene(scene);
                    }
                });
            }
        }.execute();
    }


    public Scene createScene() {
        if (mActivity.getEngine() == null) {
            // if the engine hasn't been created yet, schedule a refresh for later...
            mActivity.runOnUpdateThread(new Runnable() {
                @Override
                public void run() {
                    refreshScene();
                }
            });
            return null;
        }

        mScene = new Scene();
        mScene.setBackground(new Background(0.0f, 0.0f, 0.0f));

        drawBackground(mScene);

        WormholeEntity entity = new WormholeEntity(this, mWormholeTextureRegion, mActivity.getVertexBufferObjectManager());
        mScene.attachChild(entity);

        return mScene;
    }

    private void drawBackground(Scene scene) {
        Random r = new Random(mWormholeStarKey.hashCode());
        final int STAR_SIZE = 256;
        for (int y = 0; y < Sector.SECTOR_SIZE / STAR_SIZE; y++) {
            for (int x = 0; x < Sector.SECTOR_SIZE / STAR_SIZE; x++) {
                Sprite bgSprite = new Sprite(
                        (float) x * STAR_SIZE, (float) y * STAR_SIZE, STAR_SIZE, STAR_SIZE,
                        mBackgroundStarsTextureRegion.getTextureRegion(r.nextInt(16)),
                        mActivity.getVertexBufferObjectManager());
                scene.attachChild(bgSprite);
            }
        }

        final int GAS_SIZE = 512;
        for (int i = 0; i < 10; i++) {
            float x = r.nextInt(Sector.SECTOR_SIZE + (GAS_SIZE / 4)) - (GAS_SIZE / 8);
            float y = r.nextInt(Sector.SECTOR_SIZE + (GAS_SIZE / 4)) - (GAS_SIZE / 8);

            Sprite bgSprite = new Sprite(
                    x - (GAS_SIZE / 2.0f), y - (GAS_SIZE / 2.0f), GAS_SIZE, GAS_SIZE,
                    mBackgroundGasTextureRegion.getTextureRegion(r.nextInt(14)),
                    mActivity.getVertexBufferObjectManager());
            scene.attachChild(bgSprite);
        }
    }

}
