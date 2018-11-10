package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceManager;

public class DepositRequestDialog extends DialogFragment {
    private View mView;
    private int mAllianceID;

    public void setAllianceID(int allianceID) {
        mAllianceID = allianceID;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.alliance_request_dlg, null);

        TextView instructions = mView.findViewById(R.id.instructions);
        instructions.setText("Enter the amount to deposit, and a brief description of why you're depositing (optional), then click \"Deposit\".");

        return new StyledDialog.Builder(getActivity())
                           .setView(mView)
                           .setTitle("Deposit Cash")
                           .setPositiveButton("Deposit", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   onDeposit();
                               }
                           })
                           .setNegativeButton("Cancel", null)
                           .create();
    }

    private void onDeposit() {
        TextView message = mView.findViewById(R.id.message);
        TextView amountView = mView.findViewById(R.id.amount);
        long amount = 0;
        try {
            amount = Long.parseLong(amountView.getText().toString());
        } catch (NumberFormatException e) {
            TextView instructions = mView.findViewById(R.id.instructions);
            instructions.setText("Make sure you enter a valid amount of cash to deposit.");
            return;
        }
        AllianceManager.i.requestDeposit(mAllianceID, message.getText().toString(), amount);

        dismiss();
    }
}

