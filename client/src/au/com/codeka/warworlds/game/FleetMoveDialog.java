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
import au.com.codeka.Cash;
import au.com.codeka.common.Vector2;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.game.starfield.StarfieldSceneManager;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.Sector;
import au.com.codeka.warworlds.model.Sprite;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.StarSummary;

public class FleetMoveDialog extends DialogFragment {
    private Fleet mFleet;
    private StarfieldSceneManager mStarfield;
 //   private SourceStarOverlay mSourceStarOverlay;
  //  private DestinationStarOverlay mDestinationStarOverlay;
    private StarSummary mSourceStarSummary;
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

    //   mSourceStarOverlay = new SourceStarOverlay();
    //    mDestinationStarOverlay = new DestinationStarOverlay();

     //   mStarfield = (StarfieldSceneManager) mView.findViewById(R.id.starfield);

        mStarfield.addSelectionChangedListener(new StarfieldSceneManager.OnSelectionChangedListener() {
            @Override
            public void onStarSelected(Star star) {
     //           mStarfield.removeOverlay(mDestinationStarOverlay);
      //          mStarfield.addOverlay(mDestinationStarOverlay, star);
       //         mSourceStarOverlay.reset();

                if (mSourceStarSummary != null) {
                    float distanceInParsecs = Sector.distanceInParsecs(mSourceStarSummary, star);
                    ShipDesign design = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, mFleet.getDesignID());

                    String leftDetails = String.format(Locale.ENGLISH,
                            "<b>Star:</b> %s<br /><b>Distance:</b> %.2f pc",
                            star.getName(), distanceInParsecs);
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

        StarManager.getInstance().requestStarSummary(mFleet.getStarKey(),
                new StarManager.StarSummaryFetchedHandler() {
            @Override
            public void onStarSummaryFetched(StarSummary s) {
                mSourceStarSummary = s;

                long sectorX = s.getSectorX();
                long sectorY = s.getSectorY();
                int offsetX = s.getOffsetX();
                int offsetY = s.getOffsetY();
    //            offsetX = offsetX - (int) ((mStarfield.getWidth() / 2) / mStarfield.getPixelScale());
    //            offsetY = offsetY -  (int) ((mStarfield.getHeight() / 2) / mStarfield.getPixelScale());

    //            mStarfield.scrollTo(sectorX, sectorY, offsetX, offsetY, true);
    //            mStarfield.addOverlay(mSourceStarOverlay, s);
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

        EmpireManager.i.getEmpire().addCash(-mEstimatedCost);

        final Activity activity = getActivity();
        dismiss();

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
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
                    StarManager.getInstance().refreshStar(mFleet.getStarKey());

                    // the empire needs to be updated, too, since we'll have subtracted
                    // the cost of this move from your cash
                    EmpireManager.i.refreshEmpire(mFleet.getEmpireKey());
                }
            }
        }.execute();
    }
}
