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
import au.com.codeka.common.Log;
import au.com.codeka.common.TimeFormatter;
import au.com.codeka.common.model.BaseAllianceRequestVote;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.ImageHelper;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.eventbus.EventHandler;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.AllianceRequest;
import au.com.codeka.warworlds.model.Empire;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.EmpireShieldManager;
import au.com.codeka.warworlds.model.ShieldManager;

import com.google.protobuf.InvalidProtocolBufferException;

public class RequestVoteDialog extends DialogFragment {
  private View view;
  private Alliance alliance;
  private AllianceRequest request;
  private static final Log log = new Log("RequestVoteDialog");

  /**
   * Creates a new RequestVoteDialog and fills the supplied parameters into an
   * args Bundle. Usage of Bundle and serialization is necessary for correct
   * re-creation of fragments through android.
   */
  public static RequestVoteDialog newInstance(Alliance alliance, AllianceRequest request) {
    RequestVoteDialog requestVoteDialog = new RequestVoteDialog();
    Bundle args = new Bundle();
    Messages.Alliance.Builder abuilder = Messages.Alliance.newBuilder();
    Messages.AllianceRequest.Builder arbuilder = Messages.AllianceRequest.newBuilder();
    alliance.toProtocolBuffer(abuilder);
    request.toProtocolBuffer(arbuilder);
    args.putByteArray("alliance", abuilder.build().toByteArray());
    args.putByteArray("alliancerequest", arbuilder.build().toByteArray());

    requestVoteDialog.setArguments(args);
    return requestVoteDialog;
  }

  private AllianceRequest getRequest() {
    if (request == null) {
      Bundle args = this.getArguments();
      request = new AllianceRequest();
      try {
        Messages.AllianceRequest messagealliancerequest = Messages.AllianceRequest.parseFrom(
            args.getByteArray("alliancerequest"));
        request.fromProtocolBuffer(messagealliancerequest);
      } catch (InvalidProtocolBufferException e) {
        log.error("Could not read AllianceRequest from Protocol Buffer", e);
      }
    }
    return request;
  }

  private Alliance getAlliance() {
    if (alliance == null) {
      Bundle args = this.getArguments();
      alliance = new Alliance();
      try {
        Messages.Alliance messagealliance = Messages.Alliance.parseFrom(
            args.getByteArray("alliance"));
        alliance.fromProtocolBuffer(messagealliance);
      } catch (InvalidProtocolBufferException e) {
        log.error("Could not read Alliance from Protocol Buffer", e);
      }
    }
    return alliance;
  }

  @Override
  public void onStart() {
    super.onStart();
    EmpireManager.eventBus.register(eventHandler);
    ShieldManager.eventBus.register(eventHandler);
  }

