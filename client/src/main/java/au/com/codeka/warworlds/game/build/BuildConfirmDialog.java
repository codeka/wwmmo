package au.com.codeka.warworlds.game.build;

import org.joda.time.DateTime;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.Design;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.ctrl.BuildEstimateView;
import au.com.codeka.warworlds.model.BuildManager;
import au.com.codeka.warworlds.model.BuildRequest;
import au.com.codeka.warworlds.model.Building;
import au.com.codeka.warworlds.model.Colony;
import au.com.codeka.warworlds.model.DesignManager;
import au.com.codeka.warworlds.model.SpriteDrawable;
import au.com.codeka.warworlds.model.SpriteManager;
import au.com.codeka.warworlds.model.Star;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public class BuildConfirmDialog extends DialogFragment {
  private Star star;
  private Colony colony;
  private Design design;
  private Building existingBuilding;
  private View view;
  private BuildEstimateView buildEstimateView;

  public BuildConfirmDialog() {
  }

  public void setup(Design design, Star star, Colony colony) {
    this.design = design;
    this.star = star;
    this.colony = colony;
  }

  public void setup(Building existingBuilding, Star star, Colony colony) {
    this.existingBuilding = existingBuilding;
    design = existingBuilding.getDesign();
    this.star = star;
    this.colony = colony;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle state) {
    super.onSaveInstanceState(state);

    state.putString("au.com.codeka.warworlds.DesignID", design.getID());
    state.putInt("au.com.codeka.warworlds.DesignKind", design.getDesignKind().getValue());
    state.putString("au.com.codeka.warworlds.ColonyKey", colony.getKey());

    Messages.Star.Builder star_pb = Messages.Star.newBuilder();
    star.toProtocolBuffer(star_pb);
    state.putByteArray("au.com.codeka.warworlds.Star", star_pb.build().toByteArray());

    if (existingBuilding != null) {
      state.putString("au.com.codeka.warworlds.ExistingBuildingKey", existingBuilding.getKey());
    }
  }

  private void restoreSavedInstanceState(Bundle savedInstanceState) {
    byte[] bytes = savedInstanceState.getByteArray("au.com.codeka.warworlds.Star");
    try {
      Messages.Star star_pb;
      star_pb = Messages.Star.parseFrom(bytes);
      star = new Star();
      star.fromProtocolBuffer(star_pb);
    } catch (InvalidProtocolBufferException e) {
      // Ignore.
    }

    String colonyKey = savedInstanceState.getString("au.com.codeka.warworlds.ColonyKey");
    for (BaseColony baseColony : star.getColonies()) {
      if (baseColony.getKey().equals(colonyKey)) {
        colony = (Colony) baseColony;
      }
    }

    String existingBuildingKey = savedInstanceState.getString("au.com.codeka.warworlds.ExistingBuildingKey");
    if (existingBuildingKey != null) {
      for (BaseBuilding baseBuilding : colony.getBuildings()) {
        if (baseBuilding.getKey().equals(existingBuildingKey)) {
          existingBuilding = (Building) baseBuilding;
        }
      }
    }

    DesignKind designKind = DesignKind.fromNumber(savedInstanceState.getInt("au.com.codeka.warworlds.DesignKind"));
    String designID = savedInstanceState.getString("au.com.codeka.warworlds.DesignID");
    design = DesignManager.i.getDesign(designKind, designID);
  }

  @Override
  @NonNull
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Activity activity = checkNotNull(getActivity());
    LayoutInflater inflater = activity.getLayoutInflater();
    view = inflater.inflate(R.layout.build_confirm_dlg, null);

    if (savedInstanceState != null) {
      restoreSavedInstanceState(savedInstanceState);
    }

    final SeekBar countSeekBar = view.findViewById(R.id.build_count_seek);
    final EditText countEdit = view.findViewById(R.id.build_count_edit);
    buildEstimateView = view.findViewById(R.id.build_estimate);
    buildEstimateView.setOnBuildEstimateRefreshRequired(new BuildEstimateView.BuildEstimateRefreshRequiredHandler() {
      @Override
      public void onBuildEstimateRefreshRequired() {
        refreshBuildEstimates();
        refreshBuildNowCost();
      }
    });

    TextView nameTextView = view.findViewById(R.id.building_name);
    ImageView iconImageView = view.findViewById(R.id.building_icon);
    TextView descriptionTextView = view.findViewById(R.id.building_description);

    nameTextView.setText(design.getDisplayName());
    iconImageView.setImageDrawable(new SpriteDrawable(SpriteManager.i.getSprite(design.getSpriteName())));
    descriptionTextView.setText(Html.fromHtml(design.getDescription()));

    View upgradeContainer = view.findViewById(R.id.upgrade_container);
    View buildCountContainer = view.findViewById(R.id.build_count_container);
    if (design.canBuildMultiple() && existingBuilding == null) {
      buildCountContainer.setVisibility(View.VISIBLE);
      upgradeContainer.setVisibility(View.GONE);
    } else {
      buildCountContainer.setVisibility(View.GONE);
      if (existingBuilding != null) {
        upgradeContainer.setVisibility(View.VISIBLE);

        TextView timeToBuildLabel = view.findViewById(R.id.building_timetobuild_label);
        timeToBuildLabel.setText("Time to upgrade:");

        TextView currentLevel = view.findViewById(R.id.upgrade_current_level);
        currentLevel.setText(String.format(Locale.ENGLISH, "%d", existingBuilding.getLevel()));
      }
    }

    countEdit.setText("1");
    countSeekBar.setMax(99);
    countSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          countEdit.setText(String.format(Locale.ENGLISH, "%d", progress + 1));
          refreshBuildEstimates();
          refreshBuildNowCost();
        }
      }
    });

    countEdit.addTextChangedListener(new TextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        if (s.toString().length() == 0) {
          return;
        }
        int count = 1;
        try {
          count = Integer.parseInt(s.toString());
        } catch (Exception e) {
          // ignore errors here
        }
        if (count <= 0) {
          count = 1;
          countEdit.setText("1");
        }
        if (count <= 100) {
          countSeekBar.setProgress(count - 1);
        } else {
          countSeekBar.setProgress(99);
        }

        refreshBuildEstimates();
        refreshBuildNowCost();
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
    });

    refreshBuildEstimates();
    refreshBuildNowCost();

    StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
    if (existingBuilding == null) {
      b.setTitle("Build");
    } else {
      b.setTitle("Upgrade");
    }
    b.setView(view);

    String label = (existingBuilding == null ? "Build" : "Upgrade");
    b.setPositiveButton(label, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        onBuildClick(false);
      }
    });

    b.setNegativeButton("Cancel", null);

    view.findViewById(R.id.build_now_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onBuildClick(true);
      }
    });

    if (design.canBuildMultiple()) {
      final InputMethodManager imm =
          (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
      countEdit.post(new Runnable() {
        @Override
        public void run() {
          countEdit.requestFocus();
          countEdit.selectAll();
          imm.showSoftInput(countEdit, InputMethodManager.SHOW_IMPLICIT);
        }
      });
    }

    return b.create();
  }

  private void refreshBuildEstimates() {
    int count = 1;
    if (design.canBuildMultiple()) {
      final EditText countEdit = view.findViewById(R.id.build_count_edit);
      try {
        count = Integer.parseInt(countEdit.getText().toString());
      } catch (NumberFormatException e) {
        count = 1;
      }
    }

    final DateTime startTime = DateTime.now();

    BuildRequest buildRequest = new BuildRequest("FAKE_BUILD_REQUEST",
        design.getDesignKind(), design.getID(), colony.getKey(),
        startTime, count,
        (existingBuilding == null ? null : existingBuilding.getKey()),
        (existingBuilding == null ? 0 : existingBuilding.getLevel()),
        null, null, star.getKey(), colony.getPlanetIndex(), colony.getEmpireKey(),
        null);

    buildEstimateView.refresh(star, buildRequest);
  }

  private void refreshBuildNowCost() {
    double mineralsToUse = design.getBuildCost().getCostInMinerals();
    double cost = mineralsToUse * getCount();

    Button btn = view.findViewById(R.id.build_now_btn);
    btn.setText(String.format(
        Locale.ENGLISH,
        "%s now ($%.0f)",
        existingBuilding == null ? "Build" : "Upgrade",
        cost));
  }

  private void onBuildClick(boolean buildNow) {
    final Activity activity = getActivity();

    BuildManager.i.build(activity, colony, design, existingBuilding, getCount(), buildNow);
    dismiss();
  }

  private int getCount() {
    final EditText countEdit = view.findViewById(R.id.build_count_edit);

    int count = 1;
    if (design.canBuildMultiple()) {
      try {
        count = Integer.parseInt(countEdit.getText().toString());
      } catch (NumberFormatException e) {
        // Ignore for now.
      }
    }

    return count;
  }
}
