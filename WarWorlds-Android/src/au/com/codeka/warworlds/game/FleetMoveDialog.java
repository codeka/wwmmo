package au.com.codeka.warworlds.game;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import au.com.codeka.Cash;
import au.com.codeka.Point2D;
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
import au.com.codeka.warworlds.model.StarSummary;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class FleetMoveDialog extends DialogFragment {
    private Fleet mFleet;
    private StarfieldSurfaceView mStarfield;
    private SourceStarOverlay mSourceStarOverlay;
    private DestinationStarOverlay mDestinationStarOverlay;
    private StarSummary mSourceStarSummary;
    private View mView;

    public FleetMoveDialog() {
    }

    public void setFleet(Fleet fleet) {
        mFleet = fleet;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fleet_move_dlg, null);

        final View starDetailsView = mView.findViewById(R.id.star_details);
        final View instructionsView = mView.findViewById(R.id.instructions);
        final TextView starDetailsLeft = (TextView) mView.findViewById(R.id.star_details_left);
        final TextView starDetailsRight = (TextView) mView.findViewById(R.id.star_details_right);

        starDetailsView.setVisibility(View.GONE);
        instructionsView.setVisibility(View.VISIBLE);

        mSourceStarOverlay = new SourceStarOverlay();
        mDestinationStarOverlay = new DestinationStarOverlay();

        mStarfield = (StarfieldSurfaceView) mView.findViewById(R.id.starfield);
        mStarfield.setZOrderOnTop(true);

        mStarfield.addSelectionChangedListener(new StarfieldSurfaceView.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
                mStarfield.removeOverlay(mDestinationStarOverlay);
                mStarfield.addOverlay(mDestinationStarOverlay, star);
                mSourceStarOverlay.reset();

                if (mSourceStarSummary != null) {
                    float distanceInParsecs = SectorManager.getInstance()
                                              .distanceInParsecs(mSourceStarSummary, star);
                    ShipDesign design = ShipDesignManager.getInstance().getDesign(mFleet.getDesignID());

                    String leftDetails = String.format(Locale.ENGLISH,
                            "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
                            star.getName(), distanceInParsecs);
                    starDetailsLeft.setText(Html.fromHtml(leftDetails));

                    float timeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
                    int hrs = (int) Math.floor(timeInHours);
                    int mins = (int) Math.floor((timeInHours - hrs) * 60.0f);

                    float cost = design.getFuelCost(distanceInParsecs, mFleet.getNumShips());
                    String cash = Cash.format(cost);

                    String fontOpen = "";
                    String fontClose = "";
                    if (cost > EmpireManager.getInstance().getEmpire().getCash()) {
                        fontOpen = "<font color=\"#ff0000\">";
                        fontClose = "</font>";
                    }

                    String rightDetails = String.format(Locale.ENGLISH,
                            "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Cost:</b> %s%s",
                            hrs, mins, fontOpen, cash, fontClose);
                    starDetailsRight.setText(Html.fromHtml(rightDetails));
                }

                ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(true);
                starDetailsView.setVisibility(View.VISIBLE);
                instructionsView.setVisibility(View.GONE);
            }

            @Override
            public void onFleetSelected(Fleet fleet) {
            }
        });

        StarManager.getInstance().requestStarSummary(getActivity(), mFleet.getStarKey(),
                new StarManager.StarSummaryFetchedHandler() {
            @Override
            public void onStarSummaryFetched(StarSummary s) {
                mSourceStarSummary = s;

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

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setView(mView);

        b.setPositiveButton("Move", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onMoveClick();
            }
            
        });

        b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        final AlertDialog dialog = b.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });

        return dialog;
    }

    private void onMoveClick() {
        final Star selectedStar = mStarfield.getSelectedStar();
        if (selectedStar == null) {
            return;
        }

        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

        final Activity activity = getActivity();

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
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    AlertDialog dialog = new AlertDialog.Builder(activity)
                                            .setMessage("Could not move the fleet: do you have enough cash?")
                                            .create();
                    dialog.show();
                } else {
                    // the star this fleet is attached to needs to be refreshed...
                    StarManager.getInstance().refreshStar(activity, mFleet.getStarKey());

                    // the empire needs to be updated, too, since we'll have subtracted
                    // the cost of this move from your cash
                    EmpireManager.getInstance().refreshEmpire(mFleet.getEmpireKey());
                }
            }

        }.execute();
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
