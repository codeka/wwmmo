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
import au.com.codeka.common.model.Alliance;
import au.com.codeka.common.model.AllianceRequest;
import au.com.codeka.common.model.Empire;
import au.com.codeka.common.model.Model;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceRequestHelper;
import au.com.codeka.warworlds.model.EmpireHelper;
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
        TextView requestBy = (TextView) mView.findViewById(R.id.request_by);

        Empire empire;
        if (mRequest.target_empire_id != null) {
            empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.target_empire_id));
        } else {
            empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.request_empire_id));
        }
        if (empire != null) {
            // it should never be null, so we won't bother refreshing...
            empireName.setText(empire.display_name);
            empireIcon.setImageBitmap(EmpireHelper.getShield(activity, empire));
        }
        requestDescription.setText(AllianceRequestHelper.getDescription(mRequest));
        message.setText(mRequest.message);

        if (mRequest.votes == 0) {
            requestVotes.setText("0");
        } else {
            requestVotes.setText(String.format(Locale.ENGLISH, "%s%d",
                    mRequest.votes < 0 ? "-" : "+", Math.abs(mRequest.votes)));
        }

        TreeSet<Integer> excludingEmpires = new TreeSet<Integer>(
                Arrays.asList(new Integer[] {mRequest.request_empire_id}));
        if (mRequest.target_empire_id != null) {
            excludingEmpires.add(mRequest.target_empire_id);
        }
        int totalPossibleVotes = Model.getTotalPossibleVotes(mAlliance, excludingEmpires);
        int requiredVotes = Model.getRequiredVotes(mRequest.request_type);
        if (requiredVotes > totalPossibleVotes) {
            requiredVotes = totalPossibleVotes;
        }
        requestRequiredVotes.setText(String.format(Locale.ENGLISH, "/ %d", requiredVotes));

        if (mRequest.target_empire_id != null) {
            empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.request_empire_id));

            requestBy.setVisibility(View.VISIBLE);
            requestBy.setText(String.format(Locale.ENGLISH, "by %s",
                    empire.display_name));
        } else {
            requestBy.setVisibility(View.GONE);
        }

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
