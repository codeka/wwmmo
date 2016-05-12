package au.com.codeka.warworlds.game.starfield;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Pair;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;

/**
 * This is the base class for StarfieldSurfaceView and TacticalMapView, it contains the common code
 * for scrolling through sectors of stars, etc.
 */
public class SectorView extends UniverseElementSurfaceView {
  protected boolean scrollToCentre = false;

  protected int sectorRadius = 1;
  protected long sectorX;
  protected long sectorY;
  protected float offsetX;
  protected float offsetY;

  public SectorView(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (this.isInEditMode()) {
      return;
    }

    sectorX = sectorY = 0;
    offsetX = offsetY = 0;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    SectorManager.eventBus.register(eventHandler);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    SectorManager.eventBus.unregister(eventHandler);
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onSectorUpdated(Sector sector) {
      redraw();
    }

    @EventHandler
    public void onSectorListUpdated(SectorManager.SectorListChangedEvent event) {
      redraw();
    }
  };

  /**
   * Scroll to the given sector (x,y) and offset into the sector.
   */
  public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY) {
    scrollTo(sectorX, sectorY, offsetX, offsetY, false);
  }

  /**
   * Scroll to the given sector (x,y) and offset into the sector.
   */
  public void scrollTo(long sectorX, long sectorY, float offsetX, float offsetY, boolean centre) {
    this.sectorX = sectorX;
    this.sectorY = sectorY;
    this.offsetX = -offsetX;
    this.offsetY = -offsetY;

    if (centre) {
      scrollToCentre = true;
    }

    List<Pair<Long, Long>> missingSectors = new ArrayList<>();

    for (sectorY = this.sectorY - sectorRadius; sectorY <= this.sectorY + sectorRadius; sectorY++) {
      for (sectorX = this.sectorX - sectorRadius; sectorX <= this.sectorX + sectorRadius; sectorX++) {
        Pair<Long, Long> key = new Pair<>(sectorX, sectorY);
        Sector s = SectorManager.i.getSector(sectorX, sectorY);
        if (s == null) {
          missingSectors.add(key);
        }
      }
    }

    if (!missingSectors.isEmpty()) {
      SectorManager.i.refreshSectors(missingSectors, false);
    }

    redraw();
  }

  /**
   * Scrolls the view by a relative amount.
   *
   * @param distanceX Number of pixels in the X direction to scroll.
   * @param distanceY Number of pixels in the Y direction to scroll.
   */
  public void scroll(float distanceX, float distanceY) {
    offsetX += distanceX;
    offsetY += distanceY;

    boolean needUpdate = false;
    while (offsetX < -(Sector.SECTOR_SIZE / 2)) {
      offsetX += Sector.SECTOR_SIZE;
      sectorX++;
      needUpdate = true;
    }
    while (offsetX > (Sector.SECTOR_SIZE / 2)) {
      offsetX -= Sector.SECTOR_SIZE;
      sectorX--;
      needUpdate = true;
    }
    while (offsetY < -(Sector.SECTOR_SIZE / 2)) {
      offsetY += Sector.SECTOR_SIZE;
      sectorY++;
      needUpdate = true;
    }
    while (offsetY > (Sector.SECTOR_SIZE / 2)) {
      offsetY -= Sector.SECTOR_SIZE;
      sectorY--;
      needUpdate = true;
    }

    if (needUpdate) {
      scrollTo(sectorX, sectorY, -offsetX, -offsetY);
    }
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (isInEditMode()) {
      return;
    }
    super.onDraw(canvas);

    if (scrollToCentre) {
      scroll(getWidth() / 2.0f / getPixelScale(), getHeight() / 2.0f / getPixelScale());
      scrollToCentre = false;
    }
  }
}
