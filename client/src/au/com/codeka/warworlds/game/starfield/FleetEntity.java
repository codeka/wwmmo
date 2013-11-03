package au.com.codeka.warworlds.game.starfield;

import org.andengine.entity.Entity;
import org.andengine.entity.sprite.Sprite;
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
public class FleetEntity extends Entity {
    private static final Logger log = LoggerFactory.getLogger(FleetEntity.class);
    private StarfieldSceneManager mStarfield;
    private Vector2 mSrcPoint;
    private Vector2 mDestPoint;
    private Fleet mFleet;

    public FleetEntity(StarfieldSceneManager starfield, Vector2 srcPoint, Vector2 destPoint, Fleet fleet,
                       VertexBufferObjectManager vertexBufferObjectManager) {
        super(0.0f, 0.0f, getIconWidth(starfield, fleet), getIconHeight(starfield, fleet));
        mStarfield = starfield;
        mSrcPoint = srcPoint;
        mDestPoint = destPoint;
        mFleet = fleet;
        setup(vertexBufferObjectManager);
    }

    private static float getIconWidth(StarfieldSceneManager starfield, Fleet fleet) {
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        ITextureRegion textureRegion = starfield.getSpriteTexture(design.getSpriteName());
        return textureRegion.getWidth();
    }

    private static float getIconHeight(StarfieldSceneManager starfield, Fleet fleet) {
        ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());
        ITextureRegion textureRegion = starfield.getSpriteTexture(design.getSpriteName());
        return textureRegion.getWidth();
    }

    public void setup(VertexBufferObjectManager vertexBufferObjectManager) {
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

        // we don't want to start the fleet over the top of the star, so we'll offset it a bit
        distance -= 40.0f;
        if (distance < 0) {
            distance = 0;
        }

        Vector2 direction = Vector2.pool.borrow().reset(mDestPoint);
        direction.subtract(mSrcPoint);
        direction.normalize();

        Vector2 location = Vector2.pool.borrow().reset(direction);
        location.scale(distance * fractionComplete);
        location.add(mSrcPoint);

        ITextureRegion textureRegion = mStarfield.getSpriteTexture(design.getSpriteName());
        Sprite fleetSprite = new Sprite(0.0f, 0.0f, textureRegion.getWidth(), textureRegion.getHeight(),
                textureRegion, vertexBufferObjectManager);
        attachChild(fleetSprite);
        Vector2 up = Vector2.pool.borrow().reset(0, 1.0f); // fleetSprite.getUp();

        float angle = Vector2.angleBetween(up, direction);

        direction.scale(20.0f);
        location.add(direction);
        Vector2.pool.release(direction); direction = null;

        setPosition((float) location.x, (float) location.y);
        setRotation((float)(angle * 180.0f / Math.PI));
/*
        // check if there's any other fleets nearby and offset this one by a bit so that they
        // don't overlap
        Random rand = new Random(mFleet.getKey().hashCode());
        for (int i = 0; i < mVisibleEntities.size(); i++) {
            VisibleEntity existing = mVisibleEntities.get(i);
            if (existing.fleet == null) {
                continue;
            }

            if (existing.position.distanceTo(position) < (15.0f * pixelScale)) {
                // pick a random direction and offset it a bit
                Vector2 offset = Vector2.pool.borrow().reset(0, 20.0 * pixelScale);
                offset.rotate(rand.nextFloat() * 2 * (float) Math.PI);
                position.add(offset);
                Vector2.pool.release(offset);
                i = -1; // start looping again...
            }
        }

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
}
