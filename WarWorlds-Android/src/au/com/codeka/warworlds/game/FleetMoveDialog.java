package au.com.codeka.warworlds.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import warworlds.Warworlds.FleetOrder;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import au.com.codeka.Point2D;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.starfield.StarfieldSurfaceView;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

public class FleetMoveDialog extends Dialog implements DialogManager.DialogConfigurable {
    private Logger log = LoggerFactory.getLogger(FleetMoveDialog.class);
    private Activity mActivity;
    private Fleet mFleet;
    private StarfieldSurfaceView mStarfield;
    private SourceStarOverlay mSourceStarOverlay;
    private DestinationStarOverlay mDestinationStarOverlay;

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

        moveBtn.setEnabled(false); // disabled until you select a star

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

        mStarfield.addStarSelectedListener(new StarfieldSurfaceView.OnStarSelectedListener() {
            @Override
            public void onStarSelected(Star star) {
                mStarfield.removeOverlay(mDestinationStarOverlay);
                mStarfield.addOverlay(mDestinationStarOverlay, star);
                mSourceStarOverlay.calcTriangle();

                moveBtn.setEnabled(true);
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
                        FleetOrder fleetOrder = warworlds.Warworlds.FleetOrder.newBuilder()
                                       .setOrder(warworlds.Warworlds.FleetOrder.FLEET_ORDER.MOVE)
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
                        // the star this fleet is attached to needs to be refreshed...
                        StarManager.getInstance().refreshStar(mFleet.getStarKey());
                        moveBtn.setEnabled(true);
                        cancelBtn.setEnabled(true);
                        if (success) {
                            dismiss();
                        }
                    }

                }.execute();
            }
        });
    }

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        mFleet = (Fleet) bundle.getParcelable("au.com.codeka.warworlds.Fleet");

        StarManager.getInstance().requestStar(mFleet.getStarKey(), false,
                                              new StarManager.StarFetchedHandler() {
            @Override
            public void onStarFetched(Star s) {
                long sectorX = s.getSectorX();
                long sectorY = s.getSectorY();
                int offsetX = s.getOffsetX();
                int offsetY = s.getOffsetY();
                offsetX = offsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
                offsetY = offsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

                mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY);
                mStarfield.addOverlay(mSourceStarOverlay, s);
            }
        });
    }

    /**
     * This overlay is drawn over the "source" star so that we can see where the fleet is moving
     * from.
     */
    private class SourceStarOverlay extends StarfieldSurfaceView.StarAttachedOverlay {
        private RotatingCircle mSourceCircle;

        private Paint mTrianglePaint;
        private Path mTrianglePath;

        public SourceStarOverlay() {
            Paint p = new Paint();
            p.setARGB(255, 0, 255, 0);
            mSourceCircle = new RotatingCircle(p);

            mTrianglePaint = new Paint();
            mTrianglePaint.setARGB(255, 0, 255, 0);
            mTrianglePaint.setAntiAlias(true);
            mTrianglePaint.setStyle(Style.FILL);
            mTrianglePath = new Path();
        }

        @Override
        public void setCentre(double x, double y) {
            super.setCentre(x, y);

            mSourceCircle.setCentre(x, y);
            mSourceCircle.setRadius(30.0);

            calcTriangle();
        }

        /**
         * Calculates the triangle that we draw over the star to show the direction our
         * fleet will move in.
         */
        public void calcTriangle() {
            Point2D top;
            if (mDestinationStarOverlay.isVisible()) {
                top = new Point2D(mDestinationStarOverlay.getCentre());
                top.subtract(mCentre);
                log.debug(String.format("Top: (%.2f, %.2f)", top.x, top.y));
                top.normalize();
            } else {
                top = new Point2D(0, -1);
                log.debug("No destination is selected...");
            }

            Point2D back = new Point2D(top);
            back.scale(-10.0f);

            Point2D right = new Point2D(top);
            right.rotate((float)(Math.PI / 2.0));

            Point2D left = new Point2D(top);
            left.rotate((float)(-Math.PI / 2.0));

            top.scale(20.0f);
            top.add(mCentre);

            right.scale(10.0f);
            right.add(back);
            right.add(mCentre);

            left.scale(10.0f);
            left.add(back);
            left.add(mCentre);

            mTrianglePath.reset();
            mTrianglePath.moveTo(top.x, top.y);
            mTrianglePath.lineTo(right.x, right.y);
            mTrianglePath.lineTo(left.x, left.y);
            mTrianglePath.lineTo(top.x, top.y);
        }

        @Override
        public void draw(Canvas canvas) {
            mSourceCircle.draw(canvas);
            canvas.drawPath(mTrianglePath, mTrianglePaint);
        }
    }

    /**
     * This overlay is drawn once you've selected a star as the "destination" for your move.
     */
    private class DestinationStarOverlay extends StarfieldSurfaceView.StarAttachedOverlay {
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

            mSourceStarOverlay.calcTriangle();
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
