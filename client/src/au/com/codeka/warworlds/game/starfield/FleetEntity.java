package au.com.codeka.warworlds.game.starfield;

import java.util.Locale;

import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.vbo.DrawType;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.FleetUpgrade;

/** An entity that represents a moving fleet. */
public class FleetEntity extends SelectableEntity {
    private static final Logger log = LoggerFactory.getLogger(FleetEntity.class);
    private StarfieldSceneManager mStarfield;
    private Vector2 mSrcPoint;
    private Vector2 mDestPoint;
    private Fleet mFleet;
    private FleetSprite mFleetSprite;
    private AnimatedSprite mBoostSprite;

    private Line mSelectionLine;

    public FleetEntity(StarfieldSceneManager starfield, Vector2 srcPoint, Vector2 destPoint, Fleet fleet,
                       VertexBufferObjectManager vertexBufferObjectManager) {
        super(0.0f, 0.0f, 1.0f, 1.0f);
        mStarfield = starfield;
        mSrcPoint = srcPoint;
        mDestPoint = destPoint;
        mFleet = fleet;
        log.debug("Adding fleet: from=("+mSrcPoint.x+", "+mSrcPoint.y+") to=("+mDestPoint.x+", "+mDestPoint.y+")");
        setup(starfield, vertexBufferObjectManager);
    }

    @Override
    public Entity getTouchEntity() {
        return mFleetSprite;
    }

    @Override
    public void onSelected(SelectionIndicatorEntity selectionIndicator) {
        selectionIndicator.setScale(20.0f);

        if (mSelectionLine == null) {
            mSelectionLine = new Line(0.0f, 0.0f, 0.0f, 0.0f,
                    mStarfield.getActivity().getVertexBufferObjectManager());
            mSelectionLine.setColor(0.0f, 1.0f, 0.0f);
            mSelectionLine.setZIndex(-1);
        }

        float x = getX();
        float y = getY();
        mSelectionLine.setPosition((float) mSrcPoint.x - x, (float) mSrcPoint.y - y,
                                   (float) mDestPoint.x - x, (float) mDestPoint.y - y);
        attachChild(mSelectionLine);
        sortChildren();
    }

    @Override
    public void onDeselected(SelectionIndicatorEntity selectionIndicator) {
        detachChild(mSelectionLine);
    }

    public Fleet getFleet() {
        return mFleet;
    }

    public void setup(StarfieldSceneManager starfield, VertexBufferObjectManager vertexBufferObjectManager) {
        // work out how far along the fleet has moved so we can draw the icon at the correct
        // spot. Also, we'll draw the name of the empire, number of ships etc.
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mFleet.getDesignID());
        float timeToDestinationInHours = mFleet.getTimeToDestination();
        float timeFromSourceInHours = mFleet.getTimeFromSource();

        double fractionComplete = timeFromSourceInHours / (timeFromSourceInHours + timeToDestinationInHours);
        if (fractionComplete > 1.0) {
            fractionComplete = 1.0;
        }

        ITextureRegion textureRegion = mStarfield.getSpriteTexture(design.getSpriteName());
        float spriteWidth = textureRegion.getWidth();
        float spriteHeight = textureRegion.getHeight();
        float aspect = spriteWidth / spriteHeight;
        if (spriteWidth > 40.0f) {
            spriteWidth = 40.0f;
            spriteHeight = 40.0f / aspect;
        }
        if (spriteHeight > 40.0f) {
            spriteWidth = 40.0f * aspect;
            spriteHeight = 40.0f;
        }

        Vector2 up = Vector2.pool.borrow().reset(1.0f, 0.0f);
        Vector2 direction = Vector2.pool.borrow().reset(mDestPoint);
        direction.subtract(mSrcPoint);
        direction.normalize();
        float angle = Vector2.angleBetweenCcw(up, direction);
        Vector2.pool.release(direction); direction = null;

        mFleetSprite = new FleetSprite(spriteWidth, spriteHeight, (float)(angle * 180.0f / Math.PI),
                textureRegion, vertexBufferObjectManager);
        attachChild(mFleetSprite);

