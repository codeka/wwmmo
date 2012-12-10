package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import au.com.codeka.warworlds.R;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.solarsystem_focus, container);

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

        final Button okButton = (Button) view.findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mColony.setPopulationFocus(populationFocus.getProgress() / 100.0f);
                mColony.setFarmingFocus(farmingFocus.getProgress() / 100.0f);
                mColony.setMiningFocus(miningFocus.getProgress() / 100.0f);
                mColony.setConstructionFocus(constructionFocus.getProgress() / 100.0f);

                // todo: show spinner?
                okButton.setEnabled(false);

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
                        StarManager.getInstance().refreshStar(
                                getActivity(), mColony.getStarKey());

                        okButton.setEnabled(true);
                        dismiss();
                    }
                }.execute();
            }
        });

        return view;
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
}
