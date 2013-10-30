package au.com.codeka.warworlds.game.starfield;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.andengine.entity.Entity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
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
        super(x, y, star.getSize() * 2.0f, star.getSize() * 2.0f);
        mStar = star;
        mStarfield = starfield;

        final float starSize = star.getSize() * 2.0f;
        mStarSprite = new StarSprite(starSize, textureRegion, vertexBufferObjectManager);
        if (star.getStarType().getImageScale() > 1.0) {
            mStarSprite.setScale((float) star.getStarType().getImageScale());
        }
        mStarSprite.setRotation(new Random(Integer.parseInt(star.getKey()) * 100000l).nextFloat() * 360.0f);
        mStarName = new Text(0.0f, -starSize * 0.6f,
                             mStarfield.getFont(), star.getName(),
                             mStarfield.getActivity().getVertexBufferObjectManager());

        attachChild(mStarSprite);
        attachChild(mStarName);

        addEmpireIcons();
        addFleetIcons();
    }

    public Entity getTouchEntity() {
        return mStarSprite;
    }

    public Star getStar() {
        return mStar;
    }

    private void addEmpireIcons() {
        List<BaseColony> colonies = mStar.getColonies();
        if (colonies != null && !colonies.isEmpty()) {
            Map<String, Integer> colonyEmpires = new TreeMap<String, Integer>();

            for (int i = 0; i < colonies.size(); i++) {
                BaseColony colony = colonies.get(i);
                if (colony.getEmpireKey() == null) {
                    continue;
                }

                Empire emp = EmpireManager.i.getEmpire(colony.getEmpireKey());
                if (emp != null) {
                    Integer n = colonyEmpires.get(emp.getKey());
                    if (n == null) {
                        n = 1;
                        colonyEmpires.put(emp.getKey(), n);
                    } else {
                        colonyEmpires.put(emp.getKey(), n+1);
                    }
                }
            }

            int i = 1;
            for (String empireKey : colonyEmpires.keySet()) {
                Integer n = colonyEmpires.get(empireKey);
                Empire emp = EmpireManager.i.getEmpire(empireKey);
                ITextureRegion texture = EmpireShieldManager.i.getShieldTexture(mStarfield.getActivity(), emp);

                Vector2 pt = Vector2.pool.borrow().reset(0, 30.0f);
                pt.rotate(-(float)(Math.PI / 4.0) * i);

                Sprite shieldSprite = new Sprite(
                        (float) pt.x, (float) pt.y,
                        20.0f, 20.0f, texture, mStarfield.getActivity().getVertexBufferObjectManager());
                attachChild(shieldSprite);

                String name;
                if (n.equals(1)) {
                    name = emp.getDisplayName();
                } else {
                    name = String.format(Locale.ENGLISH, "%s (%d)", emp.getDisplayName(), n);
                }

                Text empireName = new Text((float) pt.x, (float) pt.y, mStarfield.getFont(),
                                            name, mStarfield.getActivity().getVertexBufferObjectManager());
                empireName.setScale(0.666f);
                float offset = ((empireName.getLineWidthMaximum() * 0.666f) / 2.0f) + 14.0f;
                empireName.setX(empireName.getX() + offset);
                attachChild(empireName);

                Vector2.pool.release(pt); pt = null;
                i++;
            }
        }
    }

    private void addFleetIcons() {
        List<BaseFleet> fleets = mStar.getFleets();
        if (fleets != null && !fleets.isEmpty()) {
            Map<String, Integer> empireFleets = new TreeMap<String, Integer>();
            for (int i = 0; i < fleets.size(); i++) {
                BaseFleet f = fleets.get(i);
                if (f.getEmpireKey() == null || f.getState() == BaseFleet.State.MOVING) {
                    // ignore moving fleets, we'll draw them separately
                    continue;
                }

                Integer n = empireFleets.get(f.getEmpireKey());
                if (n == null) {
                    empireFleets.put(f.getEmpireKey(), (int) Math.ceil(f.getNumShips()));
                } else {
                    empireFleets.put(f.getEmpireKey(), n + (int) Math.ceil(f.getNumShips()));
                }
            }

            int i = 0;
            for (String empireKey : empireFleets.keySet()) {
                Integer numShips = empireFleets.get(empireKey);
                Empire emp = EmpireManager.i.getEmpire(empireKey);
                if (emp != null) {
                    Vector2 pt = Vector2.pool.borrow().reset(0, 30.0f);
                    pt.rotate((float)(Math.PI / 4.0) * i);

                    Sprite iconSprite = new Sprite(
                            (float) pt.x, (float) pt.y,
                            20.0f, 20.0f, mStarfield.getFleetIconTextureRegion(),
                            mStarfield.getActivity().getVertexBufferObjectManager());
                    attachChild(iconSprite);

                    String name = String.format(Locale.ENGLISH, "%s (%d)", emp.getDisplayName(), numShips);
                    Text empireName = new Text((float) pt.x, (float) pt.y, mStarfield.getFont(),
                            name, mStarfield.getActivity().getVertexBufferObjectManager());
                    empireName.setScale(0.666f);
                    float offset = ((empireName.getLineWidthMaximum() * 0.666f) / 2.0f) + 14.0f;
                    empireName.setX(empireName.getX() - offset);
                    attachChild(empireName);

                    Vector2.pool.release(pt); pt = null;
                }

                i++;
            }
        }
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
                    return true;
                }
            }
            return false;
        }
    }
}
