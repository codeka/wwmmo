package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BaseAllianceRequestVote;

public class AllianceRequestVote extends BaseAllianceRequestVote {
    public void setEmpireID(int empireID) {
        mEmpireID = empireID;
    }

    public void setVotes(int votes) {
        mVotes = votes;
    }
}
