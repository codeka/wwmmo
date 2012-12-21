package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
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

    public FocusDialog() {
    }

    public void setColony(Colony colony) {
        mColony = colony;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.focus_dlg, null);

        final SeekBar populationFocus = (SeekBar) view.findViewById(R.id.solarsystem_colony_population_focus);
        final SeekBar farmingFocus = (SeekBar) view.findViewById(R.id.solarsystem_colony_farming_focus);
        final SeekBar miningFocus = (SeekBar) view.findViewById(R.id.solarsystem_colony_mining_focus);
        final SeekBar constructionFocus = (SeekBar) view.findViewById(R.id.solarsystem_colony_construction_focus);

        populationFocus.setProgress((int)(mColony.getPopulationFocus() * 100.0));
        farmingFocus.setProgress((int)(mColony.getFarmingFocus() * 100.0));
        miningFocus.setProgress((int)(mColony.getMiningFocus() * 100.0));
        constructionFocus.setProgress((int)(mColony.getConstructionFocus() * 100.0));

        mSeekBars = new ArrayList<SeekBar>();
        int[] ids = {R.id.solarsystem_colony_population_focus,
                     R.id.solarsystem_colony_farming_focus,
                     R.id.solarsystem_colony_mining_focus,
                     R.id.solarsystem_colony_construction_focus};
        for(int id : ids) {
            SeekBar seekBar = (SeekBar) view.findViewById(id);
            seekBar.setMax(100);
            mSeekBars.add(seekBar);

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

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(view);

        b.setPositiveButton("Set", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        for (SeekBar seekBar : mSeekBars) {
            if (seekBar == changedSeekBar)
                continue;
            seekBar.setProgress((int)(seekBar.getProgress() / ratio));
        }
    }

    private void onSetClick() {
        ((StyledDialog) getDialog()).getPositiveButton().setEnabled(false);

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
