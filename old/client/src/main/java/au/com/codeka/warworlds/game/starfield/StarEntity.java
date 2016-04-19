package au.com.codeka.warworlds.game.starfield;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import android.util.SparseArray;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;

/** An entity that represents a star in the main starfield view. */
public class StarEntity extends SelectableEntity {
  private StarfieldSceneManager starfield;
  private StarSprite starSprite;
  private Star star;

  public StarEntity(StarfieldSceneManager starfield, Star star, float x, float y,
      ITextureRegion textureRegion, VertexBufferObjectManager vertexBufferObjectManager,
      boolean showText, float textAlpha) {
    super(0.0f, 0.0f, 1.0f, 1.0f);
    this.star = star;
    this.starfield = starfield;

    final float starSize = star.getSize() * 2.0f;
    starSprite = new StarSprite(starSize, textureRegion, vertexBufferObjectManager);
    if (star.getStarType().getImageScale() > 1.0) {
      starSprite.setScale((float) star.getStarType().getImageScale());
    }
    starSprite.setRotation(
        new Random(Integer.parseInt(star.getKey()) * 100000l).nextFloat() * 360.0f);
    attachChild(starSprite);

    if (star.hasAttachedEntities()) {
      for (Entity child : star.getAttachedEntities()) {
        if (child.hasParent()) {
          child.getParent().detachChild(child);
        }
        attachChild(child);
      }
    }

    // if it's a wormhole, we want to register an updater to apply the rotation
    if (star.getStarType().getType() == Star.Type.Wormhole) {
      starfield.getActivity().getEngine().registerUpdateHandler(new IUpdateHandler() {
        @Override
        public void onUpdate(float dt) {
          float rotation = starSprite.getRotation() + 15.0f * dt;
          while (rotation > 360.0f) {
            rotation -= 360.0f;
          }
          starSprite.setRotation(rotation);
        }

        @Override
        public void reset() {
        }
      });
    }

    // don't display the name for marker stars...
    if (showText && star.getStarType().getType() != Star.Type.Marker) {
      Text starName = new Text(0.0f, -starSize * 0.6f, this.starfield.getFont(), star.getName(),
          this.starfield.getActivity().getVertexBufferObjectManager());
      attachChild(starName);
    }

    if (showText) {
      addEmpireIcons(textAlpha);
    }

    setPosition(x, y);
  }

  @Override
  public Entity getTouchEntity() {
    return starSprite;
  }

  @Override
  public void onSelected(SelectionIndicatorEntity selectionIndicator) {
    selectionIndicator.setScale(star.getSize());
  }

  @Override
  public void onDeselected(SelectionIndicatorEntity selectionIndicator) {
  }

  @Override
  public void dispose() {
    super.dispose();
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      getChildByIndex(i).dispose();
    }
  }

  public Star getStar() {
    return star;
  }

  public void setStar(Star s) {
    if (s == null || !s.getKey().equals(star.getKey())) {
      throw new RuntimeException("Can only be used to refresh the existing star!");
    }
    star = s;
  }

  private void addEmpireIcons(float alpha) {
    List<BaseColony> colonies = star.getColonies();
    SparseArray<Integer[]> empires = new SparseArray<>();
    if (colonies != null && !colonies.isEmpty()) {
      for (int i = 0; i < colonies.size(); i++) {
        BaseColony colony = colonies.get(i);
        if (colony.getEmpireKey() == null) {
          continue;
        }

        Empire emp = EmpireManager.i.getEmpire(((Colony) colony).getEmpireID());
        if (emp != null) {
          Integer[] counts = empires.get(emp.getID());
          if (counts == null) {
            counts = new Integer[] {0, 0, 0};
            empires.put(emp.getID(), counts);
          }
          counts[0] += 1;
        }
      }
    }

    List<BaseFleet> fleets = star.getFleets();
    if (fleets != null && !fleets.isEmpty()) {
      for (int i = 0; i < fleets.size(); i++) {
        BaseFleet f = fleets.get(i);
        if (f.getEmpireKey() == null || f.getState() == BaseFleet.State.MOVING) {
          // ignore moving fleets, we'll draw them separately
          continue;
        }

        Empire emp = EmpireManager.i.getEmpire(((Fleet) f).getEmpireID());
        if (emp != null) {
          Integer[] counts = empires.get(emp.getID());
          if (counts == null) {
            counts = new Integer[] {0, 0, 0};
            empires.put(emp.getID(), counts);
          }
          if (f.getDesignID().equals("fighter")) {
            counts[1] += (int) Math.ceil(f.getNumShips());
          } else {
            counts[2] += (int) Math.ceil(f.getNumShips());
          }
        }
      }
    }

    for (int i = 0; i < empires.size(); i++) {
      int empireID = empires.keyAt(i);
      Integer[] counts = empires.valueAt(i);
      Empire emp = EmpireManager.i.getEmpire(empireID);
      ITextureRegion texture = EmpireShieldManager.i.getShieldTexture(starfield.getActivity(), emp);

      Vector2 pt = Vector2.pool.borrow().reset(0, 30.0f);
      pt.rotate(-(float) (Math.PI / 4.0) * (i + 1));

      Sprite shieldSprite = new Sprite((float) pt.x, (float) pt.y, 20.0f, 20.0f, texture,
          starfield.getActivity().getVertexBufferObjectManager());
      shieldSprite.setAlpha(alpha);
      attachChild(shieldSprite);

      String text;
      if (counts[1] == 0 && counts[2] == 0) {
        text = String.format(Locale.ENGLISH, "%d", counts[0]);
      } else if (counts[0] == 0) {
        text = String.format(Locale.ENGLISH, "[%d, %d]", counts[1], counts[2]);
      } else {
        text = String.format(Locale.ENGLISH, "%d ● [%d, %d]", counts[0], counts[1], counts[2]);
      }
      Text empireCounts = new Text((float) pt.x, (float) pt.y, starfield.getFont(), text,
          starfield.getActivity().getVertexBufferObjectManager());
      empireCounts.setAlpha(alpha);
      empireCounts.setScale(0.666f);
      float offset = ((empireCounts.getLineWidthMaximum() * 0.666f) / 2.0f) + 14.0f;
      empireCounts.setX(empireCounts.getX() + offset);
      attachChild(empireCounts);

      Vector2.pool.release(pt);
    }
  }

  private class StarSprite extends Sprite {
    public StarSprite(float size, ITextureRegion textureRegion,
        VertexBufferObjectManager vertexBufferObjectManager) {
      super(0.0f, 0.0f, size, size, textureRegion, vertexBufferObjectManager);
    }

    @Override
    public boolean onAreaTouched(final TouchEvent sceneTouchEvent, final float touchAreaLocalX,
        final float touchAreaLocalY) {
      if (star.getStarType().getType() == Star.Type.Marker) {
        // you can't select markers
        return false;
      }

      StarfieldScene scene = starfield.getScene();
      if (scene != null && sceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
        scene.setSelectingEntity(StarEntity.this);
      } else if (scene != null && sceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
        SelectableEntity selectingEntity = scene.getSelectingEntity();
        if (selectingEntity == StarEntity.this) {
          scene.selectStar(StarEntity.this);
          return true;
        }
      }
      return false;
    }
  }
}
