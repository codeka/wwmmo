package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import au.com.codeka.common.Log;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BuildingDesign;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.warworlds.ctrl.SelectionView;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.DesignHelper;
import au.com.codeka.warworlds.game.StarfieldBackgroundRenderer;
import au.com.codeka.warworlds.game.UniverseElementSurfaceView;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.ImageManager;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.PlanetImageManager;
import au.com.codeka.warworlds.model.ShieldManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarImageManager;

/**
 * \c SurfaceView that displays a solar system. Star in the top-left, planets arrayed around,
 * and representations of the fleets, etc.
 */
public class SolarSystemSurfaceView extends UniverseElementSurfaceView {
  private static final Log log = new Log("SolarSystemSUrfaceView");
  private Context context;
  private Star star;
  private PlanetInfo[] planetInfos;
  private PlanetInfo selectedPlanet;
  private Paint planetPaint;
  private SelectionView selectionView;
  private CopyOnWriteArrayList<OnPlanetSelectedListener> planetSelectedListeners;
  private StarfieldBackgroundRenderer backgroundRenderer;
  private Matrix matrix;
  private final HashMap<String, DesignHelper.PendingAsyncLoad> designBitmaps = new HashMap<>();

  private final Comparator<Building> buildingDesignComparator =
      (lhs, rhs) -> lhs.getDesignID().compareTo(rhs.getDesignID());

  public SolarSystemSurfaceView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    planetSelectedListeners = new CopyOnWriteArrayList<>();

