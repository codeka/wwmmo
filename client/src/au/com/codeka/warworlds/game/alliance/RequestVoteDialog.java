package au.com.codeka.warworlds.game.alliance;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceRequest;
import au.com.codeka.warworlds.model.AllianceManager;

public class RequestVoteDialog extends DialogFragment {
    private AllianceRequest mRequest;

    public void setRequest(AllianceRequest request) {
        mRequest = request;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new StyledDialog.Builder(getActivity())
                           .setMessage("Are you for or against this request?")
                           .setTitle("Approve / Reject")
                           .setPositiveButton("For", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   onApprove();
                               }
                           })
                           .setNegativeButton("Against", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onReject();
                                }
                            })
                           .setNeutralButton("Cancel", null)
                           .create();
    }

    private void onApprove() {
        updateRequest(true);
    }

    private void onReject() {
        updateRequest(false);
    }

    private void updateRequest(boolean approve) {
        AllianceManager.i.vote(mRequest, approve);
        dismiss();
    }
}
