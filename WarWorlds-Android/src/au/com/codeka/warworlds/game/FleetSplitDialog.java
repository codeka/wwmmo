package au.com.codeka.warworlds.game;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class FleetSplitDialog extends DialogFragment {
    private Fleet mFleet;

    public FleetSplitDialog() {
    }

    public void setFleet(Fleet fleet) {
        mFleet = fleet;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = inflater.inflate(R.layout.fleet_split_dlg, container);

        final SeekBar splitRatio = (SeekBar) view.findViewById(R.id.split_ratio);
        final TextView splitLeft = (TextView) view.findViewById(R.id.split_left);
        final TextView splitRight = (TextView) view.findViewById(R.id.split_right);
        final Button splitBtn = (Button) view.findViewById(R.id.split_btn);

        View fleetView = view.findViewById(R.id.fleet);
        FleetList.populateFleetRow(getActivity(), null, fleetView, mFleet);

        splitRatio.setMax(mFleet.getNumShips());
        splitRatio.setProgress(mFleet.getNumShips() / 2);
        splitLeft.setText(Integer.toString(mFleet.getNumShips() / 2));
        splitRight.setText(Integer.toString(mFleet.getNumShips() - (mFleet.getNumShips() / 2)));

        splitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                splitBtn.setEnabled(false);

                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... params) {
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
                    protected void onPostExecute(Boolean success) {
                        // the star this fleet is attached to needs to be refreshed...
                        StarManager.getInstance().refreshStar(
                                getActivity(), mFleet.getStarKey());
                        splitBtn.setEnabled(true);
                        if (success) {
                            dismiss();
                        }
                    }

                }.execute();
            }
        });

        splitRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                splitLeft.setText(Integer.toString(progress));
                splitRight.setText(Integer.toString(seekBar.getMax() - progress));
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
                    splitRatio.setProgress(val);
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
                    splitRatio.setProgress(splitRatio.getMax() - val);
                }
            }
        });

        return view;
    }
}
