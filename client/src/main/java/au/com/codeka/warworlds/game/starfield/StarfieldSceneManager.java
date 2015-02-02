package au.com.codeka.warworlds.game.starfield;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.Entity;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.ITextureAtlas.ITextureAtlasStateListener;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder
    .TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import au.com.codeka.common.Log;
import au.com.codeka.common.Pair;
import au.com.codeka.common.Tuple;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.eventbus.EventBus;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.MyEmpire;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

/**
 * \c SurfaceView that displays the starfield. You can scroll around, tap on stars to bring
 * up their details and so on.
 */
public class StarfieldSceneManager extends SectorSceneManager {
  private static final Log log = new Log("StarfieldSceneManager");

  public static final EventBus eventBus = new EventBus();

  private BaseStar hqStar;
  private boolean hasScrolled;

  private boolean wasDragging;

  private Font font;
  private TiledTextureRegion bigStarTextureRegion;
  private TiledTextureRegion normalStarTextureRegion;

  private ITextureRegion arrowIconTextureRegion;

  private HashMap<String, ITextureRegion> fleetSpriteTextures;

  private TiledTextureRegion backgroundGasTextureRegion;
  private TiledTextureRegion backgroundStarsTextureRegion;

  private boolean isBackgroundVisible = true;
  private float backgroundZoomAlpha = 1.0f;
  private boolean isTacticalVisible = false;
  private float tacticalZoomAlpha = 0.0f;

  public StarfieldSceneManager(BaseStarfieldActivity activity) {
    super(activity);
  }

