package au.com.codeka.warworlds.model;

import au.com.codeka.Cash;
import au.com.codeka.common.model.BaseAllianceRequest;
import au.com.codeka.common.model.BaseAllianceRequestVote;
import au.com.codeka.common.protobuf.Messages;

public class AllianceRequest extends BaseAllianceRequest {
    @Override
    protected BaseAllianceRequestVote createVote(Messages.AllianceRequestVote pb) {
        AllianceRequestVote vote = new AllianceRequestVote();
        if (pb != null) {
            vote.fromProtocolBuffer(pb);
        }
        return vote;
    }

    /**
     * Returns a user-readable description of this request.
     */
    public String getDescription() {
        switch (mRequestType) {
        case JOIN:
            return "Join";
        case LEAVE:
            return "Leave";
        case KICK:
            return "KICK";
        case DEPOSIT_CASH:
            return String.format("Despoit %s", Cash.format(mAmount));
        case WITHDRAW_CASH:
            return String.format("Withdraw %s", Cash.format(mAmount));
        case CHANGE_NAME:
            return String.format("Change alliance name to \"%s\"", mNewName);
        case CHANGE_IMAGE:
            return "Change image (TODO)";
        default:
            return "Unknown!";
        }
    }
}
