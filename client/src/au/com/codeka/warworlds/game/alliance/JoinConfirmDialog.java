package au.com.codeka.warworlds.game.alliance;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceJoinRequest;
import au.com.codeka.warworlds.model.AllianceManager;

public class JoinConfirmDialog extends DialogFragment {
    private AllianceJoinRequest mJoinRequest;

    public void setJoinRequest(AllianceJoinRequest joinRequest) {
        mJoinRequest = joinRequest;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new StyledDialog.Builder(getActivity())
                           .setMessage("Do you want to approve or reject this join request? Approving it will cause this empire to become a member of the alliance.")
                           .setTitle("Approve / Reject")
                           .setPositiveButton("Approve", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {
                                   onApprove();
                               }
                           })
                           .setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onReject();
                                }
                            })
                           .setNeutralButton("Cancel", null)
                           .create();
    }

    private void onApprove() {
        updateJoinRequest(true);
    }

    private void onReject() {
        updateJoinRequest(false);
    }

    private void updateJoinRequest(boolean approve) {
        mJoinRequest.setState(approve ? AllianceJoinRequest.RequestState.ACCEPTED
                                      : AllianceJoinRequest.RequestState.REJECTED);
        AllianceManager.i.updateJoinRequest(mJoinRequest);
        dismiss();
    }
}
