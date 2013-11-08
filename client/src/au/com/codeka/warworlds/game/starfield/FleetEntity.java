package au.com.codeka.warworlds.game.starfield;

import java.util.ArrayList;
import java.util.Random;

import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.Fleet;

/** An entity that represents a moving fleet. */
public class FleetEntity extends SelectableEntity {
    private static final Logger log = LoggerFactory.getLogger(FleetEntity.class);
    private StarfieldSceneManager mStarfield;
    private Vector2 mSrcPoint;
    private Vector2 mDestPoint;
    private Fleet mFleet;
    private FleetSprite mFleetSprite;

    public FleetEntity(StarfieldSceneManager starfield, Vector2 srcPoint, Vector2 destPoint, Fleet fleet,
                       VertexBufferObjectManager vertexBufferObjectManager) {
        super(0.0f, 0.0f, 1.0f, 1.0f);
        mStarfield = starfield;
        mSrcPoint = srcPoint;
        mDestPoint = destPoint;
        mFleet = fleet;
        setup(starfield, vertexBufferObjectManager);
    }

    public Entity getTouchEntity() {
        return mFleetSprite;
    }

    public Fleet getFleet() {
        return mFleet;
    }

    public void setup(StarfieldSceneManager starfield, VertexBufferObjectManager vertexBufferObjectManager) {
        // work out how far along the fleet has moved so we can draw the icon at the correct
        // spot. Also, we'll draw the name of the empire, number of ships etc.
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mFleet.getDesignID());
        double distance = mSrcPoint.distanceTo(mDestPoint);
        double totalTimeInHours = (distance / 10.0) / design.getSpeedInParsecPerHour();

        DateTime startTime = mFleet.getStateStartTime();
        DateTime now = DateTime.now(DateTimeZone.UTC);
        float timeSoFarInHours = Seconds.secondsBetween(startTime, now).getSeconds() / 3600.0f;

        double fractionComplete = timeSoFarInHours / totalTimeInHours;
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

        Vector2 up = Vector2.pool.borrow().reset(-1.0f, 0.0f); // fleetSprite.getUp();
        Vector2 direction = Vector2.pool.borrow().reset(mDestPoint);
        direction.subtract(mSrcPoint);
        direction.normalize();
        float angle = Vector2.angleBetween(up, direction);
        Vector2.pool.release(direction); direction = null;

        mFleetSprite = new FleetSprite(spriteWidth, spriteHeight, (float)(angle * 180.0f / Math.PI),
                textureRegion, vertexBufferObjectManager);
        attachChild(mFleetSprite);

        Vector2 location = getLocation((float) fractionComplete);

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

        setPosition((float) location.x, (float) location.y);
        Vector2.pool.release(location);
/*

        Empire emp = getEmpire(fleet.getEmpireKey());
        if (emp != null) {
            Bitmap shield = EmpireShieldManager.i.getShield(mContext, emp);
            if (shield != null) {
                mMatrix.reset();
                mMatrix.postTranslate(-(shield.getWidth() / 2.0f), -(shield.getHeight() / 2.0f));
                mMatrix.postScale(16.0f * pixelScale / shield.getWidth(),
                                  16.0f * pixelScale / shield.getHeight());
                mMatrix.postTranslate((float) position.x + (20.0f * pixelScale),
                                      (float) position.y);
                canvas.drawBitmap(shield, mMatrix, mStarPaint);
            }

            String msg = emp.getDisplayName();
            canvas.drawText(msg, (float) position.x + (30.0f * pixelScale),
                            (float) position.y, mStarPaint);

            msg = String.format(Locale.ENGLISH, "%s (%d)", design.getDisplayName(), (int) Math.ceil(fleet.getNumShips()));
            canvas.drawText(msg, (float) position.x + (30.0f * pixelScale),
                            (float) position.y + (10.0f * pixelScale), mStarPaint);
        }
*/
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
