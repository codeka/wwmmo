package au.com.codeka.warworlds.game;

import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Cash;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.design.DesignKind;
import au.com.codeka.common.design.ShipDesign;
import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.FleetOrder;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.starfield.StarfieldSurfaceView;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.StarManager;

public class FleetMoveDialog extends DialogFragment {
    private Fleet mFleet;
    private StarfieldSurfaceView mStarfield;
    private SourceStarOverlay mSourceStarOverlay;
    private DestinationStarOverlay mDestinationStarOverlay;
    private Star mSourceStarSummary;
    private View mView;
    private float mEstimatedCost;

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

        mStarfield.addSelectionChangedListener(new StarfieldSurfaceView.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
                mStarfield.removeOverlay(mDestinationStarOverlay);
                mStarfield.addOverlay(mDestinationStarOverlay, star);
                mSourceStarOverlay.reset();

                if (mSourceStarSummary != null) {
                    float distanceInParsecs = Model.distanceInParsecs(mSourceStarSummary, star);
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mFleet.design_id);

                    String leftDetails = String.format(Locale.ENGLISH,
                            "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
                            star.name, distanceInParsecs);
                    starDetailsLeft.setText(Html.fromHtml(leftDetails));

                    float timeInHours = distanceInParsecs / design.getSpeedInParsecPerHour();
                    int hrs = (int) Math.floor(timeInHours);
                    int mins = (int) Math.floor((timeInHours - hrs) * 60.0f);

                    mEstimatedCost = design.getFuelCost(distanceInParsecs, mFleet.num_ships);
                    String cash = Cash.format(mEstimatedCost);

                    String fontOpen = "";
                    String fontClose = "";
                    if (mEstimatedCost > EmpireManager.i.getEmpire().cash) {
                        fontOpen = "<font color=\"#ff0000\">";
                        fontClose = "</font>";
                    }

                    String rightDetails = String.format(Locale.ENGLISH,
                            "<b>ETA:</b> %d hrs, %d mins<br />%s<b>Cost:</b> %s%s",
                            hrs, mins, fontOpen, cash, fontClose);
                    starDetailsRight.setText(Html.fromHtml(rightDetails));
                }

                StyledDialog dialog = ((StyledDialog) getDialog());
                if (dialog == null) {
                    // can happen if they dismiss the dialog (I think?)
                    return;
                }
                dialog.getPositiveButton().setEnabled(true);
                starDetailsView.setVisibility(View.VISIBLE);
                instructionsView.setVisibility(View.GONE);
            }

            @Override
            public void onFleetSelected(Fleet fleet) {
            }
        });

        StarManager.i.requestStarSummary(mFleet.star_key,
                new StarManager.StarSummaryFetchedHandler() {
            @Override
            public void onStarSummaryFetched(Star s) {
                mSourceStarSummary = s;

                long sectorX = s.sector_x;
                long sectorY = s.sector_y;
                int offsetX = s.offset_x;
                int offsetY = s.offset_y;
                offsetX = offsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
                offsetY = offsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

                mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
                mStarfield.addOverlay(mSourceStarOverlay, s);
            }
        });

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);

        b.setPositiveButton("Move", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onMoveClick();
            }
        });
        b.setNegativeButton("Cancel", null);

        final StyledDialog dialog = b.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getPositiveButton().setEnabled(false);
            }
        });

        return dialog;
    }

    private void onMoveClick() {
        final Star selectedStar = mStarfield.getSelectedStar();
        if (selectedStar == null) {
            return;
        }

        final StyledDialog dialog = (StyledDialog) getDialog();
        dialog.getPositiveButton().setEnabled(false);
        dialog.getNegativeButton().setEnabled(false);

        final Activity activity = getActivity();
        dismiss();

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           mFleet.star_key,
                                           mFleet.key);
                FleetOrder fleetOrder = new FleetOrder.Builder()
                               .order(FleetOrder.FLEET_ORDER.MOVE)
                               .star_key(selectedStar.key)
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
                    StyledDialog dialog = new StyledDialog.Builder(activity)
                                            .setMessage("Could not move the fleet: do you have enough cash?")
                                            .create();
                    dialog.show();
                    dialog.getPositiveButton().setEnabled(true);
                    dialog.getNegativeButton().setEnabled(true);
                } else {
                    // the star this fleet is attached to needs to be refreshed...
                    StarManager.i.refreshStar(mFleet.star_key);

                    // the empire needs to be updated, too, since we'll have subtracted
                    // the cost of this move from your cash
                    EmpireManager.i.refreshEmpire(mFleet.empire_key);
                }
            }
        }.execute();
    }

    /**
     * This overlay is drawn over the "source" star so that we can see where the fleet is moving
     * from.
     */
    private class SourceStarOverlay extends StarfieldSurfaceView.VisibleEntityAttachedOverlay {
        private Paint mShipPaint;
        private Sprite mSprite;
        private Matrix mMatrix;

        public SourceStarOverlay() {
            Paint p = new Paint();
            p.setARGB(255, 0, 255, 0);

            mShipPaint = new Paint();
            mShipPaint.setARGB(255, 255, 255, 255);

            mMatrix = new Matrix();
        }

        @Override
        public void setCentre(double x, double y) {
            super.setCentre(x, y);
            reset();
        }

        /**
         * Calculates the triangle that we draw over the star to show the direction our
         * fleet will move in.
         */
        public void reset() {
            mSprite = SpriteManager.i.getSprite(
                    DesignManager.i.getDesign(DesignKind.SHIP, mFleet.design_id).getSpriteName());

            float pixelScale = getPixelScale();

            Vector2 direction = Vector2.pool.borrow();
            if (mDestinationStarOverlay.isVisible()) {
                direction.reset(mDestinationStarOverlay.getCentre());
                direction.subtract(mCentre);
                direction.normalize();
            } else {
                direction.reset(0, -1);
            }

            float angle = Vector2.angleBetween(mSprite.getUp(), direction);
            Vector2.pool.release(direction); direction = null;

            // scale zoom and rotate the bitmap all with one matrix
            mMatrix.reset();
            mMatrix.postTranslate(-(mSprite.getWidth() / 2.0f),
                                  -(mSprite.getHeight() / 2.0f));
            mMatrix.postScale(20.0f * pixelScale / mSprite.getWidth(),
                              20.0f * pixelScale / mSprite.getHeight());
            mMatrix.postRotate((float) (angle * 180.0 / Math.PI));
            mMatrix.postTranslate((float) mCentre.x, (float) mCentre.y);
        }

        @Override
        public void draw(Canvas canvas) {
            if (mSprite != null) {
                canvas.save();
                canvas.concat(mMatrix);
                mSprite.draw(canvas);
                canvas.restore();
            }
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
            Vector2 start = mSourceStarOverlay.getCentre();
            canvas.drawLine((float) start.x, (float) start.y,
                            (float) mCentre.x, (float) mCentre.y,
                            mLinePaint);

            canvas.drawCircle((float) mCentre.x, (float) mCentre.y, 10.0f, mCirclePaint);
        }

    }
}