  @Override
  public void onStop() {
    super.onStop();
    EmpireManager.eventBus.unregister(eventHandler);
    ShieldManager.eventBus.unregister(eventHandler);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Activity activity = getActivity();
    LayoutInflater inflater = activity.getLayoutInflater();
    view = inflater.inflate(R.layout.alliance_request_vote_dlg, null);

    refresh();

    StyledDialog.Builder dialogBuilder = new StyledDialog.Builder(getActivity()).setView(view);
    if (getRequest().getState() == AllianceRequest.RequestState.PENDING) {
      dialogBuilder.setPositiveButton("Save Vote", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          onSaveVote();
        }
      }).setNegativeButton("Cancel", null);
    } else {
      dialogBuilder.setNeutralButton("Close", null);
    }
    return dialogBuilder.create();
  }

  private void onSaveVote() {
    RadioButton ayeButton = (RadioButton) view.findViewById(R.id.vote_aye);
    RadioButton nayButton = (RadioButton) view.findViewById(R.id.vote_nay);

    if (ayeButton.isChecked()) {
      AllianceManager.i.vote(getRequest(), true);
    } else if (nayButton.isChecked()) {
      AllianceManager.i.vote(getRequest(), false);
    }

    dismiss();
  }

  private void refresh() {
    LayoutInflater inflater = getActivity().getLayoutInflater();

    TextView empireName = (TextView) view.findViewById(R.id.empire_name);
    ImageView empireIcon = (ImageView) view.findViewById(R.id.empire_icon);
    TextView requestDescription = (TextView) view.findViewById(R.id.request_description);
    ImageView pngImage = (ImageView) view.findViewById(R.id.png_image);
    TextView requestVotes = (TextView) view.findViewById(R.id.request_votes);
    TextView requestRequiredVotes = (TextView) view.findViewById(R.id.request_required_votes);
    TextView message = (TextView) view.findViewById(R.id.message);
    TextView requestBy = (TextView) view.findViewById(R.id.request_by);

    Empire empire;
    if (getRequest().getTargetEmpireID() != null) {
      empire = EmpireManager.i.getEmpire(getRequest().getTargetEmpireID());
    } else {
      empire = EmpireManager.i.getEmpire(getRequest().getRequestEmpireID());
    }
    if (empire != null) {
      // it should never be null, so we won't bother refreshing...
      empireName.setText(empire.getDisplayName());
      empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), empire));
    }
    requestDescription.setText(getRequest().getDescription());
    message.setText(getRequest().getMessage());

    if (getRequest().getNumVotes() == 0) {
      requestVotes.setText("0");
    } else {
      requestVotes.setText(String.format(Locale.ENGLISH, "%s%d",
          getRequest().getNumVotes() < 0 ? "-" : "+", Math.abs(getRequest().getNumVotes())));
    }

    if (getRequest().getPngImage() != null) {
      pngImage.setVisibility(View.VISIBLE);
      pngImage.setImageBitmap(new ImageHelper(getRequest().getPngImage()).getImage());
    } else {
      pngImage.setVisibility(View.GONE);
    }

    TreeSet<Integer> excludingEmpires = new TreeSet<Integer>(
        Arrays.asList(new Integer[] { getRequest().getRequestEmpireID() }));
    if (getRequest().getTargetEmpireID() != null) {
      excludingEmpires.add(getRequest().getTargetEmpireID());
    }
    int totalPossibleVotes = getAlliance().getTotalPossibleVotes(excludingEmpires);
    int requiredVotes = getRequest().getRequestType().getRequiredVotes();
    if (requiredVotes > totalPossibleVotes) {
      requiredVotes = totalPossibleVotes;
    }
    requestRequiredVotes.setText(String.format(Locale.ENGLISH, "/ %d", requiredVotes));

    if (getRequest().getTargetEmpireID() != null) {
      empire = EmpireManager.i.getEmpire(getRequest().getRequestEmpireID());
      if (empire != null) {
        requestBy.setVisibility(View.VISIBLE);
        requestBy.setText(String.format(Locale.ENGLISH, "by %s", empire.getDisplayName()));
      }
    } else {
      requestBy.setVisibility(View.GONE);
    }

    // if it's not pending, then you can't vote....
    if (getRequest().getState() != AllianceRequest.RequestState.PENDING) {
      view.findViewById(R.id.horz_sep_2).setVisibility(View.GONE);
      view.findViewById(R.id.votes).setVisibility(View.GONE);
    } else {
      view.findViewById(R.id.horz_sep_2).setVisibility(View.VISIBLE);
      view.findViewById(R.id.votes).setVisibility(View.VISIBLE);
    }

    LinearLayout currVoteContainer = (LinearLayout) view.findViewById(R.id.curr_votes);
    currVoteContainer.removeAllViews();
    if (!getRequest().getVotes().isEmpty()) {
      view.findViewById(R.id.curr_votes_none).setVisibility(View.GONE);
      for (BaseAllianceRequestVote vote : getRequest().getVotes()) {
        View v = inflater.inflate(R.layout.alliance_request_vote_empire_row, currVoteContainer,
            false);
        Empire voteEmpire = EmpireManager.i.getEmpire(vote.getEmpireID());
        if (voteEmpire != null) {
          empireIcon = (ImageView) v.findViewById(R.id.empire_icon);
          empireIcon.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), voteEmpire));
          empireName = (TextView) v.findViewById(R.id.empire_name);
          empireName.setText(voteEmpire.getDisplayName());
        }
        TextView voteDate = (TextView) v.findViewById(R.id.vote_time);
        voteDate.setText(TimeFormatter.create().format(vote.getDate()));
        TextView votes = (TextView) v.findViewById(R.id.empire_votes);
        votes.setText(String.format("%s%d", vote.getVotes() > 0 ? "+" : "-",
            Math.abs(vote.getVotes())));
        if (vote.getVotes() > 0) {
          votes.setTextColor(Color.GREEN);
        } else {
          votes.setTextColor(Color.RED);
        }
        currVoteContainer.addView(v);
      }
    } else {
      view.findViewById(R.id.curr_votes_none).setVisibility(View.VISIBLE);
    }
  }

  private Object eventHandler = new Object() {
    @EventHandler
    public void onShieldUpdated(ShieldManager.ShieldUpdatedEvent event) {
      refresh();
    }

    @EventHandler
    public void onEmpireFetched(Empire empire) {
      refresh();
    }
  };
}