        FleetUpgrade.BoostFleetUpgrade boostUpgrade = (FleetUpgrade.BoostFleetUpgrade) mFleet.getUpgrade("boost");
        if (boostUpgrade != null && boostUpgrade.isBoosting()) {
            mBoostSprite = new AnimatedSprite(9.0f, -5.0f, 12.0f, 12.0f, (ITiledTextureRegion) mStarfield.getSpriteTexture("ship.upgrade.boost"),
                    vertexBufferObjectManager, DrawType.STATIC);
            mFleetSprite.attachChild(mBoostSprite);
            mBoostSprite.animate(100);
        }

        Vector2 location = getLocation((float) fractionComplete);
/*
        // check if there's any other fleets nearby and offset this one by a bit so that they
        // don't overlap
        Random rand = new Random(mFleet.getKey().hashCode());
        ArrayList<FleetEntity> existingFleets = new ArrayList<FleetEntity>(starfield.getMovingFleets());
        for (int i = 0; i < existingFleets.size(); i++) {
            FleetEntity existingFleet = existingFleets.get(i);
            Vector2 existingPosition = Vector2.pool.borrow().reset(existingFleet.getX(), existingFleet.getY());
            if (existingPosition.distanceTo(location) < 30.0f) {
                // pick a random direction and offset it a bit
                Vector2 offset = Vector2.pool.borrow().reset(0, 40.0);
                offset.rotate(rand.nextFloat() * 2 * (float) Math.PI);
                location.add(offset);
                Vector2.pool.release(offset);
                i = 0;
            }
        }
*/
        setPosition((float) location.x, (float) location.y);
        Vector2.pool.release(location);

        Empire emp = EmpireManager.i.getEmpire(mFleet.getEmpireKey());
        if (emp != null) {
            ITextureRegion texture = EmpireShieldManager.i.getShieldTexture(mStarfield.getActivity(), emp);
            Vector2 pt = Vector2.pool.borrow().reset(30.0f, 0.0f);
            if (texture != null) {
                Sprite shieldSprite = new Sprite(
                        (float) pt.x, (float) pt.y,
                        20.0f, 20.0f, texture, mStarfield.getActivity().getVertexBufferObjectManager());
                attachChild(shieldSprite);
            }

            String name;
            if (mFleet.getNumShips() > 1.0f) {
                name = String.format(Locale.ENGLISH, "%d × %s", (int) mFleet.getNumShips(), design.getDisplayName());
            } else {
                name = String.format(Locale.ENGLISH, "%s", design.getDisplayName());
            }
            Text fleetName = new Text((float) pt.x, (float) pt.y, mStarfield.getFont(),
                    name, mStarfield.getActivity().getVertexBufferObjectManager());
            fleetName.setScale(0.666f);
            float offset = ((fleetName.getLineWidthMaximum() * 0.666f) / 2.0f) + 14.0f;
            fleetName.setX(fleetName.getX() + offset);
            attachChild(fleetName);

            Vector2.pool.release(pt); pt = null;
        }
    }

    private Vector2 getLocation(float fractionComplete) {
        // we don't want to start the fleet over the top of the star, so we'll offset it a bit
        double distance = mSrcPoint.distanceTo(mDestPoint) - 40.0f;
        if (distance < 0) {
            distance = 0;
        }

        Vector2 direction = Vector2.pool.borrow().reset(mDestPoint);
        direction.subtract(mSrcPoint);
        direction.normalize();

        Vector2 location = Vector2.pool.borrow().reset(direction);
        location.scale(distance * fractionComplete);
        location.add(mSrcPoint);

        direction.scale(20.0f);
        location.add(direction);

        return location;
    }

    private class FleetSprite extends Sprite {
        public FleetSprite(float width, float height, float rotation, ITextureRegion textureRegion,
                VertexBufferObjectManager vertexBufferObjectManager) {
            super(0.0f, 0.0f, width, height, textureRegion, vertexBufferObjectManager);
            setRotation(rotation);
        }

        @Override
        public boolean onAreaTouched(final TouchEvent sceneTouchEvent,
                                     final float touchAreaLocalX,
                                     final float touchAreaLocalY) {
            if (sceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
                mStarfield.setSelectingEntity(FleetEntity.this);
            } else if (sceneTouchEvent.getAction() == TouchEvent.ACTION_UP) {
                SelectableEntity selectingEntity = mStarfield.getSelectingEntity();
                if (selectingEntity == FleetEntity.this) {
                    mStarfield.selectFleet(FleetEntity.this);
                    return true;
                }
            }
            return false;
        }
    }
}
