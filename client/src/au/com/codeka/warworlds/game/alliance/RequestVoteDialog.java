package au.com.codeka.warworlds.game.alliance;

import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceRequest;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;

public class RequestVoteDialog extends DialogFragment {
    private View mView;
    private Alliance mAlliance;
    private AllianceRequest mRequest;

    public void setRequest(Alliance alliance, AllianceRequest request) {
        mAlliance = alliance;
        mRequest = request;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.alliance_request_vote_dlg, null);

        TextView empireName = (TextView) mView.findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) mView.findViewById(R.id.empire_icon);
        TextView requestDescription = (TextView) mView.findViewById(R.id.request_description);
        TextView requestVotes = (TextView) mView.findViewById(R.id.request_votes);
        TextView requestRequiredVotes = (TextView) mView.findViewById(R.id.request_required_votes);
        TextView message = (TextView) mView.findViewById(R.id.message);

        Empire empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.getRequestEmpireID()));
        if (empire != null) {
            // it should never be null, so we won't bother refreshing...
            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(empire.getShield(activity));
        }
        requestDescription.setText(mRequest.getDescription());
        message.setText(mRequest.getMessage());

        if (mRequest.getVotes() == 0) {
            requestVotes.setText("0");
        } else {
            requestVotes.setText(String.format(Locale.ENGLISH, "%s%d",
                    mRequest.getVotes() < 0 ? "-" : "+", Math.abs(mRequest.getVotes())));
        }

        TreeSet<Integer> excludingEmpires = new TreeSet<Integer>(
                Arrays.asList(new Integer[] {mRequest.getRequestEmpireID()}));
        int totalPossibleVotes = mAlliance.getTotalPossibleVotes(excludingEmpires);
        int requiredVotes = mRequest.getRequestType().getRequiredVotes();
        if (requiredVotes > totalPossibleVotes) {
            requiredVotes = totalPossibleVotes;
        }
        requestRequiredVotes.setText(String.format(Locale.ENGLISH, "/ %d", requiredVotes));

        return new StyledDialog.Builder(getActivity())
                   .setView(mView)
                   .setPositiveButton("Save Vote", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           onSaveVote();
                       }
                   })
                   .setNegativeButton("Cancel", null)
                   .create();
    }

    private void onSaveVote() {
        RadioButton ayeButton = (RadioButton) mView.findViewById(R.id.vote_aye);
        RadioButton nayButton = (RadioButton) mView.findViewById(R.id.vote_nay);

        if (ayeButton.isChecked()) {
            AllianceManager.i.vote(mRequest, true);
        } else if (nayButton.isChecked()) {
            AllianceManager.i.vote(mRequest, false);
        }

        dismiss();
    }
}
