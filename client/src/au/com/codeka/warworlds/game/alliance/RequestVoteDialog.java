package au.com.codeka.warworlds.game.alliance;

import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import au.com.codeka.common.TimeInHours;
import au.com.codeka.common.model.BaseAllianceRequestVote;
import au.com.codeka.warworlds.ImageHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceRequest;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.ShieldManager;

public class RequestVoteDialog extends DialogFragment
                               implements EmpireManager.EmpireFetchedHandler,
                                          ShieldManager.ShieldUpdatedHandler {
    private View mView;
    private Alliance mAlliance;
    private AllianceRequest mRequest;

    public void setRequest(Alliance alliance, AllianceRequest request) {
        mAlliance = alliance;
        mRequest = request;
    }

    @Override
    public void onStart() {
        super.onStart();
        EmpireManager.i.addEmpireUpdatedListener(null, this);
        EmpireShieldManager.i.addShieldUpdatedHandler(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EmpireManager.i.removeEmpireUpdatedListener(this);
        EmpireShieldManager.i.removeShieldUpdatedHandler(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.alliance_request_vote_dlg, null);

        refresh();

        StyledDialog.Builder dialogBuilder = new StyledDialog.Builder(getActivity())
                   .setView(mView);
        if (mRequest.getState() == AllianceRequest.RequestState.PENDING) {
            dialogBuilder.setPositiveButton("Save Vote", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int which) {
                           onSaveVote();
                       }
                })
                .setNegativeButton("Cancel", null);
        } else {
            dialogBuilder.setNeutralButton("Close", null);
        }
        return dialogBuilder.create();
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

    private void refresh() {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        TextView empireName = (TextView) mView.findViewById(R.id.empire_name);
        ImageView empireIcon = (ImageView) mView.findViewById(R.id.empire_icon);
        TextView requestDescription = (TextView) mView.findViewById(R.id.request_description);
        ImageView pngImage = (ImageView) mView.findViewById(R.id.png_image);
        TextView requestVotes = (TextView) mView.findViewById(R.id.request_votes);
        TextView requestRequiredVotes = (TextView) mView.findViewById(R.id.request_required_votes);
        TextView message = (TextView) mView.findViewById(R.id.message);
        TextView requestBy = (TextView) mView.findViewById(R.id.request_by);

        Empire empire;
        if (mRequest.getTargetEmpireID() != null) {
            empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.getTargetEmpireID()));
        } else {
            empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.getRequestEmpireID()));
        }
        if (empire != null) {
            // it should never be null, so we won't bother refreshing...
            empireName.setText(empire.getDisplayName());
            empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
        }
        requestDescription.setText(mRequest.getDescription());
        message.setText(mRequest.getMessage());

        if (mRequest.getNumVotes() == 0) {
            requestVotes.setText("0");
        } else {
            requestVotes.setText(String.format(Locale.ENGLISH, "%s%d",
                    mRequest.getNumVotes() < 0 ? "-" : "+", Math.abs(mRequest.getNumVotes())));
        }

        if (mRequest.getPngImage() != null) {
            pngImage.setVisibility(View.VISIBLE);
            pngImage.setImageBitmap(new ImageHelper(mRequest.getPngImage()).getImage());
        } else {
            pngImage.setVisibility(View.GONE);
        }

        TreeSet<Integer> excludingEmpires = new TreeSet<Integer>(
                Arrays.asList(new Integer[] {mRequest.getRequestEmpireID()}));
        if (mRequest.getTargetEmpireID() != null) {
            excludingEmpires.add(mRequest.getTargetEmpireID());
        }
        int totalPossibleVotes = mAlliance.getTotalPossibleVotes(excludingEmpires);
        int requiredVotes = mRequest.getRequestType().getRequiredVotes();
        if (requiredVotes > totalPossibleVotes) {
            requiredVotes = totalPossibleVotes;
        }
        requestRequiredVotes.setText(String.format(Locale.ENGLISH, "/ %d", requiredVotes));

        if (mRequest.getTargetEmpireID() != null) {
            empire = EmpireManager.i.getEmpire(Integer.toString(mRequest.getRequestEmpireID()));

            requestBy.setVisibility(View.VISIBLE);
            requestBy.setText(String.format(Locale.ENGLISH, "by %s",
                    empire.getDisplayName()));
        } else {
            requestBy.setVisibility(View.GONE);
        }

        // if it's not pending, then you can't vote....
        if (mRequest.getState() != AllianceRequest.RequestState.PENDING) {
            mView.findViewById(R.id.horz_sep_2).setVisibility(View.GONE);
            mView.findViewById(R.id.votes).setVisibility(View.GONE);
        } else {
            mView.findViewById(R.id.horz_sep_2).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.votes).setVisibility(View.VISIBLE);
        }

        LinearLayout currVoteContainer = (LinearLayout) mView.findViewById(R.id.curr_votes);
        currVoteContainer.removeAllViews();
        if (!mRequest.getVotes().isEmpty()) {
            mView.findViewById(R.id.curr_votes_none).setVisibility(View.GONE);
            for (BaseAllianceRequestVote vote : mRequest.getVotes()) {
                View v = inflater.inflate(R.layout.alliance_request_vote_empire_row, currVoteContainer, false);
                Empire voteEmpire = EmpireManager.i.getEmpire(Integer.toString(vote.getEmpireID()));
                if (voteEmpire != null) {
                    empireIcon = (ImageView) v.findViewById(R.id.empire_icon);
                    empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), voteEmpire));
                    empireName = (TextView) v.findViewById(R.id.empire_name);
                    empireName.setText(voteEmpire.getDisplayName());
                } else {
                    EmpireManager.i.refreshEmpire(Integer.toString(vote.getEmpireID()));
                }
                TextView voteDate = (TextView) v.findViewById(R.id.vote_time);
                voteDate.setText(TimeInHours.format(vote.getDate()));
                TextView votes = (TextView) v.findViewById(R.id.empire_votes);
                votes.setText(String.format("%s%d", vote.getVotes() > 0 ? "+" : "-", Math.abs(vote.getVotes())));
                if (vote.getVotes() > 0) {
                    votes.setTextColor(Color.GREEN);
                } else {
                    votes.setTextColor(Color.RED);
                }
                currVoteContainer.addView(v);
            }
        } else {
            mView.findViewById(R.id.curr_votes_none).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEmpireFetched(Empire empire) {
        refresh();
    }

    @Override
    public void onShieldUpdated(int id) {
        refresh();
    }
}
