package au.com.codeka.warworlds.model;

import au.com.codeka.Cash;
import au.com.codeka.common.model.BaseAllianceRequest;

public class AllianceRequest extends BaseAllianceRequest {
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
            return String.format("Withdraw: %s", Cash.format(mAmount));
        default:
            return "Unknown!";
        }
    }
}