  @Override
  public void onLoadResources() {
    BitmapTextureAtlas starTextureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(),
        256, 384, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    starTextureAtlas.setTextureAtlasStateListener(
        new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());

    normalStarTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(
        starTextureAtlas, mActivity, "stars/stars_small.png", 0, 0, 4, 6);
    bigStarTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(
        starTextureAtlas, mActivity, "stars/stars_small.png", 0, 0, 2, 3);

    BitmapTextureAtlas backgroundGasTextureAtlas = new BitmapTextureAtlas(
        mActivity.getTextureManager(), 512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    backgroundGasTextureAtlas.setTextureAtlasStateListener(
        new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());

    backgroundGasTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(
        backgroundGasTextureAtlas, mActivity, "decoration/gas.png", 0, 0, 4, 4);
    BitmapTextureAtlas backgroundStarsTextureAtlas = new BitmapTextureAtlas(
        mActivity.getTextureManager(), 512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    backgroundStarsTextureRegion = BitmapTextureAtlasTextureRegionFactory
        .createTiledFromAsset(backgroundStarsTextureAtlas, mActivity, "decoration/starfield.png",
            0, 0, 4, 4);

    BuildableBitmapTextureAtlas iconTextureAtlas = new BuildableBitmapTextureAtlas(
        mActivity.getTextureManager(), 256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    iconTextureAtlas.setTextureAtlasStateListener(
        new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());

    arrowIconTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
        iconTextureAtlas, mActivity, "img/arrow.png");

    BuildableBitmapTextureAtlas fleetSpriteTextureAtlas = new BuildableBitmapTextureAtlas(
        mActivity.getTextureManager(), 256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    fleetSpriteTextureAtlas.setTextureAtlasStateListener(
        new ITextureAtlasStateListener.DebugTextureAtlasStateListener<IBitmapTextureAtlasSource>());

    fleetSpriteTextures = new HashMap<>();
    fleetSpriteTextures.put("ship.fighter", BitmapTextureAtlasTextureRegionFactory
        .createFromAsset(fleetSpriteTextureAtlas, mActivity, "spritesheets/ship.fighter.png"));
    fleetSpriteTextures.put("ship.scout", BitmapTextureAtlasTextureRegionFactory
        .createFromAsset(fleetSpriteTextureAtlas, mActivity, "spritesheets/ship.scout.png"));
    fleetSpriteTextures.put("ship.colony", BitmapTextureAtlasTextureRegionFactory
        .createFromAsset(fleetSpriteTextureAtlas, mActivity, "spritesheets/ship.colony.png"));
    fleetSpriteTextures.put("ship.troopcarrier", BitmapTextureAtlasTextureRegionFactory
        .createFromAsset(fleetSpriteTextureAtlas, mActivity, "spritesheets/ship.troopcarrier.png"));
    fleetSpriteTextures.put("ship.wormhole-generator", BitmapTextureAtlasTextureRegionFactory
        .createFromAsset(fleetSpriteTextureAtlas, mActivity,
            "spritesheets/ship.wormhole-generator.png"));
    fleetSpriteTextures.put("ship.upgrade.boost", BitmapTextureAtlasTextureRegionFactory
        .createTiledFromAsset(fleetSpriteTextureAtlas, mActivity,
            "spritesheets/ship.upgrade.boost.png", 2, 1));

    mActivity.getShaderProgramManager().loadShaderProgram(RadarIndicatorEntity.getShaderProgram());
    mActivity.getShaderProgramManager().loadShaderProgram(
        WormholeDisruptorIndicatorEntity.getShaderProgram());
    mActivity.getTextureManager().loadTexture(starTextureAtlas);
    mActivity.getTextureManager().loadTexture(backgroundGasTextureAtlas);
    mActivity.getTextureManager().loadTexture(backgroundStarsTextureAtlas);

    try {
      BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas> builder =
          new BlackPawnTextureAtlasBuilder<>(1, 1, 1);
      iconTextureAtlas.build(builder);
      iconTextureAtlas.load();

      fleetSpriteTextureAtlas.build(builder);
      fleetSpriteTextureAtlas.load();
    } catch (TextureAtlasBuilderException e) {
      log.error("Error building texture atlas.", e);
    }

    font = FontFactory.create(mActivity.getFontManager(), mActivity.getTextureManager(), 256, 256,
        Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), 16, true, Color.WHITE);
    font.load();
  }

  @Override
  protected void onStart() {
    super.onStart();

    StarManager.eventBus.register(mEventHandler);
    EmpireManager.eventBus.register(mEventHandler);

    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (myEmpire != null) {
      BaseStar homeStar = myEmpire.getHomeStar();
      int numHqs = BuildManager.i.getTotalBuildingsInEmpire("hq");
      if (numHqs > 0) {
        hqStar = homeStar;
      }
    }
  }

  @Override
  protected void onStop() {
    super.onStop();

    StarManager.eventBus.unregister(mEventHandler);
    EmpireManager.eventBus.unregister(mEventHandler);
  }

  public Font getFont() {
    return font;
  }

  public ITextureRegion getSpriteTexture(String spriteName) {
    return fleetSpriteTextures.get(spriteName);
  }

  public ITextureRegion getArrowTexture() {
    return arrowIconTextureRegion;
  }

  @Override
  public void scrollTo(final long sectorX, final long sectorY, final float offsetX,
      final float offsetY) {
    hasScrolled = true;
    super.scrollTo(sectorX, sectorY, offsetX, offsetY);
  }

  @Override
  protected void refreshScene(StarfieldScene scene) {
    if (!hasScrolled) {
      // if you haven't scrolled yet, then don't even think about refreshing the
      // scene... it's a waste of time!
      log.debug("We haven't scrolled yet, not drawing the scene.");
      return;
    }

    if (mActivity.getEngine() == null) {
      // if the engine hasn't been created yet, we can't really do anything.
      return;
    }

    final List<Pair<Long, Long>> missingSectors = drawScene(scene);
    if (missingSectors != null) {
      SectorManager.i.refreshSectors(missingSectors, false);
    }

    scene.refreshSelectionIndicator();
    eventBus.publish(new SceneUpdatedEvent(scene));
  }

  @Override
  protected int getDesiredSectorRadius() {
    return isTacticalVisible ? 2 : 1;
  }

  @Override
  protected void refreshHud(HUD hud) {
    MyEmpire myEmpire = EmpireManager.i.getEmpire();
    if (myEmpire != null && myEmpire.getHomeStar() != null) {
      // if you have a HQ, it'll be on your home star.
      if (BuildManager.i.getTotalBuildingsInEmpire("hq") > 0) {
        hud.attachChild(new HqEntity(this, myEmpire.getHomeStar(), mActivity.getCamera(),
            mActivity.getVertexBufferObjectManager()));
      }
    }
  }

  @Override
  protected void updateZoomFactor(float zoomFactor) {
    super.updateZoomFactor(zoomFactor);
    boolean wasTacticalVisible = isTacticalVisible;
    boolean wasBackgroundVisible = isBackgroundVisible;

    // we fade out the background between 0.65 and 0.6, it should be totally invisible < 0.6
    // and totally opaque for >= 0.65
    boolean needUpdate = false;
    if (zoomFactor < 0.6f && isBackgroundVisible) {
      isBackgroundVisible = false;
      backgroundZoomAlpha = 0.0f;
      needUpdate = true;
    } else if (zoomFactor >= 0.6f && !isBackgroundVisible) {
      isBackgroundVisible = true;
      backgroundZoomAlpha = 1.0f;
      needUpdate = true;
    }
    if (zoomFactor >= 0.6f && zoomFactor < 0.65f) {
      // between 0.6 and 0.65 we need to fade the background in
      backgroundZoomAlpha = (zoomFactor - 0.6f) * 20.0f; // make it in the range 0...1
      needUpdate = true;
    }
    if (needUpdate) {
      mActivity.runOnUpdateThread(updateBackgroundRunnable);
    }

    // similarly, we fade IN the tactical view as you zoom out. It starts fading in a bit sooner
    // than the background fades out, and fades slower, too.
    needUpdate = false;
    if (zoomFactor >= 0.6f && isTacticalVisible) {
      isTacticalVisible = false;
      tacticalZoomAlpha = 0.0f;
      needUpdate = true;
    } else if (zoomFactor < 0.4f && !isTacticalVisible) {
      isTacticalVisible = true;
      tacticalZoomAlpha = 1.0f;
      needUpdate = true;
    }
    if (zoomFactor >= 0.4f && zoomFactor < 0.6f) {
      isTacticalVisible = true;
      tacticalZoomAlpha = 1.0f - ((zoomFactor - 0.4f) * 5.0f); // make it 1...0
      needUpdate = true;
    }
    if (needUpdate) {
      mActivity.runOnUpdateThread(updateTacticalRunnable);
    }

    if (wasTacticalVisible != isTacticalVisible || wasBackgroundVisible != isBackgroundVisible) {
      // If the tactical view or background has gone from visible -> invisible (or vice
      // versa), then we'll need to redraw the scene.
      queueRefreshScene();
    }
  }

  /**
   * Updates the background entities with the current zoom alpha on the update thread.
   */
  private final Runnable updateBackgroundRunnable = new Runnable() {
    @Override
    public void run() {
      StarfieldScene scene = getScene();
      if (scene == null) {
        return;
      }
      List<Entity> backgroundEntities = scene.getBackgroundEntities();
      if (backgroundEntities == null) {
        return;
      }
      for (Entity entity : backgroundEntities) {
        entity.setVisible(isBackgroundVisible);
        entity.setAlpha(backgroundZoomAlpha);
        entity.setColor(backgroundZoomAlpha, backgroundZoomAlpha, backgroundZoomAlpha);
      }
    }
  };

  private final Runnable updateTacticalRunnable = new Runnable() {
    @Override
    public void run() {
      StarfieldScene scene = getScene();
      if (scene == null) {
        return;
      }
      List<Entity> tacticalEntities = scene.getTacticalEntities();
      if (tacticalEntities == null) {
        return;
      }
      for (Entity entity : tacticalEntities) {
        setTacticalZoomFactor(entity);
      }
    }
  };

  private List<Pair<Long, Long>> drawScene(StarfieldScene scene) {
    List<Pair<Long, Long>> missingSectors = null;

    // if the tactical view is visible, we can completely skip drawing the background.
    if (!isTacticalVisible) {
      for (int y = -scene.getSectorRadius(); y <= scene.getSectorRadius(); y++) {
        for (int x = -scene.getSectorRadius(); x <= scene.getSectorRadius(); x++) {
          long sX = scene.getSectorX() + x;
          long sY = scene.getSectorY() + y;
          Sector sector = SectorManager.i.getSector(sX, sY);
          if (sector == null) {
            if (missingSectors == null) {
              missingSectors = new ArrayList<>();
            }
            missingSectors.add(new Pair<>(sX, sY));
            continue;
          }

          int sx = (x * Sector.SECTOR_SIZE);
          int sy = -(y * Sector.SECTOR_SIZE);
          drawBackground(scene, sector, sx, sy);
        }
      }
    }

    for (int y = -scene.getSectorRadius(); y <= scene.getSectorRadius(); y++) {
      for (int x = -scene.getSectorRadius(); x <= scene.getSectorRadius(); x++) {
        long sX = scene.getSectorX() + x;
        long sY = scene.getSectorY() + y;

        Sector sector = SectorManager.i.getSector(sX, sY);
        if (sector == null) {
          continue;
        }

        int sx = (x * Sector.SECTOR_SIZE);
        int sy = -(y * Sector.SECTOR_SIZE);
        addSector(scene, sx, sy, sector);
      }
    }

    return missingSectors;
  }

  private void drawBackground(StarfieldScene scene, Sector sector, int sx, int sy) {
    Random r = new Random(sector.getX() ^ (sector.getY() * 48647563));
    final int STAR_SIZE = 256;
    for (int y = 0; y < Sector.SECTOR_SIZE / STAR_SIZE; y++) {
      for (int x = 0; x < Sector.SECTOR_SIZE / STAR_SIZE; x++) {
        Sprite bgSprite = new Sprite((float) (sx + (x * STAR_SIZE)), (float) (sy + (y * STAR_SIZE)),
            STAR_SIZE, STAR_SIZE, backgroundStarsTextureRegion.getTextureRegion(r.nextInt(16)),
            mActivity.getVertexBufferObjectManager());
        setBackgroundEntityZoomFactor(bgSprite);
        scene.attachBackground(bgSprite);
      }
    }

    final int GAS_SIZE = 512;
    for (int i = 0; i < 10; i++) {
      float x = r.nextInt(Sector.SECTOR_SIZE + (GAS_SIZE / 4)) - (GAS_SIZE / 8);
      float y = r.nextInt(Sector.SECTOR_SIZE + (GAS_SIZE / 4)) - (GAS_SIZE / 8);

      Sprite bgSprite = new Sprite((sx + x) - (GAS_SIZE / 2.0f), (sy + y) - (GAS_SIZE / 2.0f),
          GAS_SIZE, GAS_SIZE, backgroundGasTextureRegion.getTextureRegion(r.nextInt(14)),
          mActivity.getVertexBufferObjectManager());
      setBackgroundEntityZoomFactor(bgSprite);
      scene.attachBackground(bgSprite);
    }
  }

  private void setBackgroundEntityZoomFactor(Sprite bgSprite) {
    if (backgroundZoomAlpha <= 0.0f) {
      bgSprite.setVisible(false);
    } else if (backgroundZoomAlpha >= 1.0f) {
      // do nothing
    } else {
      bgSprite.setAlpha(backgroundZoomAlpha);
      bgSprite.setColor(backgroundZoomAlpha, backgroundZoomAlpha, backgroundZoomAlpha);
    }
  }

  /**
   * Draws a sector, which is a 1024x1024 area of stars.
   */
  private void addSector(StarfieldScene scene, int offsetX, int offsetY, Sector sector) {
    if (isTacticalVisible) {
      addTacticalSprite(scene, offsetX, offsetY, sector);
    }

    for (BaseStar star : sector.getStars()) {
      addStar(scene, (Star) star, offsetX, offsetY);
    }
    for (BaseStar star : sector.getStars()) {
      for (BaseFleet fleet : star.getFleets()) {
        if (fleet.getState() == Fleet.State.MOVING) {
          addMovingFleet(scene, (Fleet) fleet, (Star) star, offsetX, offsetY);
        }
      }
    }
  }

  /**
   * Adds a sprite for the 'tactical' view, This is a single texture over the whole sector which
   * is colored in depending on who owns the star underneath it.
   */
  private void addTacticalSprite(StarfieldScene scene, int offsetX, int offsetY, Sector sector) {
    BitmapTextureAtlas textureAtlas = new BitmapTextureAtlas(mActivity.getTextureManager(),
        256, 256, TextureOptions.NEAREST);
    TacticalBitmapTextureSource bitmapSource = TacticalBitmapTextureSource.create(sector);
    ITextureRegion textureRegion = BitmapTextureAtlasTextureRegionFactory.createFromSource(
        textureAtlas, bitmapSource, 0, 0);
    textureAtlas.load();

    TacticalOverlayEntity tacticalOverlayEntity = new TacticalOverlayEntity(
        offsetX + Sector.SECTOR_SIZE / 2, offsetY + Sector.SECTOR_SIZE / 2,
        Sector.SECTOR_SIZE, Sector.SECTOR_SIZE, textureRegion,
        mActivity.getVertexBufferObjectManager());
    setTacticalZoomFactor(tacticalOverlayEntity);
    scene.attachTacticalEntity(tacticalOverlayEntity, textureAtlas, textureRegion);
  }

  private void setTacticalZoomFactor(Entity entity) {
    entity.setVisible(isTacticalVisible);
    entity.setAlpha(tacticalZoomAlpha);
    entity.setColor(tacticalZoomAlpha, tacticalZoomAlpha, tacticalZoomAlpha);
  }

  /**
   * Draws a single star. Note that we draw all stars first, then the names of stars
   * after.
   */
  private void addStar(StarfieldScene scene, Star star, int x, int y) {
    x += star.getOffsetX();
    y += Sector.SECTOR_SIZE - star.getOffsetY();

    ITextureRegion textureRegion;
    if (star.getStarType().getInternalName().equals("neutron")) {
      textureRegion = bigStarTextureRegion.getTextureRegion(0);
    } else if (star.getStarType().getInternalName().equals("wormhole")) {
      textureRegion = bigStarTextureRegion.getTextureRegion(1);
    } else {
      int offset = 0;
      if (star.getStarType().getInternalName().equals("black-hole")) {
        offset = 8;
      } else if (star.getStarType().getInternalName().equals("blue")) {
        offset = 9;
      } else if (star.getStarType().getInternalName().equals("orange")) {
        offset = 12;
      } else if (star.getStarType().getInternalName().equals("red")) {
        offset = 13;
      } else if (star.getStarType().getInternalName().equals("white")) {
        offset = 16;
      } else if (star.getStarType().getInternalName().equals("yellow")) {
        offset = 17;
      } else if (star.getStarType().getInternalName().equals("marker")) {
        offset = 18;
      }
      textureRegion = normalStarTextureRegion.getTextureRegion(offset);
    }

    StarEntity starEntity = new StarEntity(this, star, (float) x, (float) y, textureRegion,
        mActivity.getVertexBufferObjectManager(), !isTacticalVisible, 1.0f - tacticalZoomAlpha);
    scene.registerTouchArea(starEntity.getTouchEntity());
    scene.attachChild(starEntity);
  }

  /**
   * Given a sector, returns the (x, y) coordinates (in view-space) of the origin of this
   * sector.
   */
  public Vector2 getSectorOffset(long sx, long sy) {
    return getSectorOffset(mSectorX, mSectorY, sx, sy);
  }

  public Vector2 getSectorOffset(long sectorX, long sectorY, long sx, long sy) {
    sx -= sectorX;
    sy -= sectorY;
    return Vector2.pool.borrow().reset((sx * Sector.SECTOR_SIZE), -(sy * Sector.SECTOR_SIZE));
  }

  /**
   * Draw a moving fleet as a line between the source and destination stars, with an icon
   * representing the current location of the fleet.
   */
  private void addMovingFleet(StarfieldScene scene, Fleet fleet, Star srcStar, int offsetX,
      int offsetY) {
    // we'll need to find the destination star
    Star destStar = SectorManager.i.findStar(fleet.getDestinationStarKey());
    if (destStar == null) {
      // the destination star isn't in one of the sectors we have in memory, we'll
      // just ignore this fleet (it's probably flying off the edge of the sector and our
      // little viewport won't see it anyway -- unless you've got a REALLY long-range
      // flight, maybe we can stop that from being possible).
      return;
    }

    Vector2 srcPoint = Vector2.pool.borrow().reset(offsetX, offsetY);
    srcPoint.x += srcStar.getOffsetX();
    srcPoint.y += Sector.SECTOR_SIZE - srcStar.getOffsetY();

    Vector2 destPoint =
        getSectorOffset(scene.getSectorX(), scene.getSectorY(), destStar.getSectorX(),
            destStar.getSectorY());
    destPoint.x += destStar.getOffsetX();
    destPoint.y += Sector.SECTOR_SIZE - destStar.getOffsetY();

    FleetEntity fleetEntity =
        new FleetEntity(this, srcPoint, destPoint, fleet, mActivity.getVertexBufferObjectManager());
    scene.registerTouchArea(fleetEntity.getTouchEntity());
    scene.attachChild(fleetEntity);
  }

  @Override
  public boolean onSceneTouchEvent(Scene scene, TouchEvent touchEvent) {
    boolean handled = super.onSceneTouchEvent(scene, touchEvent);

    if (touchEvent.getAction() == TouchEvent.ACTION_DOWN) {
      wasDragging = false;
    } else if (touchEvent.getAction() == TouchEvent.ACTION_UP) {
      if (!wasDragging) {
        float tx = touchEvent.getX();
        float ty = touchEvent.getY();

        long sectorX = (long) (tx / Sector.SECTOR_SIZE) + mSectorX;
        long sectorY = (long) (ty / Sector.SECTOR_SIZE) + mSectorY;
        int offsetX = (int) (tx - (tx / Sector.SECTOR_SIZE));
        int offsetY = Sector.SECTOR_SIZE - (int) (ty - (ty / Sector.SECTOR_SIZE));
        while (offsetX < 0) {
          sectorX--;
          offsetX += Sector.SECTOR_SIZE;
        }
        while (offsetX > Sector.SECTOR_SIZE) {
          sectorX++;
          offsetX -= Sector.SECTOR_SIZE;
        }
        while (offsetY < 0) {
          sectorY--;
          offsetY += Sector.SECTOR_SIZE;
        }
        while (offsetY > Sector.SECTOR_SIZE) {
          sectorY++;
          offsetY -= Sector.SECTOR_SIZE;
        }

        ((StarfieldScene) scene).selectNothing(sectorX, sectorY, offsetX, offsetY);
        handled = true;
      }
    }

    return handled;
  }

  @Override
  protected GestureDetector.OnGestureListener createGestureListener() {
    return new GestureListener();
  }

  @Override
  protected ScaleGestureDetector.OnScaleGestureListener createScaleGestureListener() {
    return new ScaleGestureListener();
  }

  /**
   * The default gesture listener is just for scrolling around.
   */
  protected class GestureListener extends SectorSceneManager.GestureListener {
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      super.onScroll(e1, e2, distanceX, distanceY);

      // because we've navigating the map, we're no longer in the process of selecting a sprite.
      StarfieldScene scene = getScene();
      if (scene != null) {
        scene.cancelSelect();
      }
      wasDragging = true;
      return true;
    }
  }

  /**
   * The default scale gesture listener scales the view.
   */
  protected class ScaleGestureListener extends SectorSceneManager.ScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      super.onScale(detector);

      // because we've navigating the map, we're no longer in the process of selecting a sprite.
      StarfieldScene scene = getScene();
      if (scene != null) {
        scene.cancelSelect();
      }
      wasDragging = true;
      return true;
    }
  }

  private Object mEventHandler = new Object() {
    private String lastMyEmpireName;
    private DateTime lastMyEmpireShieldUpdateTime;

    /**
     * When a star is updated, if it's one of ours, then we'll want to redraw to make sure we
     * have the latest data (e.g. it might've been renamed)
     */
    @EventHandler
    public void onStarFetched(final Star s) {
      getActivity().runOnUpdateThread(new Runnable() {
        @Override
        public void run() {
          StarfieldScene scene = getScene();
          if (scene != null) {
            scene.onStarFetched(s);
          }
        }
      });
    }

    @EventHandler
    public void onEmpireUpdate(Empire empire) {
      MyEmpire myEmpire = EmpireManager.i.getEmpire();
      if (empire.getKey().equals(myEmpire.getKey())) {
        // If the player's empire changes, it might mean that the location of their HQ has
        // changed, so we'll want to make sure it's still correct.
        if (hqStar != null) {
          hqStar = empire.getHomeStar();
        }

        // Only refresh the scene if something we actually care about has changed (such
        // as the player's name or the shield image). Otherwise, this gets fired for every
        // notification, for example, and we don't need to redraw the scene for that.
        if (lastMyEmpireName == null || !lastMyEmpireName.equals(empire.getDisplayName())
            || lastMyEmpireShieldUpdateTime == null || !lastMyEmpireShieldUpdateTime
            .equals(empire.getShieldLastUpdate())) {
          lastMyEmpireName = empire.getDisplayName();
          lastMyEmpireShieldUpdateTime = empire.getShieldLastUpdate();
          queueRefreshScene();
        }

        return;
      }

      // If it's anyone but the player, then just refresh the scene.
      queueRefreshScene();
    }
  };

  public static class SpaceTapEvent {
    public long sectorX;
    public long sectorY;
    public int offsetX;
    public int offsetY;

    public SpaceTapEvent(long sectorX, long sectorY, int offsetX, int offsetY) {
      this.sectorX = sectorX;
      this.sectorY = sectorY;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
    }
  }

  public static class StarSelectedEvent {
    public Star star;

    public StarSelectedEvent(Star star) {
      this.star = star;
    }
  }

  public static class FleetSelectedEvent {
    public Fleet fleet;

    public FleetSelectedEvent(Fleet fleet) {
      this.fleet = fleet;
    }
  }

  public static class SceneUpdatedEvent {
    public StarfieldScene scene;

    public SceneUpdatedEvent(StarfieldScene scene) {
      this.scene = scene;
    }
  }
}
