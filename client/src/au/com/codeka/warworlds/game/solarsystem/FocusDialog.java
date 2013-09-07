package au.com.codeka.warworlds.game.solarsystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.model.Colony;
import au.com.codeka.common.model.Model;
import au.com.codeka.common.model.Planet;
import au.com.codeka.common.model.Star;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.StarManager;

public class FocusDialog extends DialogFragment {
    private static Logger log = LoggerFactory.getLogger(FocusDialog.class);
    private Colony mColony;
    private Planet mPlanet;
    private List<SeekBar> mSeekBars;
    private List<TextView> mTextViews;
    private List<ImageButton> mLockButtons;
    private int mLockedIndex;
    private View mView;

    private static float SEEKBAR_MAX = 1000.0f;

    public FocusDialog() {
    }

    public void setColony(Star star, Colony colony) {
        mColony = colony;
        mPlanet = (Planet) star.planets.get(colony.planet_index - 1);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.focus_dlg, null);

        if (savedInstanceState != null) {
            byte[] bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Colony");
            if (bytes != null) {
                try {
                    mColony = Model.wire.parseFrom(bytes, Colony.class);
                    mPlanet = Model.wire.parseFrom(bytes, Planet.class);
                } catch (IOException e) {
                }
            }
        }

        mLockedIndex = -1;
        mSeekBars = new ArrayList<SeekBar>();
        mTextViews = new ArrayList<TextView>();
        mLockButtons = new ArrayList<ImageButton>();
        int[] seekBarIds = {R.id.focus_population,
                            R.id.focus_farming,
                            R.id.focus_mining,
                            R.id.focus_construction};
        int[] textViewIds = {R.id.focus_population_value,
                             R.id.focus_farming_value,
                             R.id.focus_mining_value,
                             R.id.focus_construction_value};
        int[] lockButtonIds = {R.id.focus_population_lock,
                               R.id.focus_farming_lock,
                               R.id.focus_mining_lock,
                               R.id.focus_construction_lock};
        for (int i = 0; i < 4; i++) {
            SeekBar seekBar = (SeekBar) mView.findViewById(seekBarIds[i]);
            TextView textView = (TextView) mView.findViewById(textViewIds[i]);
            ImageButton lockButton = (ImageButton) mView.findViewById(lockButtonIds[i]);
            seekBar.setMax((int) SEEKBAR_MAX);
            lockButton.setTag(i);
            seekBar.setTag(i);
            mSeekBars.add(seekBar);
            mTextViews.add(textView);
            mLockButtons.add(lockButton);

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
                        redistribute(seekBar, progress / SEEKBAR_MAX);
                    }
                }
            });

            lockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageButton thisLockButton = (ImageButton) v;
                    int index = (Integer) thisLockButton.getTag();

                    // if we're the locked one, unlock
                    if (mLockedIndex == index) {
                        mLockedIndex = -1;
                        thisLockButton.setImageResource(R.drawable.lock_opened);
                        mSeekBars.get(index).setEnabled(true);
                        return;
                    }

                    // otherwise we're now locked -- unlock all others
                    mLockedIndex = index;
                    for (int i = 0; i < 4; i++) {
                        if (i == index) {
                            mLockButtons.get(i).setImageResource(R.drawable.lock_closed);
                            mSeekBars.get(i).setEnabled(false);
                        } else {
                            mLockButtons.get(i).setImageResource(R.drawable.lock_opened);
                            mSeekBars.get(i).setEnabled(true);
                        }
                    }
                }
            });
        }

        ((SeekBar) mView.findViewById(R.id.focus_population))
                    .setProgress((int)(mColony.focus_population * SEEKBAR_MAX));
        ((SeekBar) mView.findViewById(R.id.focus_farming))
                    .setProgress((int)(mColony.focus_farming * SEEKBAR_MAX));
        ((SeekBar) mView.findViewById(R.id.focus_mining))
                    .setProgress((int)(mColony.focus_mining * SEEKBAR_MAX));
        ((SeekBar) mView.findViewById(R.id.focus_construction))
                    .setProgress((int)(mColony.focus_construction * SEEKBAR_MAX));

        ((TextView) mView.findViewById(R.id.focus_population_value))
                    .setText(focusToString(mColony.focus_population));
        ((TextView) mView.findViewById(R.id.focus_farming_value))
                    .setText(focusToString(mColony.focus_farming));
        ((TextView) mView.findViewById(R.id.focus_mining_value))
                    .setText(focusToString(mColony.focus_mining));
        ((TextView) mView.findViewById(R.id.focus_construction_value))
                    .setText(focusToString(mColony.focus_construction));

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

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mColony != null) {
            state.putByteArray("au.com.codeka.warworlds.Colony", mColony.toByteArray());
        }
        if (mPlanet != null) {
            state.putByteArray("au.com.codeka.warworlds.Planet", mPlanet.toByteArray());
        }
    }

    private static String focusToString(float focus) {
        return String.format("%d", Math.round(focus * 100.0f));
    }

    private void redistribute(SeekBar changedSeekBar, double newValue) {
        SeekBar lockedSeekBar = null;
        if (mLockedIndex >= 0) {
            lockedSeekBar = mSeekBars.get(mLockedIndex);
        }

        double otherValuesTotal = 0.0;
        for (SeekBar seekBar : mSeekBars) {
            if (seekBar == changedSeekBar)
                continue;
            int progress = seekBar.getProgress();
            otherValuesTotal += (progress / SEEKBAR_MAX);
        }

        double desiredOtherValuesTotal = 1.0 - newValue;
        if (lockedSeekBar != null) {
            desiredOtherValuesTotal -= (lockedSeekBar.getProgress() / SEEKBAR_MAX);
        }

        if (desiredOtherValuesTotal <= 0.0) {
            for (int i = 0; i < 4; i++) {
                SeekBar seekBar = mSeekBars.get(i);
                TextView textView = mTextViews.get(i);

                if (seekBar != changedSeekBar && seekBar != lockedSeekBar) {
                    seekBar.setProgress(0);
                }
                textView.setText("0");
            }
        }
        double ratio = otherValuesTotal / desiredOtherValuesTotal;

        for (int i = 0; i < 4; i++) {
            SeekBar seekBar = mSeekBars.get(i);
            TextView textView = mTextViews.get(i);
            if (seekBar != changedSeekBar && seekBar != lockedSeekBar) {
                int progress = seekBar.getProgress();
                seekBar.setProgress((int)(progress / ratio));
            }

            textView.setText(focusToString(seekBar.getProgress() / SEEKBAR_MAX));
        }

        updateDeltas();
    }

    private void updateDeltas() {
        float population = mColony.population;

        float focusFarming = (float) (((SeekBar) mView.findViewById(R.id.focus_farming)).getProgress() / SEEKBAR_MAX);
        float focusMining = (float) (((SeekBar) mView.findViewById(R.id.focus_mining)).getProgress() / SEEKBAR_MAX);

        float congenialityFarming = (float) mPlanet.farming_congeniality / 100.0f;
        float congenialityMining = (float) mPlanet.mining_congeniality / 100.0f;

        float rateFarming = population * focusFarming * congenialityFarming;
        float rateMining = population * focusMining * congenialityMining;

        ((TextView) mView.findViewById(R.id.focus_farming_delta))
                    .setText(String.format("%s%d / hr", (rateFarming < 0 ? "-" : "+"), Math.abs((int) rateFarming)));
        ((TextView) mView.findViewById(R.id.focus_mining_delta))
                    .setText(String.format("%s%d / hr", (rateMining < 0 ? "-" : "+"), Math.abs((int) rateMining)));
    }

    private void onSetClick() {
        mColony.focus_population = (float) (mSeekBars.get(0).getProgress() / SEEKBAR_MAX);
        mColony.focus_farming = (float) (mSeekBars.get(1).getProgress() / SEEKBAR_MAX);
        mColony.focus_mining = (float) (mSeekBars.get(2).getProgress() / SEEKBAR_MAX);
        mColony.focus_construction = (float) (mSeekBars.get(3).getProgress() / SEEKBAR_MAX);
        dismiss();

        new BackgroundRunner<Void>() {
            @Override
            protected Void doInBackground() {
                String url = String.format("stars/%s/colonies/%s", mColony.star_key, mColony.key);

                try {
                    ApiClient.putProtoBuf(url, mColony, Colony.class);
                } catch (ApiException e) {
                    log.error("Error updating colony!", e);
                }

                return null;
            }
            @Override
            protected void onComplete(Void unused) {
                // notify the StarManager that this star has been updated
                StarManager.i.refreshStar(mColony.star_key);
            }
        }.execute();
    }
}