    planetPaint = new Paint();
    planetPaint.setARGB(255, 255, 255, 255);
    planetPaint.setStyle(Style.STROKE);
    matrix = new Matrix();
  }

  public void setSelectionView(SelectionView selectionView) {
    this.selectionView = selectionView;
    if (this.selectionView != null) {
      this.selectionView.setVisibility(View.GONE);
    }
  }

  public void setStar(Star star) {
    this.star = star;

    BasePlanet[] planets = this.star.getPlanets();
    planetInfos = new PlanetInfo[planets.length];

    for (int i = 0; i < planets.length; i++) {
      PlanetInfo planetInfo = new PlanetInfo();
      planetInfo.planet = (Planet) planets[i];
      planetInfo.centre = new Vector2(0, 0);
      planetInfo.distanceFromSun = 0.0f;
      planetInfos[i] = planetInfo;
    }

    placePlanets();
    redraw();
  }

  /**
   * Gets a \c Point2D representing the centre of the given planet, relative to this
   * \c SolarSystemSurfaceView in device pixels.
   */
  public Vector2 getPlanetCentre(Planet planet) {
    if (planetInfos == null) {
      return null;
    }

    for (PlanetInfo planetInfo : planetInfos) {
      if (planetInfo.planet.getIndex() == planet.getIndex()) {
        Vector2 pixels = planetInfo.centre;
        return new Vector2(pixels.x / getPixelScale(),
            pixels.y / getPixelScale());
      }
    }

    return null;
  }

  private void placePlanets() {
    int width = getWidth();

    float planetStart = 150 * getPixelScale();
    float distanceBetweenPlanets = width - planetStart;
    distanceBetweenPlanets /= planetInfos.length;

    for (int i = 0; i < planetInfos.length; i++) {
      PlanetInfo planetInfo = planetInfos[i];

      float distanceFromSun =
          planetStart + (distanceBetweenPlanets * i) + (distanceBetweenPlanets / 2.0f);
      float x = 0;
      float y = -1 * distanceFromSun;

      float angle = (0.5f / (planetInfos.length + 1));
      angle = (float) ((angle * i * Math.PI) + angle * Math.PI);

      Vector2 centre = new Vector2(x, y);
      centre.rotate(angle);
      centre.y *= -1;

      planetInfo.centre = centre;
      planetInfo.distanceFromSun = distanceFromSun;

      List<BaseColony> colonies = star.getColonies();
      if (colonies != null && !colonies.isEmpty()) {
        for (BaseColony colony : colonies) {
          if (colony.getPlanetIndex() == planetInfos[i].planet.getIndex()) {
            planetInfo.colony = (Colony) colony;
            planetInfo.buildings = new ArrayList<>();

            for (BaseBuilding building : colony.getBuildings()) {
              BuildingDesign design =
                  (BuildingDesign) DesignManager.i.getDesign(
                      DesignKind.BUILDING, building.getDesignID());
              if (design.showInSolarSystem()) {
                planetInfo.buildings.add((Building) building);
                if (!designBitmaps.containsKey(design.getID())) {
                  designBitmaps.put(
                      design.getID(),
                      DesignHelper.loadAsync(design, () -> {
                        log.info("loaded a design");
                        post(this::redraw);
                      }));
                }
              }
            }

            if (!planetInfo.buildings.isEmpty()) {
              Collections.sort(planetInfo.buildings, buildingDesignComparator);
            }
          }
        }
      }

      planetInfos[i] = planetInfo;
    }

    updateSelection();
  }

  public void addPlanetSelectedListener(OnPlanetSelectedListener listener) {
    if (!planetSelectedListeners.contains(listener)) {
      planetSelectedListeners.add(listener);
    }
  }

  public void removePlanetSelectedListener(OnPlanetSelectedListener listener) {
    planetSelectedListeners.remove(listener);
  }

  protected void firePlanetSelected(Planet planet) {
    for (OnPlanetSelectedListener listener : planetSelectedListeners) {
      listener.onPlanetSelected(planet);
    }
  }

  public Planet getSelectedPlanet() {
    return selectedPlanet.planet;
  }

  public void selectPlanet(int planetIndex) {
    for (PlanetInfo planetInfo : planetInfos) {
      if (planetInfo.planet.getIndex() == planetIndex) {
        selectedPlanet = planetInfo;

        firePlanetSelected(selectedPlanet.planet);

        if (selectedPlanet != null && selectionView != null) {
          updateSelection();
        } else if (selectionView != null) {
          selectionView.setVisibility(View.GONE);
        }
      }
    }
  }

  private void updateSelection() {
    if (selectedPlanet != null && selectionView != null) {
      selectionView.setVisibility(View.VISIBLE);

      RelativeLayout.LayoutParams params =
          (RelativeLayout.LayoutParams) selectionView.getLayoutParams();
      int size =
          (int) ((((selectedPlanet.planet.getSize() - 10.0) / 8.0) + 4.0) * 10.0)
              + (int) (40 * getPixelScale());
      params.width = size;
      params.height = size;
      params.leftMargin = (int) (getLeft() + selectedPlanet.centre.x - (params.width / 2));
      params.topMargin = (int) (getTop() + selectedPlanet.centre.y - (params.height / 2));
      selectionView.setLayoutParams(params);
    }
  }

  /**
   * Creates the \c OnGestureListener that'll handle our gestures.
   */
  @Override
  protected GestureDetector.OnGestureListener createGestureListener() {
    return new GestureListener();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    ShieldManager.eventBus.register(eventHandler);
    ImageManager.eventBus.register(eventHandler);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    ImageManager.eventBus.unregister(eventHandler);
    if (backgroundRenderer != null) {
      backgroundRenderer.close();
      backgroundRenderer = null;
    }
    ShieldManager.eventBus.unregister(eventHandler);
  }

  @Override
  public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    if (planetInfos == null) {
      return;
    }
    placePlanets();
  }

  /**
   * Draws the actual starfield to the given \c Canvas. This will be called in
   * a background thread, so we can't do anything UI-specific, except drawing
   * to the canvas.
   */
  @Override
  public void onDraw(Canvas canvas) {
    if (isInEditMode()) {
      return;
    }

    if (star != null) {
      canvas.drawColor(Color.BLACK);

      if (backgroundRenderer == null) {
        backgroundRenderer = new StarfieldBackgroundRenderer(new long[]{star.getKey().hashCode()});
      }
      backgroundRenderer.drawBackground(
          canvas, 0, 0, getWidth() / getPixelScale(), getHeight() / getPixelScale());

      drawSun(canvas);
      drawPlanets(canvas);
    }

    drawOverlays(canvas);
  }

  private void drawSun(Canvas canvas) {
    float pixelScale = getPixelScale();

    int imageSize = (int) (300.0f * pixelScale);
    Sprite sprite = StarImageManager.getInstance().getSprite(star, imageSize, false);

    matrix.reset();
    matrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
    matrix.postScale(300.0f * pixelScale / sprite.getWidth(),
        300.0f * pixelScale / sprite.getHeight());
    canvas.save();
    canvas.concat(matrix);
    sprite.draw(canvas);
    canvas.restore();
  }

  private void drawPlanets(Canvas canvas) {
    for (PlanetInfo info : planetInfos) {
      canvas.drawCircle(0, 0,
          info.distanceFromSun, planetPaint);
    }

    PlanetImageManager pim = PlanetImageManager.getInstance();

    for (final PlanetInfo planetInfo : planetInfos) {
      Sprite sprite = pim.getSprite(planetInfo.planet);
      matrix.reset();
      matrix.postTranslate(-(sprite.getWidth() / 2.0f), -(sprite.getHeight() / 2.0f));
      matrix.postScale(100.0f * getPixelScale() / sprite.getWidth(),
          100.0f * getPixelScale() / sprite.getHeight());
      matrix.postTranslate((float) planetInfo.centre.x, (float) planetInfo.centre.y);
      canvas.save();
      canvas.concat(matrix);
      sprite.draw(canvas);
      canvas.restore();

      if (planetInfo.buildings != null) {
        int j = 0;
        float angleOffset = (float) (Math.PI / 4.0) * (planetInfo.buildings.size() - 1) / 2.0f;
        for (Building building : planetInfo.buildings) {
          BuildingDesign design = building.getDesign();
          DesignHelper.PendingAsyncLoad pendingLoad = designBitmaps.get(design.getID());
          if (pendingLoad == null) {
            // shouldn't happen.
            continue;
          }

          Bitmap bitmap = pendingLoad.bitmap;
          if (bitmap != null) {
            Vector2 pt = Vector2.pool.borrow().reset(0, -30.0f);
            pt.rotate(angleOffset - (float) (Math.PI / 4.0) * j);

            matrix.reset();
            matrix.postTranslate(-(bitmap.getWidth() / 2.0f), -(bitmap.getHeight() / 2.0f));
            matrix.postScale(20.0f * getPixelScale() / bitmap.getWidth(),
                20.0f * getPixelScale() / bitmap.getHeight());
            matrix.postTranslate((float) (planetInfo.centre.x + (pt.x * getPixelScale())),
                (float) (planetInfo.centre.y + (pt.y * getPixelScale())));

            canvas.save();
            canvas.drawBitmap(bitmap, matrix, planetPaint);
            canvas.restore();
          }

          j++;
        }
      }

      if (planetInfo.colony != null) {
        Empire empire = EmpireManager.i.getEmpire(planetInfo.colony.getEmpireID());
        if (empire != null) {
          Bitmap shield = EmpireShieldManager.i.getShield(context, empire);
          if (shield != null) {
            matrix.reset();
            matrix.postTranslate(-shield.getWidth() / 2.0f, -shield.getHeight() / 2.0f);
            matrix.postScale(20.0f * getPixelScale() / shield.getWidth(),
                20.0f * getPixelScale() / shield.getHeight());
            matrix.postTranslate((float) planetInfo.centre.x,
                (float) planetInfo.centre.y + (30.0f * getPixelScale()));
            canvas.drawBitmap(shield, matrix, planetPaint);
          }
        }
      }
    }
  }

  private final Object eventHandler = new Object() {
    @EventHandler
    public void onSpriteGenerated(ImageManager.SpriteGeneratedEvent event) {
      redraw();
    }

    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      invalidate();
    }
  };

  /**
   * Implements the \c OnGestureListener methods that we use to respond to
   * various touch events.
   */
  private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
      if (planetInfos == null) {
        return false;
      }

      Vector2 tapLocation = Vector2.pool.borrow().reset(e.getX(), e.getY());
      PlanetInfo closestPlanet = null;
      for (PlanetInfo planetInfo : planetInfos) {
        if (closestPlanet == null) {
          closestPlanet = planetInfo;
        } else {
          double distanceToClosest = tapLocation.distanceTo(closestPlanet.centre);
          double distanceToThis = tapLocation.distanceTo(planetInfo.centre);
          if (distanceToThis < distanceToClosest) {
            closestPlanet = planetInfo;
          }
        }
      }

      PlanetInfo newSelection = null;
      if (closestPlanet != null &&
          tapLocation.distanceTo(closestPlanet.centre) < 60.0 * getPixelScale()) {
        newSelection = closestPlanet;
      }

      if (newSelection != null) {
        selectPlanet(newSelection.planet.getIndex());

        // play the 'click' sound effect
        playSoundEffect(android.view.SoundEffectConstants.CLICK);
      }

      Vector2.pool.release(tapLocation);
      return false;
    }
  }

  /**
   * This class contains info about the planets we need to render and interact with.
   *
   * @author dean@codeka.com.au
   */
  private static class PlanetInfo {
    public Planet planet;
    public Vector2 centre;
    public float distanceFromSun;
    public Colony colony;
    public ArrayList<Building> buildings;
  }

  /**
   * This interface should be implemented when you want to listen for "planet selected"
   * events -- that is, when the user selects a new planet (by tapping on it).
   */
  public interface OnPlanetSelectedListener {
    /**
     * This is called when the user selects (by tapping it) a planet. By definition, only
     * one planet can be selected at a time.
     */
    void onPlanetSelected(Planet planet);
  }
}
