package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class FocusDialog extends DialogFragment {
    private static Logger log = LoggerFactory.getLogger(FocusDialog.class);
    private Colony mColony;
    private List<SeekBar> mSeekBars;
    private List<TextView> mTextViews;

    public FocusDialog() {
    }

    public void setColony(Colony colony) {
        mColony = colony;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.focus_dlg, null);

        mSeekBars = new ArrayList<SeekBar>();
        mTextViews = new ArrayList<TextView>();
        int[] seekBarIds = {R.id.focus_population,
                            R.id.focus_farming,
                            R.id.focus_mining,
                            R.id.focus_construction};
        int[] textViewIds = {R.id.focus_population_value,
                             R.id.focus_farming_value,
                             R.id.focus_mining_value,
                             R.id.focus_construction_value};
        for (int i = 0; i < 4; i++) {
            SeekBar seekBar = (SeekBar) view.findViewById(seekBarIds[i]);
            TextView textView = (TextView) view.findViewById(textViewIds[i]);
            seekBar.setMax(100);
            mSeekBars.add(seekBar);
            mTextViews.add(textView);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        redistribute(seekBar, progress / 100.0);
                    }
                }
            });
        }

        ((SeekBar) view.findViewById(R.id.focus_population))
                    .setProgress((int)(mColony.getPopulationFocus() * 100.0));
        ((SeekBar) view.findViewById(R.id.focus_farming))
                    .setProgress((int)(mColony.getFarmingFocus() * 100.0));
        ((SeekBar) view.findViewById(R.id.focus_mining))
                    .setProgress((int)(mColony.getMiningFocus() * 100.0));
        ((SeekBar) view.findViewById(R.id.focus_construction))
                    .setProgress((int)(mColony.getConstructionFocus() * 100.0));

        ((TextView) view.findViewById(R.id.focus_population_value))
                    .setText(Integer.toString((int)(mColony.getPopulationFocus() * 100.0)));
        ((TextView) view.findViewById(R.id.focus_farming_value))
                    .setText(Integer.toString((int)(mColony.getFarmingFocus() * 100.0)));
        ((TextView) view.findViewById(R.id.focus_mining_value))
                    .setText(Integer.toString((int)(mColony.getMiningFocus() * 100.0)));
        ((TextView) view.findViewById(R.id.focus_construction_value))
                    .setText(Integer.toString((int)(mColony.getConstructionFocus() * 100.0)));


        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(view);

        b.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onSetClick();
            }
        });
        b.setNegativeButton("Cancel", null);

        return b.create();
    }

    private void redistribute(SeekBar changedSeekBar, double newValue) {
        double otherValuesTotal = 0.0;
        for (SeekBar seekBar : mSeekBars) {
            if (seekBar == changedSeekBar)
                continue;
            otherValuesTotal += (seekBar.getProgress() / 100.0);
        }

        double desiredOtherValuesTotal = 1.0 - newValue;
        if (desiredOtherValuesTotal == 0.0)
            desiredOtherValuesTotal = 0.1;
        double ratio = otherValuesTotal / desiredOtherValuesTotal;

        for (int i = 0; i < 4; i++) {
            SeekBar seekBar = mSeekBars.get(i);
            TextView textView = mTextViews.get(i);
            if (seekBar != changedSeekBar) {
                seekBar.setProgress((int)(seekBar.getProgress() / ratio));
            }

            textView.setText(Integer.toString(seekBar.getProgress()));
        }
    }

    private void onSetClick() {
        ((StyledDialog) getDialog()).setCloseable(false);

        mColony.setPopulationFocus(mSeekBars.get(0).getProgress() / 100.0f);
        mColony.setFarmingFocus(mSeekBars.get(1).getProgress() / 100.0f);
        mColony.setMiningFocus(mSeekBars.get(2).getProgress() / 100.0f);
        mColony.setConstructionFocus(mSeekBars.get(3).getProgress() / 100.0f);

        final Activity activity = getActivity();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                String url = String.format("stars/%s/colonies/%s",
                                           mColony.getStarKey(),
                                           mColony.getKey());

                Messages.Colony pb = mColony.toProtocolBuffer();
                try {
                    pb = ApiClient.putProtoBuf(url, pb, Messages.Colony.class);
                } catch (ApiException e) {
                    log.error("Error updating colony!", e);
                }

                return null;
            }
            @Override
            protected void onPostExecute(Void unused) {
                // notify the StarManager that this star has been updated
                StarManager.getInstance().refreshStar(activity,
                                                      mColony.getStarKey());

                dismiss();
            }
        }.execute();
    }
}
