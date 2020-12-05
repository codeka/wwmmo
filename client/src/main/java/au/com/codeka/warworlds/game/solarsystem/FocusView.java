package au.com.codeka.warworlds.game.solarsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import au.com.codeka.BackgroundRunner;
import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.Planet;
import au.com.codeka.warworlds.model.Star;
import au.com.codeka.warworlds.model.StarManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FocusView extends FrameLayout {
  private static final Log log = new Log("FocusDialog");

  // Either colony/planet will be non-null or callback will be.
  @Nullable private Colony colony;
  @Nullable private Planet planet;

  private final List<SeekBar> seekBars;
  private final List<TextView> textViews;
  private final List<ImageButton> lockButtons;
  private final ArrayList<Integer> lockedIndexes;

  private static float SEEKBAR_MAX = 1000.0f;

  public FocusView(@Nonnull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    inflate(context, R.layout.focus_ctrl, this);

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
      SeekBar seekBar = findViewById(seekBarIds[i]);
      TextView textView = findViewById(textViewIds[i]);
      ImageButton lockButton = findViewById(lockButtonIds[i]);
      Button plusButton = findViewById(plusBtnIds[i]);
      Button minusButton = findViewById(minusBtnIds[i]);
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
        if (lockedIndexes.contains(index)) {
          // Be sure to call the overload that takes a value, not an index.
          lockedIndexes.remove((Object) index);
          thisLockButton.setImageResource(R.drawable.lock_opened);
          seekBars.get(index).setEnabled(true);
        } else {
          lockedIndexes.add(index);
          thisLockButton.setImageResource(R.drawable.lock_closed);
          seekBars.get(index).setEnabled(false);
        }

        // if there's two locked buttons, make sure the others are disabled, you can't lock more
        // than two at once.
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
  }

  public void setColony(Star star, Colony colony) {
    this.colony = colony;
    planet = (Planet) star.getPlanets()[colony.getPlanetIndex() - 1];

    ((SeekBar) findViewById(R.id.focus_population))
        .setProgress((int) (colony.getPopulationFocus() * SEEKBAR_MAX));
    ((SeekBar) findViewById(R.id.focus_farming))
        .setProgress((int) (colony.getFarmingFocus() * SEEKBAR_MAX));
    ((SeekBar) findViewById(R.id.focus_mining))
        .setProgress((int) (colony.getMiningFocus() * SEEKBAR_MAX));
    ((SeekBar) findViewById(R.id.focus_construction))
        .setProgress((int) (colony.getConstructionFocus() * SEEKBAR_MAX));

    ((TextView) findViewById(R.id.focus_population_value))
        .setText(focusToString(colony.getPopulationFocus()));
    ((TextView) findViewById(R.id.focus_farming_value))
        .setText(focusToString(colony.getFarmingFocus()));
    ((TextView) findViewById(R.id.focus_mining_value))
        .setText(focusToString(colony.getMiningFocus()));
    ((TextView) findViewById(R.id.focus_construction_value))
        .setText(focusToString(colony.getConstructionFocus()));
    updateDeltas();
  }

  public void setInitialValues(
      float initialFocusPopulation, float initialFocusFarming, float initialFocusMining,
      float initialFocusConstruction) {
    ((SeekBar) findViewById(R.id.focus_population))
        .setProgress((int) (initialFocusPopulation * SEEKBAR_MAX));
    ((SeekBar) findViewById(R.id.focus_farming))
        .setProgress((int) (initialFocusFarming * SEEKBAR_MAX));
    ((SeekBar) findViewById(R.id.focus_mining))
        .setProgress((int) (initialFocusMining * SEEKBAR_MAX));
    ((SeekBar) findViewById(R.id.focus_construction))
        .setProgress((int) (initialFocusConstruction * SEEKBAR_MAX));

    ((TextView) findViewById(R.id.focus_population_value)).setText(focusToString(0.25f));
    ((TextView) findViewById(R.id.focus_farming_value)).setText(focusToString(0.25f));
    ((TextView) findViewById(R.id.focus_mining_value)).setText(focusToString(0.25f));
    ((TextView) findViewById(R.id.focus_construction_value)).setText(focusToString(0.25f));

    updateDeltas();
  }

  public float getFocusPopulation() {
    return (seekBars.get(0).getProgress() / SEEKBAR_MAX);
  }

  public float getFocusFarming() {
    return (seekBars.get(1).getProgress() / SEEKBAR_MAX);
  }

  public float getFocusMining() {
    return (seekBars.get(2).getProgress() / SEEKBAR_MAX);
  }

  public float getFocusConstruction() {
    return (seekBars.get(3).getProgress() / SEEKBAR_MAX);
  }

  public void save() {
    colony.setPopulationFocus(getFocusPopulation());
    colony.setFarmingFocus(getFocusFarming());
    colony.setMiningFocus(getFocusMining());
    colony.setConstructionFocus(getFocusConstruction());

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
      ((TextView) findViewById(R.id.focus_farming_delta)).setText("-- / hr");
      ((TextView) findViewById(R.id.focus_mining_delta)).setText("-- / hr");
      return;
    }

    float population = colony.getPopulation();

    float focusFarming =
        (((SeekBar) findViewById(R.id.focus_farming)).getProgress() / SEEKBAR_MAX);
    float focusMining =
        (((SeekBar) findViewById(R.id.focus_mining)).getProgress() / SEEKBAR_MAX);

    float congenialityFarming = (float) planet.getFarmingCongeniality() / 100.0f;
    float congenialityMining = (float) planet.getMiningCongeniality() / 100.0f;

    float rateFarming = population * focusFarming * congenialityFarming;
    float rateMining = population * focusMining * congenialityMining;

    ((TextView) findViewById(R.id.focus_farming_delta))
        .setText(String.format(
            Locale.ENGLISH,
            "%s%d / hr",
            (rateFarming < 0 ? "-" : "+"), Math.abs((int) rateFarming)));
    ((TextView) findViewById(R.id.focus_mining_delta))
        .setText(String.format(
            Locale.ENGLISH,
            "%s%d / hr",
            (rateMining < 0 ? "-" : "+"), Math.abs((int) rateMining)));
  }
}
