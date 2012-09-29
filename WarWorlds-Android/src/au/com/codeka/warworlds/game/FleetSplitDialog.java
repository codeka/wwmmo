package au.com.codeka.warworlds.game;

import warworlds.Warworlds.FleetOrder;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.ctrl.FleetList;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.StarManager;

public class FleetSplitDialog extends Dialog implements DialogManager.DialogConfigurable {
    private Fleet mFleet;
    private Context mContext;

    public static final int ID = 1004;

    public FleetSplitDialog(Activity activity) {
        super(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_fleet_split_dlg);

        final SeekBar splitRatio = (SeekBar) findViewById(R.id.split_ratio);
        final TextView splitLeft = (TextView) findViewById(R.id.split_left);
        final TextView splitRight = (TextView) findViewById(R.id.split_right);
        final Button splitBtn = (Button) findViewById(R.id.split_btn);

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
                        FleetOrder fleetOrder = warworlds.Warworlds.FleetOrder.newBuilder()
                                       .setOrder(warworlds.Warworlds.FleetOrder.FLEET_ORDER.SPLIT)
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
                        StarManager.getInstance().refreshStar(mFleet.getStarKey());
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
    }

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        mFleet = (Fleet) bundle.getParcelable("au.com.codeka.warworlds.Fleet");

        View fleetView = findViewById(R.id.fleet);
        FleetList.populateFleetRow(mContext, null, fleetView, mFleet);

        SeekBar splitRatio = (SeekBar) findViewById(R.id.split_ratio);
        splitRatio.setMax(mFleet.getNumShips());
        splitRatio.setProgress(mFleet.getNumShips() / 2);

        TextView splitLeft = (TextView) findViewById(R.id.split_left);
        splitLeft.setText(Integer.toString(mFleet.getNumShips() / 2));

        TextView splitRight = (TextView) findViewById(R.id.split_right);
        splitRight.setText(Integer.toString(mFleet.getNumShips() - (mFleet.getNumShips() / 2)));
    }
}
