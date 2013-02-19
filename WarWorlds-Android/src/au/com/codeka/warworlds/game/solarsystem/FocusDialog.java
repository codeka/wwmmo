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
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class FocusDialog extends DialogFragment {
    private static Logger log = LoggerFactory.getLogger(FocusDialog.class);
    private Colony mColony;
    private Planet mPlanet;
    private List<SeekBar> mSeekBars;
    private List<TextView> mTextViews;
    private View mView;

    public FocusDialog() {
    }

    public void setColony(Star star, Colony colony) {
        mColony = colony;
        mPlanet = star.getPlanets()[colony.getPlanetIndex() - 1];
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.focus_dlg, null);

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
            SeekBar seekBar = (SeekBar) mView.findViewById(seekBarIds[i]);
            TextView textView = (TextView) mView.findViewById(textViewIds[i]);
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

        ((SeekBar) mView.findViewById(R.id.focus_population))
                    .setProgress((int)(mColony.getPopulationFocus() * 100.0));
        ((SeekBar) mView.findViewById(R.id.focus_farming))
                    .setProgress((int)(mColony.getFarmingFocus() * 100.0));
        ((SeekBar) mView.findViewById(R.id.focus_mining))
                    .setProgress((int)(mColony.getMiningFocus() * 100.0));
        ((SeekBar) mView.findViewById(R.id.focus_construction))
                    .setProgress((int)(mColony.getConstructionFocus() * 100.0));

        ((TextView) mView.findViewById(R.id.focus_population_value))
                    .setText(Integer.toString((int)(mColony.getPopulationFocus() * 100.0)));
        ((TextView) mView.findViewById(R.id.focus_farming_value))
                    .setText(Integer.toString((int)(mColony.getFarmingFocus() * 100.0)));
        ((TextView) mView.findViewById(R.id.focus_mining_value))
                    .setText(Integer.toString((int)(mColony.getMiningFocus() * 100.0)));
        ((TextView) mView.findViewById(R.id.focus_construction_value))
                    .setText(Integer.toString((int)(mColony.getConstructionFocus() * 100.0)));

        updateDeltas();

        StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
        b.setView(mView);

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

        updateDeltas();
    }

    private void updateDeltas() {
        float population = mColony.getPopulation();

        float focusFarming = (float) ((SeekBar) mView.findViewById(R.id.focus_farming)).getProgress() / 100.0f;
        float focusMining = (float) ((SeekBar) mView.findViewById(R.id.focus_mining)).getProgress() / 100.0f;

        float congenialityFarming = (float) mPlanet.getFarmingCongeniality() / 100.0f;
        float congenialityMining = (float) mPlanet.getMiningCongeniality() / 100.0f;

        float rateFarming = population * focusFarming * congenialityFarming;
        float rateMining = population * focusMining * congenialityMining;

        ((TextView) mView.findViewById(R.id.focus_farming_delta))
                    .setText(String.format("%s%d / hr", (rateFarming < 0 ? "-" : "+"), Math.abs((int) rateFarming)));
        ((TextView) mView.findViewById(R.id.focus_mining_delta))
                    .setText(String.format("%s%d / hr", (rateMining < 0 ? "-" : "+"), Math.abs((int) rateMining)));
    }

    private void onSetClick() {
        mColony.setPopulationFocus(mSeekBars.get(0).getProgress() / 100.0f);
        mColony.setFarmingFocus(mSeekBars.get(1).getProgress() / 100.0f);
        mColony.setMiningFocus(mSeekBars.get(2).getProgress() / 100.0f);
        mColony.setConstructionFocus(mSeekBars.get(3).getProgress() / 100.0f);

        final Activity activity = getActivity();
        dismiss();

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
            }
        }.execute();
    }
}
