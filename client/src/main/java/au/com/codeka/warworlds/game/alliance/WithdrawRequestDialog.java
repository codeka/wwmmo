package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceManager;

public class WithdrawRequestDialog extends DialogFragment {
  private View view;
  private int allianceID;

  public void setAllianceID(int allianceID) {
    this.allianceID = allianceID;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Activity activity = getActivity();
    LayoutInflater inflater = activity.getLayoutInflater();
    view = inflater.inflate(R.layout.alliance_request_dlg, null);

    TextView instructions = view.findViewById(R.id.instructions);
    instructions.setText("Enter the amount to withdraw, and the reason for wanting to withdraw " +
        "cash, then click \"Withdraw\". Your request must be approved before the cash is given " +
        "to you.");

    return new StyledDialog.Builder(getActivity())
        .setView(view)
        .setTitle("Withdraw Cash")
        .setPositiveButton("Withdraw", (dialog, which) -> onWithdraw())
        .setNegativeButton("Cancel", null)
        .create();
  }

  private void onWithdraw() {
    TextView message = view.findViewById(R.id.message);
    TextView amount = view.findViewById(R.id.amount);
    try {
      AllianceManager.i.requestWithdraw(allianceID, message.getText().toString(),
          Integer.parseInt(amount.getText().toString()));
    } catch (NumberFormatException e) {
      return;
    }

    dismiss();
  }
}

