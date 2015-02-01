package au.com.codeka.warworlds.game;

import java.util.Locale;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.Cash;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.game.starfield.BaseStarfieldActivity;
import au.com.codeka.warworlds.game.starfield.SectorSceneManager;
import au.com.codeka.warworlds.game.starfield.StarfieldSceneManager;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.designeffects.EmptySpaceMoverShipEffect;

import com.google.protobuf.InvalidProtocolBufferException;

/** This activity is used to select a location for moves. It's a bit annoying that we have to do it like this... */
public class FleetMoveActivity extends BaseStarfieldActivity {
    private Star srcStar;
    private Star destStar;
    private Fleet fleet;
    private float estimatedCost;
    private FleetIndicatorEntity fleetIndicatorEntity;

    private Star markerStar;
    private RadiusIndicatorEntity tooCloseIndicatorEntity;

    public static void show(Activity activity, Fleet fleet) {
        Intent intent = new Intent(activity, FleetMoveActivity.class);
        Messages.Fleet.Builder fleet_pb = Messages.Fleet.newBuilder();
        fleet.toProtocolBuffer(fleet_pb);
        intent.putExtra("au.com.codeka.warworlds.Fleet", fleet_pb.build().toByteArray());
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        byte[] fleetBytes = extras.getByteArray("au.com.codeka.warworlds.Fleet");
        try {
            Messages.Fleet fleet_pb = Messages.Fleet.parseFrom(fleetBytes);
            fleet = new Fleet();
            fleet.fromProtocolBuffer(fleet_pb);
        } catch (InvalidProtocolBufferException e) {
        }

        super.onCreate(savedInstanceState);

        srcStar = StarManager.i.getStar(Integer.parseInt(fleet.getStarKey()));
        if (srcStar != null) {
            mStarfield.scrollTo(srcStar);
        }

        mStarfield.setSceneCreatedHandler(new SectorSceneManager.SceneCreatedHandler() {
            @Override
            public void onSceneCreated(Scene scene) {
                if (srcStar == null) {
                    return; // shouldn't happen yet, but just in case
                }

                Vector2 srcPoint = mStarfield.getSectorOffset(srcStar.getSectorX(), srcStar.getSectorY());
                srcPoint.add(srcStar.getOffsetX(), Sector.SECTOR_SIZE - srcStar.getOffsetY());
                fleetIndicatorEntity = new FleetIndicatorEntity(mStarfield, srcPoint, fleet, getVertexBufferObjectManager());
                scene.attachChild(fleetIndicatorEntity);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshSelection();
                    }
                });
            }
        });

        Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button moveBtn = (Button) findViewById(R.id.move_btn);
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (destStar != null) {
                    onMoveClick();
                }
            }
        });
    }

    @Override
    public void onResumeFragments() {
        super.onResumeFragments();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeter.ServerGreeting greeting) {
                if (!success) {
                    return;
                }

                // TODO?
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        StarfieldSceneManager.eventBus.register(eventHandler);
        StarManager.eventBus.register(eventHandler);
    }

    @Override
    public void onStop() {
        super.onStop();
        StarfieldSceneManager.eventBus.unregister(eventHandler);
        StarManager.eventBus.unregister(eventHandler);

        // if we have a marker star, make sure we remove it first
        if (markerStar != null) {
            Sector s = SectorManager.i.getSector(markerStar.getSectorX(), markerStar.getSectorY());
            if (s != null) {
                s.getStars().remove(markerStar);
            }
        }
    }

    public Object eventHandler = new Object() {
        @EventHandler
        public void onStarUpdated(Star star) {
            if (star.getID() == Integer.parseInt(fleet.getStarKey())) {
                if (srcStar == null) {
                    srcStar = star;
                    mStarfield.scrollTo(srcStar);
                }
            }
        }

        @EventHandler
        public void onStarSelected(final StarfieldSceneManager.StarSelectedEvent event) {
            if (event.star == null) {
                destStar = null;
                refreshSelection();
                return;
            }

            if (srcStar != null && event.star.getKey().equals(srcStar.getKey())) {
                // if src & dest are the same, just forget about it
                destStar = null;
            } else {
                destStar = event.star;
            }

            refreshSelection();
        }

        @EventHandler
        public void onSpaceTap(final StarfieldSceneManager.SpaceTapEvent event) {
            // if the fleet you're moving has the 'empty space mover' effect, it means you can move it
            // to regions of empty space.
            if (!fleet.getDesign().hasEffect("empty-space-mover")) {
                return;
            }

            // when moving to a region of empty space, we need to place a special "marker" star
            // at the destination (since everything else we do assume you're moving to a star)
            if (markerStar != null) {
                Sector s = SectorManager.i.getSector(markerStar.getSectorX(), markerStar.getSectorY());
                if (s != null) {
                    s.getStars().remove(markerStar);
                }
            }
            markerStar = new Star(BaseStar.getStarType(Star.Type.Marker), "Marker", 20,
                    event.sectorX, event.sectorY, event.offsetX, event.offsetY, null);
            Sector s = SectorManager.i.getSector(event.sectorX, event.sectorY);
            if (s != null) {
                s.getStars().add(markerStar);
            }
            SectorManager.eventBus.publish(s);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStarfield.getScene().selectStar(markerStar.getKey());
                }
            });
        }
    };

    @Override
    protected int getLayoutID() {
        return R.layout.fleet_move;
    }

    private void refreshSelection() {
        final View starDetailsView = findViewById(R.id.star_details);
        final View instructionsView = findViewById(R.id.instructions);
        final TextView starDetailsLeft = (TextView) findViewById(R.id.star_details_left);
        final TextView starDetailsRight = (TextView) findViewById(R.id.star_details_right);

        if (srcStar == null) {
            return;
        }

        Vector2 srcPoint = mStarfield.getSectorOffset(srcStar.getSectorX(), srcStar.getSectorY());
        srcPoint.add(srcStar.getOffsetX(), Sector.SECTOR_SIZE - srcStar.getOffsetY());

        if (destStar == null) {
            instructionsView.setVisibility(View.VISIBLE);
            starDetailsView.setVisibility(View.GONE);
            findViewById(R.id.move_btn).setEnabled(false);
            fleetIndicatorEntity.setPoints(srcPoint, null);

            return;
        } else {
            instructionsView.setVisibility(View.GONE);
            starDetailsView.setVisibility(View.VISIBLE);
            findViewById(R.id.move_btn).setEnabled(true);

            Vector2 destPoint = mStarfield.getSectorOffset(destStar.getSectorX(), destStar.getSectorY());
            destPoint.add(destStar.getOffsetX(), Sector.SECTOR_SIZE - destStar.getOffsetY());
            fleetIndicatorEntity.setPoints(srcPoint, destPoint);

            float distanceInParsecs = Sector.distanceInParsecs(srcStar, destStar);
            ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, fleet.getDesignID());

            String leftDetails = String.format(Locale.ENGLISH,
                    "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
                    destStar.getName(), distanceInParsecs);
            starDetailsLeft.setText(Html.fromHtml(leftDetails));

            float timeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
            int hrs = (int) Math.floor(timeInHours);
            int mins = (int) Math.floor((timeInHours - hrs) * 60.0f);

            estimatedCost = design.getFuelCost(distanceInParsecs, fleet.getNumShips());
            String cash = Cash.format(estimatedCost);

            String fontOpen = "";
            String fontClose = "";
            if (estimatedCost > EmpireManager.i.getEmpire().getCash()) {
                fontOpen = "<font color=\"#ff0000\">";
                fontClose = "</font>";
            }

            String rightDetails = String.format(Locale.ENGLISH,
                    "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Cost:</b> %s%s",
                    hrs, mins, fontOpen, cash, fontClose);
            starDetailsRight.setText(Html.fromHtml(rightDetails));

            // if it's the marker star, make sure it's not too close to existing stars
            if (destStar == markerStar) {
                EmptySpaceMoverShipEffect effect = fleet.getDesign().getEffect(EmptySpaceMoverShipEffect.class);
                float minDistance = effect.getMinStarDistance();
                if (minDistance > 0) {
                    float distanceToClosestStar = findClosestStar(markerStar);
                    if (distanceToClosestStar < minDistance) {
                        if (tooCloseIndicatorEntity == null) {
                            tooCloseIndicatorEntity = new RadiusIndicatorEntity(this);
                            tooCloseIndicatorEntity.setScale(minDistance * Sector.PIXELS_PER_PARSEC * 2.0f);
                        }
                        if (!markerStar.getAttachedEntities().contains(tooCloseIndicatorEntity)) {
                            markerStar.getAttachedEntities().add(tooCloseIndicatorEntity);
                            mStarfield.queueRefreshScene();
                        }
                    }
                }
            }
        }
    }

    /** Searches for, and returns the distance to, the clostest star to the given star. */
    private float findClosestStar(Star toStar) {
        float minDistance = -1.0f;
        for (Star star : SectorManager.i.getAllVisibleStars()) {
            if (star == toStar || star.getKey().equals(toStar.getKey())) {
                continue;
            }
            float distance = Sector.distanceInParsecs(star, toStar);
            if (distance < minDistance || minDistance < 0.0f) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private void onMoveClick() {
        if (destStar == null) {
            return;
        }

        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        moveBtn.setEnabled(false);

        EmpireManager.i.getEmpire().addCash(-estimatedCost);

        new BackgroundRunner<Boolean>() {
            private String mErrorMessage;

            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           fleet.getStarKey(),
                                           fleet.getKey());
                Messages.FleetOrder.Builder builder = Messages.FleetOrder.newBuilder()
                        .setOrder(Messages.FleetOrder.FLEET_ORDER.MOVE);
                if (markerStar != null) {
                    builder.setSectorX(markerStar.getSectorX());
                    builder.setSectorY(markerStar.getSectorY());
                    builder.setOffsetX(markerStar.getOffsetX());
                    builder.setOffsetY(markerStar.getOffsetY());
                } else {
                    builder.setStarKey(destStar.getKey());
                }
                try {
                    return ApiClient.postProtoBuf(url, builder.build());
                } catch (ApiException e) {
                    mErrorMessage = e.getServerErrorMessage();
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    StyledDialog dialog = new StyledDialog.Builder(FleetMoveActivity.this)
                                            .setTitle("Could not move fleet")
                                            .setMessage(mErrorMessage == null ? "Unable to move fleet, reason unknown." : mErrorMessage)
                                            .setPositiveButton("OK", null)
                                            .create();
                    dialog.show();
                    moveBtn.setEnabled(true);
                } else {
                    // the star this fleet is attached to needs to be refreshed...
                    StarManager.i.refreshStar(Integer.parseInt(fleet.getStarKey()));

                    // the empire needs to be updated, too, since we'll have subtracted
                    // the cost of this move from your cash
                    EmpireManager.i.refreshEmpire(fleet.getEmpireID());

                    finish();
                }
            }
        }.execute();
    }

    /** This entity is used to indicator where the fleet is going to go. */
    public class FleetIndicatorEntity extends Entity {
        private StarfieldSceneManager mStarfield;
        private Vector2 mSrcPoint;
        private Vector2 mDestPoint;
        private Fleet mFleet;
        private Sprite mFleetSprite;
        private float mFractionComplete;

        public FleetIndicatorEntity(StarfieldSceneManager starfield, Vector2 srcPoint, Fleet fleet,
                           VertexBufferObjectManager vertexBufferObjectManager) {
            super(0.0f, 0.0f, 1.0f, 1.0f);
            mStarfield = starfield;
            mSrcPoint = srcPoint;
            mFleet = fleet;

            // work out how far along the fleet has moved so we can draw the icon at the correct
            // spot. Also, we'll draw the name of the empire, number of ships etc.
            ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mFleet.getDesignID());

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

            mFleetSprite = new Sprite(0.0f, 0.0f, spriteWidth, spriteHeight, textureRegion, vertexBufferObjectManager);
            attachChild(mFleetSprite);

            registerUpdateHandler(mUpdateHandler);
        }

        public void setPoints(Vector2 srcPoint, Vector2 destPoint) {
            mSrcPoint = srcPoint;
            mDestPoint = destPoint;
            setup();
        }

        public void setup() {
            if (mDestPoint == null) {
                mFractionComplete = 0.0f;
                mFleetSprite.setRotation(0.0f);
            } else {
                mFractionComplete = 0.5f;

                Vector2 up = Vector2.pool.borrow().reset(1.0f, 0.0f);
                Vector2 direction = Vector2.pool.borrow().reset(mDestPoint);
                direction.subtract(mSrcPoint);
                direction.normalize();
                float angle = Vector2.angleBetweenCcw(up, direction);
                Vector2.pool.release(direction); direction = null;

                mFleetSprite.setRotation((float)(angle * 180.0f / Math.PI));
            }

            Vector2 location = getLocation((float) mFractionComplete);
            setPosition((float) location.x, (float) location.y);
            Vector2.pool.release(location);
        }

        private Vector2 getLocation(float fractionComplete) {
            if (mDestPoint == null) {
                return Vector2.pool.borrow().reset(mSrcPoint);
            }
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

        private IUpdateHandler mUpdateHandler = new IUpdateHandler() {
            @Override
            public void onUpdate(float dt) {
                mFractionComplete += dt / 2.0f;
                while (mFractionComplete > 1.0f) {
                    mFractionComplete -= 1.0f;
                }

                Vector2 location = getLocation((float) mFractionComplete);
                setPosition((float) location.x, (float) location.y);
                Vector2.pool.release(location);
            }

            @Override
            public void reset() {
            }
        };
    }

}
