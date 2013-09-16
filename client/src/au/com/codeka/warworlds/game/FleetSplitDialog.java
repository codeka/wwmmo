package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.StarManager;

public class FleetSplitDialog extends DialogFragment {
    private Fleet mFleet;
    private View mView;

    public FleetSplitDialog() {
    }

    public void setFleet(Fleet fleet) {
        mFleet = fleet;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mFleet == null) {
            return null;
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fleet_split_dlg, null);

        final SeekBar splitRatio = (SeekBar) mView.findViewById(R.id.split_ratio);
        final TextView splitLeft = (TextView) mView.findViewById(R.id.split_left);
        final TextView splitRight = (TextView) mView.findViewById(R.id.split_right);

        View fleetView = mView.findViewById(R.id.fleet);
        FleetList.populateFleetRow(getActivity(), null, fleetView, mFleet);

        int numShips = (int) Math.ceil(mFleet.getNumShips());
        if (numShips >= 2) {
            splitRatio.setMax(numShips - 2);
        } else {
            splitRatio.setMax(numShips - 1);
        }
        splitRatio.setProgress(numShips / 2 - 1);
        splitLeft.setText(Integer.toString(numShips / 2));
        splitRight.setText(Integer.toString(numShips - (numShips / 2)));

        splitRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                splitLeft.setText(Integer.toString(progress + 1));
                splitRight.setText(Integer.toString(seekBar.getMax() - progress + 1));
            }
        });

        splitLeft.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    int val = Integer.parseInt(splitLeft.getText().toString());
                    if (val < 0) {
                        val = 0;
                    }
                    if (val > splitRatio.getMax()) {
                        val = splitRatio.getMax();
                    }
                    splitRatio.setProgress(val - 1);
                }
            }
        });

        splitRight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    int val = Integer.parseInt(splitLeft.getText().toString());
                    if (val < 0) {
                        val = 0;
                    }
                    if (val > splitRatio.getMax()) {
                        val = splitRatio.getMax();
                    }
                    splitRatio.setProgress(splitRatio.getMax() - val + 1);
                }
            }
        });

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);
        b.setTitle("Split Fleet");

        b.setPositiveButton("Split", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onSplitClick();
            }
        });

        b.setNegativeButton("Cancel", null);

        return b.create();
    }

    private void onSplitClick() {
        ((StyledDialog) getDialog()).setCloseable(false);

        final TextView splitLeft = (TextView) mView.findViewById(R.id.split_left);
        final TextView splitRight = (TextView) mView.findViewById(R.id.split_right);
        dismiss();

        new BackgroundRunner<Boolean>() {
            @Override
            protected Boolean doInBackground() {
                String url = String.format("stars/%s/fleets/%s/orders",
                                           mFleet.getStarKey(),
                                           mFleet.getKey());
                Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
                               .setOrder(Messages.FleetOrder.FLEET_ORDER.SPLIT)
                               .setSplitLeft(Integer.parseInt(splitLeft.getText().toString()))
                               .setSplitRight(Integer.parseInt(splitRight.getText().toString()))
                               .build();

                try {
                    return ApiClient.postProtoBuf(url, fleetOrder);
                } catch (ApiException e) {
                    // TODO: do something..?
                    return false;
                }
            }

            @Override
            protected void onComplete(Boolean success) {
                // the star this fleet is attached to needs to be refreshed...
                StarManager.getInstance().refreshStar(mFleet.getStarKey());
            }
        }.execute();
    }
}
