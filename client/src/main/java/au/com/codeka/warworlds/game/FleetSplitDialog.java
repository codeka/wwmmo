package au.com.codeka.warworlds.game;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.App;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;
import au.com.codeka.warworlds.concurrency.Threads;
import au.com.codeka.warworlds.ctrl.FleetListRow;
import au.com.codeka.warworlds.model.Fleet;
import au.com.codeka.warworlds.model.StarManager;

public class FleetSplitDialog extends DialogFragment {
  private Fleet fleet;
  private View view;

  public FleetSplitDialog() {
  }

  public void setFleet(Fleet fleet) {
    this.fleet = fleet;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (fleet == null) {
      return null;
    }

    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.fleet_split_dlg, null);

    final SeekBar splitRatio = view.findViewById(R.id.split_ratio);
    final TextView splitLeft = view.findViewById(R.id.split_left);
    final TextView splitRight = view.findViewById(R.id.split_right);

    FleetListRow fleetView = view.findViewById(R.id.fleet);
    fleetView.setFleet(fleet);

    int numShips = (int) Math.ceil(fleet.getNumShips());
    if (numShips >= 2) {
      splitRatio.setMax(numShips - 2);
    } else {
      splitRatio.setMax(numShips - 1);
    }
    splitRatio.setProgress(numShips / 2 - 1);
    splitLeft.setText(String.format(Locale.ENGLISH, "%d", numShips / 2));
    splitRight.setText(String.format(Locale.ENGLISH, "%d", numShips - (numShips / 2)));

    splitRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        splitLeft.setText(String.format(Locale.ENGLISH, "%d", progress + 1));
        splitRight.setText(String.format(Locale.ENGLISH, "%d", seekBar.getMax() - progress + 1));
      }
    });

    splitLeft.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          try {
            int val = Integer.parseInt(splitLeft.getText().toString());
            if (val < 0) {
              val = 0;
            }
            if (val > splitRatio.getMax()) {
              val = splitRatio.getMax();
            }
            splitRatio.setProgress(val - 1);
          } catch (NumberFormatException e) {
            // Something went wrong trying to parse the value. We'll assume it was "1"
            splitRatio.setProgress(0);
          }
        }
      }
    });

    splitRight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          try {
            int val = Integer.parseInt(splitLeft.getText().toString());
            if (val < 0) {
              val = 0;
            }
            if (val > splitRatio.getMax()) {
              val = splitRatio.getMax();
            }
            splitRatio.setProgress(splitRatio.getMax() - val + 1);
          } catch (NumberFormatException e) {
            splitRatio.setProgress(splitRatio.getMax());
          }
        }
      }
    });

    StyledDialog.Builder b = new StyledDialog.Builder(getActivity());
    b.setView(view);
    b.setTitle("Split Fleet");

    b.setPositiveButton("Split", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        onSplitClick();
      }
    });

    b.setNegativeButton("Cancel", null);

    return b.create();
  }

  private void onSplitClick() {
    ((StyledDialog) getDialog()).setCloseable(false);

    final TextView splitLeft = view.findViewById(R.id.split_left);
    final TextView splitRight = view.findViewById(R.id.split_right);
    dismiss();

    App.i.getTaskRunner().runTask(() -> {
      String url = String.format("stars/%s/fleets/%s/orders",
          fleet.getStarKey(),
          fleet.getKey());
      Messages.FleetOrder fleetOrder = Messages.FleetOrder.newBuilder()
          .setOrder(Messages.FleetOrder.FLEET_ORDER.SPLIT)
          .setSplitLeft(Integer.parseInt(splitLeft.getText().toString()))
          .setSplitRight(Integer.parseInt(splitRight.getText().toString()))
          .build();

      try {
        ApiClient.postProtoBuf(url, fleetOrder);

        // the star this fleet is attached to needs to be refreshed...
        StarManager.i.refreshStar(Integer.parseInt(fleet.getStarKey()));
      } catch (ApiException e) {
        // TODO: do something..?
      }
    }, Threads.BACKGROUND);
  }
}
