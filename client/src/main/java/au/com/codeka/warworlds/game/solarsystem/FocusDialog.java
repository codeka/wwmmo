package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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

  // Either colony/planet will be non-null or callback will be.
  @Nullable private Colony colony;
  @Nullable private Planet planet;
  @Nullable private Callback callback;

  private float initialFocusPopulation;
  private float initialFocusFarming;
  private float initialFocusMining;
  private float initialFocusConstruction;

  private List<SeekBar> seekBars;
  private List<TextView> textViews;
  private List<ImageButton> lockButtons;
  private ArrayList<Integer> lockedIndexes;
  private View view;

  public interface Callback {
    void onChangedClick(
        float focusPopulation, float focusFarming, float focusMining, float focusConstruction);
  }

  private static float SEEKBAR_MAX = 1000.0f;

  public FocusDialog() {
  }

  public void setColony(Star star, Colony colony) {
    this.colony = colony;
    planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];
  }

  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  public void setInitialValues(
      float initialFocusPopulation, float initialFocusFarming, float initialFocusMining,
      float initialFocusConstruction) {
    this.initialFocusPopulation = initialFocusPopulation;
    this.initialFocusFarming = initialFocusFarming;
    this.initialFocusMining = initialFocusMining;
    this.initialFocusConstruction = initialFocusConstruction;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.focus_dlg, null);
    if (savedInstanceState != null) {
      byte[] bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Colony");
      if (bytes != null) {
        try {
          Messages.Colony colony_pb;
          colony_pb = Messages.Colony.parseFrom(bytes);
          colony = new Colony();
          colony.fromProtocolBuffer(colony_pb);

          bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Planet");
          Messages.Planet planet_pb = Messages.Planet.parseFrom(bytes);
          planet = new Planet();
          planet.fromProtocolBuffer(null, planet_pb);
        } catch (InvalidProtocolBufferException e) {
        }
      }
    }

    lockedIndexes = new ArrayList<>();
    seekBars = new ArrayList<>();
    textViews = new ArrayList<>();
    lockButtons = new ArrayList<>();
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
      SeekBar seekBar = (SeekBar) view.findViewById(seekBarIds[i]);
      TextView textView = (TextView) view.findViewById(textViewIds[i]);
      ImageButton lockButton = (ImageButton) view.findViewById(lockButtonIds[i]);
      Button plusButton = (Button) view.findViewById(plusBtnIds[i]);
      Button minusButton = (Button) view.findViewById(minusBtnIds[i]);
      seekBar.setMax((int) SEEKBAR_MAX);
      lockButton.setTag(i);
      plusButton.setTag(i);
      minusButton.setTag(i);
      seekBar.setTag(i);
      seekBars.add(seekBar);
      textViews.add(textView);
      lockButtons.add(lockButton);

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

      lockButton.setOnClickListener(v -> {
        ImageButton thisLockButton = (ImageButton) v;
        int index = (Integer) thisLockButton.getTag();

        // if we're the locked one, unlock
        if (lockedIndexes != null && lockedIndexes.contains(index)) {
          lockedIndexes.remove(index);
          thisLockButton.setImageResource(R.drawable.lock_opened);
          seekBars.get(index).setEnabled(true);
        } else {
          lockedIndexes.add(index);
          thisLockButton.setImageResource(R.drawable.lock_closed);
          seekBars.get(index).setEnabled(false);
        }

        // if there's two locked buttons, make sure the others are disabled, you
        // can't lock more than two at once.
        for (int i1 = 0; i1 < 4; i1++) {
          boolean isLocked = lockedIndexes.contains(i1);
          if (isLocked || lockedIndexes.size() < 2) {
            lockButtons.get(i1).setEnabled(true);
          } else {
            lockButtons.get(i1).setEnabled(false);
          }
        }
      });

      plusButton.setOnClickListener(v -> {
        int index = (Integer) v.getTag();
        SeekBar seekBar1 = seekBars.get(index);
        int progress = seekBar1.getProgress() + (int) (SEEKBAR_MAX / 100);
        if (progress >= SEEKBAR_MAX) {
          progress = (int) SEEKBAR_MAX;
        }
        seekBar1.setProgress(progress);
        redistribute(seekBar1, progress / SEEKBAR_MAX);
      });

      minusButton.setOnClickListener(v -> {
        int index = (Integer) v.getTag();
        SeekBar seekBar12 = seekBars.get(index);
        int progress = seekBar12.getProgress() - (int) (SEEKBAR_MAX / 100);
        if (progress <= 0) {
          progress = 0;
        }
        seekBar12.setProgress(progress);
        redistribute(seekBar12, progress / SEEKBAR_MAX);
      });
    }

    if (colony != null) {
      ((SeekBar) view.findViewById(R.id.focus_population))
          .setProgress((int) (colony.getPopulationFocus() * SEEKBAR_MAX));
      ((SeekBar) view.findViewById(R.id.focus_farming))
          .setProgress((int) (colony.getFarmingFocus() * SEEKBAR_MAX));
      ((SeekBar) view.findViewById(R.id.focus_mining))
          .setProgress((int) (colony.getMiningFocus() * SEEKBAR_MAX));
      ((SeekBar) view.findViewById(R.id.focus_construction))
          .setProgress((int) (colony.getConstructionFocus() * SEEKBAR_MAX));

      ((TextView) view.findViewById(R.id.focus_population_value))
          .setText(focusToString(colony.getPopulationFocus()));
      ((TextView) view.findViewById(R.id.focus_farming_value))
          .setText(focusToString(colony.getFarmingFocus()));
      ((TextView) view.findViewById(R.id.focus_mining_value))
          .setText(focusToString(colony.getMiningFocus()));
      ((TextView) view.findViewById(R.id.focus_construction_value))
          .setText(focusToString(colony.getConstructionFocus()));
    } else {
        ((SeekBar) view.findViewById(R.id.focus_population))
            .setProgress((int) (initialFocusPopulation * SEEKBAR_MAX));
        ((SeekBar) view.findViewById(R.id.focus_farming))
            .setProgress((int) (initialFocusFarming * SEEKBAR_MAX));
        ((SeekBar) view.findViewById(R.id.focus_mining))
            .setProgress((int) (initialFocusMining * SEEKBAR_MAX));
        ((SeekBar) view.findViewById(R.id.focus_construction))
            .setProgress((int) (initialFocusConstruction * SEEKBAR_MAX));

        ((TextView) view.findViewById(R.id.focus_population_value)).setText(focusToString(0.25f));
        ((TextView) view.findViewById(R.id.focus_farming_value)).setText(focusToString(0.25f));
        ((TextView) view.findViewById(R.id.focus_mining_value)).setText(focusToString(0.25f));
        ((TextView) view.findViewById(R.id.focus_construction_value)).setText(focusToString(0.25f));
    }

    updateDeltas();

    StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
    b.setView(view);

    b.setPositiveButton("Set", (dialog, which) -> onSetClick());
    b.setNegativeButton("Cancel", null);

    return b.create();
  }

  @Override
  public void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    if (colony != null) {
      Messages.Colony.Builder colony_pb = Messages.Colony.newBuilder();
      colony.toProtocolBuffer(colony_pb);
      state.putByteArray("au.com.codeka.warworlds.Colony", colony_pb.build().toByteArray());
    }
    if (planet != null) {
      Messages.Planet.Builder planet_pb = Messages.Planet.newBuilder();
      planet.toProtocolBuffer(planet_pb);
      state.putByteArray("au.com.codeka.warworlds.Planet", planet_pb.build().toByteArray());
    }
  }

  private static String focusToString(float focus) {
    return String.format(Locale.ENGLISH, "%d", Math.round(focus * 100.0f));
  }

  private void redistribute(SeekBar changedSeekBar, double newValue) {
    double otherValuesTotal = 0.0;
    for (SeekBar seekBar : seekBars) {
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
        SeekBar seekBar = seekBars.get(i);
        TextView textView = textViews.get(i);

        if (seekBar != changedSeekBar && !lockedIndexes.contains(i)) {
          seekBar.setProgress(0);
        }
        textView.setText("0");
      }
    }
    double ratio = otherValuesTotal / desiredOtherValuesTotal;

    for (int i = 0; i < 4; i++) {
      SeekBar seekBar = seekBars.get(i);
      TextView textView = textViews.get(i);
      if (seekBar != changedSeekBar && !lockedIndexes.contains(i)) {
        int progress = seekBar.getProgress();
        if (progress == 0) {
          progress = 1;
        }
        seekBar.setProgress((int) (progress / ratio));
      }

      textView.setText(focusToString(seekBar.getProgress() / SEEKBAR_MAX));
    }

    updateDeltas();
  }

  private void updateDeltas() {
    if (colony == null) {
      ((TextView) view.findViewById(R.id.focus_farming_delta)).setText("-- / hr");
      ((TextView) view.findViewById(R.id.focus_mining_delta)).setText("-- / hr");
      return;
    }

    float population = colony.getPopulation();

    float focusFarming =
        (float) (((SeekBar) view.findViewById(R.id.focus_farming)).getProgress() / SEEKBAR_MAX);
    float focusMining =
        (float) (((SeekBar) view.findViewById(R.id.focus_mining)).getProgress() / SEEKBAR_MAX);

    float congenialityFarming = (float) planet.getFarmingCongeniality() / 100.0f;
    float congenialityMining = (float) planet.getMiningCongeniality() / 100.0f;

    float rateFarming = population * focusFarming * congenialityFarming;
    float rateMining = population * focusMining * congenialityMining;

    ((TextView) view.findViewById(R.id.focus_farming_delta))
        .setText(String.format(
            Locale.ENGLISH,
            "%s%d / hr",
            (rateFarming < 0 ? "-" : "+"), Math.abs((int) rateFarming)));
    ((TextView) view.findViewById(R.id.focus_mining_delta))
        .setText(String.format(
            Locale.ENGLISH,
            "%s%d / hr",
            (rateMining < 0 ? "-" : "+"), Math.abs((int) rateMining)));
  }

  private void onSetClick() {
    float focusPopulation = (float) (seekBars.get(0).getProgress() / SEEKBAR_MAX);
    float focusFarming = (float) (seekBars.get(1).getProgress() / SEEKBAR_MAX);
    float focusMining = (float) (seekBars.get(2).getProgress() / SEEKBAR_MAX);
    float focusConstruction = (float) (seekBars.get(3).getProgress() / SEEKBAR_MAX);

    if (callback != null) {
      callback.onChangedClick(focusPopulation, focusFarming, focusMining, focusConstruction);
      dismiss();
      return;
    }

    colony.setPopulationFocus(focusPopulation);
    colony.setFarmingFocus(focusFarming);
    colony.setMiningFocus(focusMining);
    colony.setConstructionFocus(focusConstruction);
    dismiss();

    new BackgroundRunner<Void>() {
      @Override
      protected Void doInBackground() {
        String url = String.format("stars/%s/colonies/%s",
            colony.getStarKey(),
            colony.getKey());

        Messages.Colony.Builder pb = Messages.Colony.newBuilder();
        colony.toProtocolBuffer(pb);
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
        StarManager.i.refreshStar(Integer.parseInt(colony.getStarKey()));
      }
    }.execute();
  }
}
