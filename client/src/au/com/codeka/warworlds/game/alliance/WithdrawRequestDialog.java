package au.com.codeka.warworlds.game.alliance;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceManager;

public class WithdrawRequestDialog extends DialogFragment {
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

        TextView instructions = (TextView) mView.findViewById(R.id.instructions);
        instructions.setText("Enter the amount to withdraw, and the reason for wanting to withdraw cash, then click \"Withdraw\". Your request must be approved before the cash is given to you.");

        return new StyledDialog.Builder(getActivity())
                           .setView(mView)
                           .setTitle("Withdraw Cash")
                           .setPositiveButton("Withdraw", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   onWithdraw();
                               }
                           })
                           .setNegativeButton("Cancel", null)
                           .create();
    }

    private void onWithdraw() {
        TextView message = (TextView) mView.findViewById(R.id.message);
        TextView amount = (TextView) mView.findViewById(R.id.amount);
        AllianceManager.i.requestWithdraw(mAllianceID, message.getText().toString(),
                            Integer.parseInt(amount.getText().toString()));

        dismiss();
    }
}

