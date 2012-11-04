package au.com.codeka.warworlds.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.FloatMath;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.Point2D;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.starfield.StarfieldSurfaceView;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.SectorManager;
import au.com.codeka.warworlds.model.ShipDesign;
import au.com.codeka.warworlds.model.ShipDesignManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class FleetMoveDialog extends Dialog implements DialogManager.DialogConfigurable {
    private Activity mActivity;
    private Fleet mFleet;
    private StarfieldSurfaceView mStarfield;
    private SourceStarOverlay mSourceStarOverlay;
    private DestinationStarOverlay mDestinationStarOverlay;
    private Star mSourceStar;

    public static final int ID = 1008;

    public FleetMoveDialog(Activity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fleet_move_dlg);

        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        final Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
        final View starDetailsView = findViewById(R.id.star_details);
        final View instructionsView = findViewById(R.id.instructions);
        final TextView starDetailsLeft = (TextView) findViewById(R.id.star_details_left);
        final TextView starDetailsRight = (TextView) findViewById(R.id.star_details_right);

        moveBtn.setEnabled(false); // disabled until you select a star
        starDetailsView.setVisibility(View.GONE);

        // we want the window to be slightly taller than a square
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        int displayWidth = display.getWidth();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = displayWidth;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        mSourceStarOverlay = new SourceStarOverlay();
        mDestinationStarOverlay = new DestinationStarOverlay();

        mStarfield = (StarfieldSurfaceView) findViewById(R.id.starfield);
        mStarfield.setZOrderOnTop(true);

        mStarfield.addSelectionChangedListener(new StarfieldSurfaceView.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
                mStarfield.removeOverlay(mDestinationStarOverlay);
                mStarfield.addOverlay(mDestinationStarOverlay, star);
                mSourceStarOverlay.reset();

                if (mSourceStar != null) {
                    float distanceInParsecs = SectorManager.getInstance()
                                              .distanceInParsecs(mSourceStar, star);
                    ShipDesign design = ShipDesignManager.getInstance().getDesign(mFleet.getDesignID());

                    String leftDetails = String.format("<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
                            star.getName(), distanceInParsecs);
                    starDetailsLeft.setText(Html.fromHtml(leftDetails));

                    float timeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
                    int hrs = (int) FloatMath.floor(timeInHours);
                    int mins = (int) FloatMath.floor((timeInHours - hrs) * 60.0f);

                    float cost = design.getFuelCost(distanceInParsecs, mFleet.getNumShips());
                    String cash = Cash.format(cost);

                    String fontOpen = "";
                    String fontClose = "";
                    if (cost > EmpireManager.getInstance().getEmpire().getCash()) {
                        fontOpen = "<font color=\"#ff0000\">";
                        fontClose = "</font>";
                    }

                    String rightDetails = String.format("<b>ETA:</b> %d hrs, %d mins<br />%s<b>Cost:</b> %s%s",
                            hrs, mins, fontOpen, cash, fontClose);
                    starDetailsRight.setText(Html.fromHtml(rightDetails));
                }

                moveBtn.setEnabled(true);
                starDetailsView.setVisibility(View.VISIBLE);
                instructionsView.setVisibility(View.GONE);
            }

            @Override
            public void onFleetSelected(Fleet fleet) {
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Star selectedStar = mStarfield.getSelectedStar();
                if (selectedStar == null) {
                    return;
                }

                moveBtn.setEnabled(false);
                cancelBtn.setEnabled(false);

                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... params) {
                        String url = String.format("stars/%s/fleets/%s/orders",
                                                   mFleet.getStarKey(),
                                                   mFleet.getKey());
                        Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                                       .setOrder(Messages.FleetOrder.FLEET_ORDER.MOVE)
                                       .setStarKey(selectedStar.getKey())
                                       .build();
                        try {
                            return ApiClient.postProtoBuf(url, fleetOrder);
                        } catch (ApiException e) {
                            // TODO: do something..?
                            return false;
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (!success) {
                            AlertDialog dialog = new AlertDialog.Builder(mActivity)
                                                    .setMessage("Could not move the fleet: do you have enough cash?")
                                                    .create();
                            dialog.show();
                        } else {
                            // the star this fleet is attached to needs to be refreshed...
                            StarManager.getInstance().refreshStar(mFleet.getStarKey());
                            moveBtn.setEnabled(true);
                            cancelBtn.setEnabled(true);
                            if (success) {
                                dismiss();
                            }

                            // the empire needs to be updated, too, since we'll have subtracted
                            // the cost of this move from your cash
                            EmpireManager.getInstance().refreshEmpire(mFleet.getEmpireKey());
                        }
                    }

                }.execute();
            }
        });
    }

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        mFleet = (Fleet) bundle.getParcelable("au.com.codeka.warworlds.Fleet");

        final Button moveBtn = (Button) findViewById(R.id.move_btn);
        final View starDetailsView = findViewById(R.id.star_details);
        final View instructionsView = findViewById(R.id.instructions);
        moveBtn.setEnabled(false);
        starDetailsView.setVisibility(View.GONE);
        instructionsView.setVisibility(View.VISIBLE);

        mStarfield.removeOverlay(mDestinationStarOverlay);
        mStarfield.deselectStar();

        StarManager.getInstance().requestStar(mFleet.getStarKey(), false,
                                              new StarManager.StarFetchedHandler() {
            @Override
            public void onStarFetched(Star s) {
                mSourceStar = s;

                long sectorX = s.getSectorX();
                long sectorY = s.getSectorY();
                int offsetX = s.getOffsetX();
                int offsetY = s.getOffsetY();
                offsetX = offsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
                offsetY = offsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

                mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
                mStarfield.addOverlay(mSourceStarOverlay, s);
            }
        });
    }

    /**
     * This overlay is drawn over the "source" star so that we can see where the fleet is moving
     * from.
     */
    private class SourceStarOverlay extends StarfieldSurfaceView.VisibleEntityAttachedOverlay {
        private RotatingCircle mSourceCircle;

        private Paint mShipPaint;
        private Sprite mSprite;
        private Matrix mMatrix;

        public SourceStarOverlay() {
            Paint p = new Paint();
            p.setARGB(255, 0, 255, 0);
            mSourceCircle = new RotatingCircle(p);

            mShipPaint = new Paint();
            mShipPaint.setARGB(255, 255, 255, 255);

            mMatrix = new Matrix();
        }

        @Override
        public void setCentre(double x, double y) {
            super.setCentre(x, y);

            mSourceCircle.setCentre(x, y);
            mSourceCircle.setRadius(30.0);

            reset();
        }

        /**
         * Calculates the triangle that we draw over the star to show the direction our
         * fleet will move in.
         */
        public void reset() {
            ShipDesignManager designManager = ShipDesignManager.getInstance();
            mSprite = designManager.getDesign(mFleet.getDesignID()).getSprite();

            float pixelScale = getPixelScale();

            Point2D direction;
            if (mDestinationStarOverlay.isVisible()) {
                direction = new Point2D(mDestinationStarOverlay.getCentre());
                direction.subtract(mCentre);
                direction.normalize();
            } else {
                direction = new Point2D(0, -1);
            }

            Point2D up = mSprite.getUp();
            float angle = Point2D.angleBetween(up, direction);

            // scale zoom and rotate the bitmap all with one matrix
            mMatrix.reset();
            mMatrix.postTranslate(-(mSprite.getWidth() / 2.0f),
                                  -(mSprite.getHeight() / 2.0f));
            mMatrix.postScale(20.0f * pixelScale / mSprite.getWidth(),
                              20.0f * pixelScale / mSprite.getHeight());
            mMatrix.postRotate((float) (angle * 180.0 / Math.PI));
            mMatrix.postTranslate(mCentre.x, mCentre.y);
        }

        @Override
        public void draw(Canvas canvas) {
            mSourceCircle.draw(canvas);

            canvas.save();
            canvas.setMatrix(mMatrix);
            mSprite.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * This overlay is drawn once you've selected a star as the "destination" for your move.
     */
    private class DestinationStarOverlay extends StarfieldSurfaceView.VisibleEntityAttachedOverlay {
        private Paint mLinePaint;
        private Paint mCirclePaint;

        public DestinationStarOverlay() {
            mLinePaint = new Paint();
            mLinePaint.setARGB(255, 0, 255, 0);
            mLinePaint.setStyle(Style.STROKE);
            mLinePaint.setAntiAlias(true);

            mCirclePaint = new Paint();
            mCirclePaint.setARGB(255, 0, 255, 0);
            mCirclePaint.setStyle(Style.FILL);
            mCirclePaint.setAntiAlias(true);
        }

        @Override
        public void setCentre(double x, double y) {
            super.setCentre(x, y);

            mSourceStarOverlay.reset();
        }

        @Override
        public void draw(Canvas canvas) {
            Point2D start = mSourceStarOverlay.getCentre();
            canvas.drawLine(start.x, start.y,
                            mCentre.x, mCentre.y,
                            mLinePaint);

            canvas.drawCircle(mCentre.x, mCentre.y, 10.0f, mCirclePaint);
        }

    }
}
