package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

import com.google.protobuf.InvalidProtocolBufferException;

public class FocusDialog extends DialogFragment {
    private static final Log log = new Log("FocusDialog");
    private Colony mColony;
    private Planet mPlanet;
    private List<SeekBar> mSeekBars;
    private List<TextView> mTextViews;
    private List<ImageButton> mLockButtons;
    private ArrayList<Integer> mLockedIndexes;
    private View mView;

    private static float SEEKBAR_MAX = 1000.0f;

    public FocusDialog() {
    }

    public void setColony(Star star, Colony colony) {
        mColony = colony;
        mPlanet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.focus_dlg, null);
        if (savedInstanceState != null) {
            byte[] bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Colony");
            if (bytes != null) {
                try {
                    Messages.Colony colony_pb;
                    colony_pb = Messages.Colony.parseFrom(bytes);
                    mColony = new Colony();
                    mColony.fromProtocolBuffer(colony_pb);
    
                    bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Planet");
                    Messages.Planet planet_pb = Messages.Planet.parseFrom(bytes);
                    mPlanet = new Planet();
                    mPlanet.fromProtocolBuffer(null, planet_pb);
                } catch (InvalidProtocolBufferException e) {
                }
            }
        }

        mLockedIndexes = new ArrayList<Integer>();
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
        int[] plusBtnIds = {R.id.focus_population_plus_btn,
                            R.id.focus_farming_plus_btn,
                            R.id.focus_mining_plus_btn,
                            R.id.focus_construction_plus_btn};
        int[] minusBtnIds = {R.id.focus_population_minus_btn,
                             R.id.focus_farming_minus_btn,
                             R.id.focus_mining_minus_btn,
                             R.id.focus_construction_minus_btn};
        for (int i = 0; i < 4; i++) {
            SeekBar seekBar = (SeekBar) mView.findViewById(seekBarIds[i]);
            TextView textView = (TextView) mView.findViewById(textViewIds[i]);
            ImageButton lockButton = (ImageButton) mView.findViewById(lockButtonIds[i]);
            Button plusButton = (Button) mView.findViewById(plusBtnIds[i]);
            Button minusButton = (Button) mView.findViewById(minusBtnIds[i]);
            seekBar.setMax((int) SEEKBAR_MAX);
            lockButton.setTag(i);
            plusButton.setTag(i);
            minusButton.setTag(i);
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
                    if (mLockedIndexes != null && mLockedIndexes.contains(index)) {
                        mLockedIndexes.remove((Object) index);
                        thisLockButton.setImageResource(R.drawable.lock_opened);
                        mSeekBars.get(index).setEnabled(true);
                    } else {
                        mLockedIndexes.add(index);
                        thisLockButton.setImageResource(R.drawable.lock_closed);
                        mSeekBars.get(index).setEnabled(false);
                    }

                    // if there's two locked buttons, make sure the others are disabled, you
                    // can't lock more than two at once.
                    for (int i = 0; i < 4; i++) {
                        boolean isLocked = mLockedIndexes.contains(i);
                        if (isLocked || mLockedIndexes.size() < 2) {
                            mLockButtons.get(i).setEnabled(true);
                        } else {
                            mLockButtons.get(i).setEnabled(false);
                        }
                    }
                }
            });

            plusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = (Integer) v.getTag();
                    SeekBar seekBar = mSeekBars.get(index);
                    int progress = seekBar.getProgress() + (int) (SEEKBAR_MAX / 100);
                    if (progress >= SEEKBAR_MAX) {
                        progress = (int) SEEKBAR_MAX;
                    }
                    seekBar.setProgress(progress);
                    redistribute(seekBar, progress / SEEKBAR_MAX);
                }
            });

            minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = (Integer) v.getTag();
                    SeekBar seekBar = mSeekBars.get(index);
                    int progress = seekBar.getProgress() - (int) (SEEKBAR_MAX / 100);
                    if (progress <= 0) {
                        progress = 0;
                    }
                    seekBar.setProgress(progress);
                    redistribute(seekBar, progress / SEEKBAR_MAX);
                }
            });
        }

        ((SeekBar) mView.findViewById(R.id.focus_population))
                    .setProgress((int)(mColony.getPopulationFocus() * SEEKBAR_MAX));
        ((SeekBar) mView.findViewById(R.id.focus_farming))
                    .setProgress((int)(mColony.getFarmingFocus() * SEEKBAR_MAX));
        ((SeekBar) mView.findViewById(R.id.focus_mining))
                    .setProgress((int)(mColony.getMiningFocus() * SEEKBAR_MAX));
        ((SeekBar) mView.findViewById(R.id.focus_construction))
                    .setProgress((int)(mColony.getConstructionFocus() * SEEKBAR_MAX));

        ((TextView) mView.findViewById(R.id.focus_population_value))
                    .setText(focusToString(mColony.getPopulationFocus()));
        ((TextView) mView.findViewById(R.id.focus_farming_value))
                    .setText(focusToString(mColony.getFarmingFocus()));
        ((TextView) mView.findViewById(R.id.focus_mining_value))
                    .setText(focusToString(mColony.getMiningFocus()));
        ((TextView) mView.findViewById(R.id.focus_construction_value))
                    .setText(focusToString(mColony.getConstructionFocus()));

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
            Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
            mColony.toProtocolBuffer(colony_pb);
            state.putByteArray("au.com.codeka.warworlds.Colony", colony_pb.build().toByteArray());
        }
        if (mPlanet != null) {
            Messages.Planet.Builder planet_pb = Messages.Planet.newBuilder();
            mPlanet.toProtocolBuffer(planet_pb);
            state.putByteArray("au.com.codeka.warworlds.Planet", planet_pb.build().toByteArray());
        }
    }

    private static String focusToString(float focus) {
        return String.format("%d", Math.round(focus * 100.0f));
    }

    private void redistribute(SeekBar changedSeekBar, double newValue) {
        double otherValuesTotal = 0.0;
        for (SeekBar seekBar : mSeekBars) {
            if (seekBar == changedSeekBar)
                continue;
            int progress = seekBar.getProgress();
            if (progress == 0) {
                progress = 1;
            }
            otherValuesTotal += (progress / SEEKBAR_MAX);
        }

        double desiredOtherValuesTotal = 1.0 - newValue;
         if (desiredOtherValuesTotal <= 0.0) {
            for (int i = 0; i < 4; i++) {
                SeekBar seekBar = mSeekBars.get(i);
                TextView textView = mTextViews.get(i);

                if (seekBar != changedSeekBar && !mLockedIndexes.contains(i)) {
                    seekBar.setProgress(0);
                }
                textView.setText("0");
            }
        }
        double ratio = otherValuesTotal / desiredOtherValuesTotal;

        for (int i = 0; i < 4; i++) {
            SeekBar seekBar = mSeekBars.get(i);
            TextView textView = mTextViews.get(i);
            if (seekBar != changedSeekBar && !mLockedIndexes.contains(i)) {
                int progress = seekBar.getProgress();
                if (progress == 0) {
                    progress = 1;
                }
                seekBar.setProgress((int)(progress / ratio));
            }

            textView.setText(focusToString(seekBar.getProgress() / SEEKBAR_MAX));
        }

        updateDeltas();
    }

    private void updateDeltas() {
        float population = mColony.getPopulation();

        float focusFarming = (float) (((SeekBar) mView.findViewById(R.id.focus_farming)).getProgress() / SEEKBAR_MAX);
        float focusMining = (float) (((SeekBar) mView.findViewById(R.id.focus_mining)).getProgress() / SEEKBAR_MAX);

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
        mColony.setPopulationFocus((float) (mSeekBars.get(0).getProgress() / SEEKBAR_MAX));
        mColony.setFarmingFocus((float) (mSeekBars.get(1).getProgress() / SEEKBAR_MAX));
        mColony.setMiningFocus((float) (mSeekBars.get(2).getProgress() / SEEKBAR_MAX));
        mColony.setConstructionFocus((float) (mSeekBars.get(3).getProgress() / SEEKBAR_MAX));
        dismiss();

        new BackgroundRunner<Void>() {
            @Override
            protected Void doInBackground() {
                String url = String.format("stars/%s/colonies/%s",
                                           mColony.getStarKey(),
                                           mColony.getKey());

                Messages.Colony.Builder pb = Messages.Colony.newBuilder();
                mColony.toProtocolBuffer(pb);
                try {
                    ApiClient.putProtoBuf(url, pb.build(), Messages.Colony.class);
                } catch (ApiException e) {
                    log.error("Error updating colony!", e);
                }

                return null;
            }
            @Override
            protected void onComplete(Void unused) {
                // notify the StarManager that this star has been updated
                StarManager.getInstance().refreshStar(mColony.getStarKey());
            }
        }.execute();
    }
}
