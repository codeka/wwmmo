package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import au.com.codeka.warworlds.DialogManager;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.StarManager;
import au.com.codeka.warworlds.model.protobuf.Messages;

public class FocusDialog extends Dialog implements DialogManager.DialogConfigurable {
    private static Logger log = LoggerFactory.getLogger(FocusDialog.class);
    private Colony mColony;
    private List<SeekBar> mSeekBars;

    public static final int ID = 1002;

    public FocusDialog(Activity activity) {
        super(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.solarsystem_focus);

        mSeekBars = new ArrayList<SeekBar>();
        int[] ids = {R.id.solarsystem_colony_population_focus,
                     R.id.solarsystem_colony_farming_focus,
                     R.id.solarsystem_colony_mining_focus,
                     R.id.solarsystem_colony_construction_focus};
        for(int id : ids) {
            SeekBar seekBar = (SeekBar) findViewById(id);
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

        final Button okButton = (Button) findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeekBar populationFocus = (SeekBar) findViewById(R.id.solarsystem_colony_population_focus);
                SeekBar farmingFocus = (SeekBar) findViewById(R.id.solarsystem_colony_farming_focus);
                SeekBar miningFocus = (SeekBar) findViewById(R.id.solarsystem_colony_mining_focus);
                SeekBar constructionFocus = (SeekBar) findViewById(R.id.solarsystem_colony_construction_focus);

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
                        StarManager.getInstance().refreshStar(mColony.getStarKey());

                        okButton.setEnabled(true);
                        dismiss();
                    }
                }.execute();
            }
        });
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

    @Override
    public void setBundle(Activity activity, Bundle bundle) {
        mColony = (Colony) bundle.getParcelable("au.com.codeka.warworlds.Colony");

        SeekBar populationFocus = (SeekBar) findViewById(R.id.solarsystem_colony_population_focus);
        populationFocus.setProgress((int)(mColony.getPopulationFocus() * 100.0));
        SeekBar farmingFocus = (SeekBar) findViewById(R.id.solarsystem_colony_farming_focus);
        farmingFocus.setProgress((int)(mColony.getFarmingFocus() * 100.0));
        SeekBar miningFocus = (SeekBar) findViewById(R.id.solarsystem_colony_mining_focus);
        miningFocus.setProgress((int)(mColony.getMiningFocus() * 100.0));
        SeekBar constructionFocus = (SeekBar) findViewById(R.id.solarsystem_colony_construction_focus);
        constructionFocus.setProgress((int)(mColony.getConstructionFocus() * 100.0));

    }
}
