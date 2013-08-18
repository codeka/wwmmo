package au.com.codeka.warworlds.server.ctrl;

import java.util.Arrays;
import java.util.TreeSet;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.AllianceMember;
import au.com.codeka.warworlds.server.model.AllianceRequest;


/**
 * This is the base class for the alliance request processors (which are actually inner classes
 * of this class as well). For each \c RequestType, we may have different vote requirements,
 * difference effects and so on.
 */
public abstract class AllianceRequestProcessor {
    /**
     * Gets an \c AllianceRequestProcessor for the given \see AllianceRequest.
     */
    public static AllianceRequestProcessor get(Alliance alliance, AllianceRequest request) {
        switch (request.getRequestType()) {
        case JOIN:
            return new JoinRequestProcessor(alliance, request);
        case LEAVE:
            return new LeaveRequestProcessor(alliance, request);
        case KICK:
            return new KickRequestProcessor(alliance, request);
        case DEPOSIT_CASH:
            return new DepositCashRequestProcessor(alliance, request);
        case WITHDRAW_CASH:
            return new WithdrawCashRequestProcessor(alliance, request);
        }

        throw new UnsupportedOperationException("Unknown request type: " + request.getRequestType());
    }

    protected Alliance mAlliance;
    protected AllianceRequest mRequest;

    protected AllianceRequestProcessor(Alliance alliance, AllianceRequest request) {
        mRequest = request;
        mAlliance = alliance;
    }

    /**
     * Called when we receive a vote for this request. If we've received enough votes, then the
     * request is passed. If we've received enough negative votes, the motion is denied.
     */
    public void onVote(AllianceController ctrl) throws Exception {
        int requiredVotes = mRequest.getRequestType().getRequiredVotes();
        int totalPossibleVotes = getTotalPossibleVotes();
        if (requiredVotes > totalPossibleVotes) {
            requiredVotes = totalPossibleVotes;
        }
        if (requiredVotes < 0) {
            requiredVotes = 0;
        }

        if (mRequest.getVotes() >= requiredVotes) {
            // if we have enough votes for a 'success', then this vote passes.
            onVotePassed(ctrl);
        } else if (mRequest.getVotes() <= -requiredVotes) {
            // if we have enough negative votes for a 'failure' then this vote fails.
            onVoteFailed(ctrl);
        }
    }

