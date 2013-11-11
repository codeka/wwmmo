package au.com.codeka.warworlds.game;

import java.util.Locale;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
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

import com.google.protobuf.InvalidProtocolBufferException;

/** This activity is used to select a location for moves. It's a bit annoying that we have to do it like this... */
public class FleetMoveActivity extends BaseStarfieldActivity {
    private static final Logger log = LoggerFactory.getLogger(FleetMoveActivity.class);
    private Star mSrcStar;
    private Star mDestStar;
    private Fleet mFleet;
    private float mEstimatedCost;
    private FleetIndicatorEntity mFleetIndicatorEntity;

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
            mFleet = new Fleet();
            mFleet.fromProtocolBuffer(fleet_pb);
        } catch (InvalidProtocolBufferException e) {
        }

        // we can get an instance of the star from the sector manager
        mSrcStar = SectorManager.getInstance().findStar(mFleet.getStarKey());

        super.onCreate(savedInstanceState);

        mStarfield.setSceneCreatedHandler(new SectorSceneManager.SceneCreatedHandler() {
            @Override
            public void onSceneCreated(Scene scene) {
                Vector2 srcPoint = mStarfield.getSectorOffset(mSrcStar.getSectorX(), mSrcStar.getSectorY());
                srcPoint.add(mSrcStar.getOffsetX(), mSrcStar.getOffsetY());
                mFleetIndicatorEntity = new FleetIndicatorEntity(mStarfield, srcPoint, mFleet, getVertexBufferObjectManager());
                scene.attachChild(mFleetIndicatorEntity);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshSelection();
                    }
                });
            }
        });

        mStarfield.scrollTo(mSrcStar.getSectorX(), mSrcStar.getSectorY(), mSrcStar.getOffsetX(), mSrcStar.getOffsetY());

        mStarfield.addSelectionChangedListener(new StarfieldSceneManager.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
                if (star == null) {
                    mDestStar = null;
                    refreshSelection();
                    return;
                }

                if (mDestStar == null || !mDestStar.getKey().equals(star.getKey())) {
                    if (star.getKey().equals(mSrcStar.getKey())) {
                        // if src & dest are the same, just forget about it
                        mDestStar = null;
                        refreshSelection();
                        return;
                    }

                    mDestStar = star;
                    refreshSelection();
                    return;
                }
            }

            @Override
            public void onFleetSelected(Fleet fleet) {
                // you can't select fleets
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
                if (mDestStar != null) {
                    onMoveClick();
                }
                finish();
            }
        });
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fleet_move;
    }

    private void refreshSelection() {
        final View starDetailsView = findViewById(R.id.star_details);
        final View instructionsView = findViewById(R.id.instructions);
        final TextView starDetailsLeft = (TextView) findViewById(R.id.star_details_left);
        final TextView starDetailsRight = (TextView) findViewById(R.id.star_details_right);

        if (mSrcStar == null) {
            return;
        }

        Vector2 srcPoint = mStarfield.getSectorOffset(mSrcStar.getSectorX(), mSrcStar.getSectorY());
        srcPoint.add(mSrcStar.getOffsetX(), mSrcStar.getOffsetY());

        if (mDestStar == null) {
            instructionsView.setVisibility(View.VISIBLE);
            starDetailsView.setVisibility(View.GONE);
            mFleetIndicatorEntity.setPoints(srcPoint, null);

            return;
        } else {
            instructionsView.setVisibility(View.GONE);
            starDetailsView.setVisibility(View.VISIBLE);

            Vector2 destPoint = mStarfield.getSectorOffset(mDestStar.getSectorX(), mDestStar.getSectorY());
            destPoint.add(mDestStar.getOffsetX(), mDestStar.getOffsetY());
            mFleetIndicatorEntity.setPoints(srcPoint, destPoint);

            float distanceInParsecs = Sector.distanceInParsecs(mSrcStar, mDestStar);
            ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mFleet.getDesignID());

            String leftDetails = String.format(Locale.ENGLISH,
                    "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
                    mDestStar.getName(), distanceInParsecs);
            starDetailsLeft.setText(Html.fromHtml(leftDetails));

            float timeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
            int hrs = (int) Math.floor(timeInHours);
            int mins = (int) Math.floor((timeInHours - hrs) * 60.0f);

            mEstimatedCost = design.getFuelCost(distanceInParsecs, mFleet.getNumShips());
            String cash = Cash.format(mEstimatedCost);

            String fontOpen = "";
            String fontClose = "";
            if (mEstimatedCost > EmpireManager.i.getEmpire().getCash()) {
                fontOpen = "<font color=\"#ff0000\">";
                fontClose = "</font>";
            }

            String rightDetails = String.format(Locale.ENGLISH,
                    "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Cost:</b> %s%s",
                    hrs, mins, fontOpen, cash, fontClose);
            starDetailsRight.setText(Html.fromHtml(rightDetails));
        }
    }

    private void onMoveClick() {
        if (mDestStar == null) {
            return;
        }

        EmpireManager.i.getEmpire().addCash(-mEstimatedCost);

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           mFleet.getStarKey(),
                                           mFleet.getKey());
                Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                               .setOrder(Messages.FleetOrder.FLEET_ORDER.MOVE)
                               .setStarKey(mDestStar.getKey())
                               .build();
                try {
                    return ApiClient.postProtoBuf(url, fleetOrder);
                } catch (ApiException e) {
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                if (!success) {
                    StyledDialog dialog = new StyledDialog.Builder(FleetMoveActivity.this)
                                            .setMessage("Could not move the fleet: do you have enough cash?")
                                            .create();
                    dialog.show();
                    dialog.getPositiveButton().setEnabled(true);
                    dialog.getNegativeButton().setEnabled(true);
                } else {
                    // the star this fleet is attached to needs to be refreshed...
                    StarManager.getInstance().refreshStar(mFleet.getStarKey());

                    // the empire needs to be updated, too, since we'll have subtracted
                    // the cost of this move from your cash
                    EmpireManager.i.refreshEmpire(mFleet.getEmpireKey());
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