    protected void onVotePassed(AllianceController ctrl) throws Exception {
        mRequest.setState(AllianceRequest.RequestState.ACCEPTED);

        String sql = "UPDATE alliance_requests SET state = ? WHERE id = ?";
        try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
            stmt.setInt(1, mRequest.getState().getNumber());
            stmt.setInt(2, mRequest.getID());
            stmt.update();
        }
    }

    protected void onVoteFailed(AllianceController ctrl) throws Exception {
        mRequest.setState(AllianceRequest.RequestState.REJECTED);

        String sql = "UPDATE alliance_requests SET state = ? WHERE id = ?";
        try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
            stmt.setInt(1, mRequest.getState().getNumber());
            stmt.setInt(2, mRequest.getID());
            stmt.update();
        }
    }

    /**
     * Given that you can't vote for your own requests, what's the maximum number of votes this
     * alliance can cast?
     */
    protected int getTotalPossibleVotes() {
        TreeSet<Integer> excludingEmpires = new TreeSet<Integer>(
                Arrays.asList(new Integer[] {mRequest.getRequestEmpireID()}));
        if (mRequest.getTargetEmpireID() != null) {
            excludingEmpires.add(mRequest.getTargetEmpireID());
        }
        return mAlliance.getTotalPossibleVotes(excludingEmpires);
    }

    private static class JoinRequestProcessor extends AllianceRequestProcessor {
        public JoinRequestProcessor(Alliance alliance, AllianceRequest request) {
            super(alliance, request);
        }

        @Override
        protected void onVotePassed(AllianceController ctrl) throws Exception {
            super.onVotePassed(ctrl);

            String sql = "UPDATE empires SET alliance_id = ?, alliance_rank = ? WHERE id = ?";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setInt(1, mRequest.getAllianceID());
                stmt.setInt(2, AllianceMember.Rank.MEMBER.getNumber());
                stmt.setInt(3, mRequest.getRequestEmpireID());
                stmt.update();
            }

            // TODO: send a notification
        }
    }

    private static class LeaveRequestProcessor extends AllianceRequestProcessor {
        public LeaveRequestProcessor(Alliance alliance, AllianceRequest request) {
            super(alliance, request);
        }

        @Override
        protected void onVotePassed(AllianceController ctrl) throws Exception {
            super.onVotePassed(ctrl);

            String sql = "UPDATE empires SET alliance_id = NULL, alliance_rank = NULL WHERE id = ?";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setInt(1, mRequest.getRequestEmpireID());
                stmt.update();
            }

            // TODO: send a notification
        }
    }

    private static class KickRequestProcessor extends AllianceRequestProcessor {
        public KickRequestProcessor(Alliance alliance, AllianceRequest request) {
            super(alliance, request);
        }

        @Override
        protected void onVotePassed(AllianceController ctrl) throws Exception {
            super.onVotePassed(ctrl);

            String sql = "UPDATE empires SET alliance_id = NULL, alliance_rank = NULL WHERE id = ?";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setInt(1, mRequest.getTargetEmpireID());
                stmt.update();
            }

            // TODO: send a notification
        }
    }

    private static class DepositCashRequestProcessor extends AllianceRequestProcessor {
        public DepositCashRequestProcessor(Alliance alliance, AllianceRequest request) {
            super(alliance, request);
        }

        @Override
        protected void onVotePassed(AllianceController ctrl) throws Exception {
            super.onVotePassed(ctrl);

            Messages.CashAuditRecord.Builder audit_record_pb = Messages.CashAuditRecord.newBuilder()
                    .setEmpireId(mRequest.getRequestEmpireID())
                    .setReason(Messages.CashAuditRecord.Reason.AllianceWithdraw);
            if (!new EmpireController(ctrl.getDB().getTransaction()).adjustBalance(
                    mRequest.getRequestEmpireID(), -mRequest.getAmount(), audit_record_pb)) {
                // if the empire didn't have enough cash, then don't proceed...
                return;
            }

            String sql = "UPDATE alliances SET bank_balance = bank_balance + ? WHERE id = ?";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setDouble(1, (double) mRequest.getAmount());
                stmt.setInt(2, mRequest.getAllianceID());
                stmt.update();
            }

            sql = "INSERT INTO alliance_bank_balance_audit (alliance_id, alliance_request_id," +
                     " empire_id, date, amount_before, amount_after) VALUES (?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setInt(1, mRequest.getAllianceID());
                stmt.setInt(2, mRequest.getID());
                stmt.setInt(3, mRequest.getRequestEmpireID());
                stmt.setDateTime(4, DateTime.now());
                stmt.setDouble(5, mAlliance.getBankBalance());
                stmt.setDouble(6, mAlliance.getBankBalance() + mRequest.getAmount());
                stmt.update();
            }
        }
    }

    private static class WithdrawCashRequestProcessor extends AllianceRequestProcessor {
        public WithdrawCashRequestProcessor(Alliance alliance, AllianceRequest request) {
            super(alliance, request);
        }

        @Override
        protected void onVotePassed(AllianceController ctrl) throws Exception {
            super.onVotePassed(ctrl);

            String sql = "UPDATE alliances SET bank_balance = bank_balance - ? WHERE id = ? AND bank_balance > ?";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setDouble(1, (double) mRequest.getAmount());
                stmt.setInt(2, mRequest.getAllianceID());
                stmt.setDouble(3, (double) mRequest.getAmount());
                if (stmt.update() == 0) {
                    // if we didn't update the row, it means there wasn't enough balance anyway...
                    return;
                }
            }

            Messages.CashAuditRecord.Builder audit_record_pb = Messages.CashAuditRecord.newBuilder()
                    .setEmpireId(mRequest.getRequestEmpireID())
                    .setReason(Messages.CashAuditRecord.Reason.AllianceWithdraw);
            new EmpireController(ctrl.getDB().getTransaction()).adjustBalance(mRequest.getRequestEmpireID(), mRequest.getAmount(), audit_record_pb);

            sql = "INSERT INTO alliance_bank_balance_audit (alliance_id, alliance_request_id," +
                     " empire_id, date, amount_before, amount_after) VALUES (?, ?, ?, ?, ?, ?)";
            try (SqlStmt stmt = ctrl.getDB().prepare(sql)) {
                stmt.setInt(1, mRequest.getAllianceID());
                stmt.setInt(2, mRequest.getID());
                stmt.setInt(3, mRequest.getRequestEmpireID());
                stmt.setDateTime(4, DateTime.now());
                stmt.setDouble(5, mAlliance.getBankBalance());
                stmt.setDouble(6, mAlliance.getBankBalance() - mRequest.getAmount());
                stmt.update();
            }
        }
    }
}
