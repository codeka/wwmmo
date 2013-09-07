package au.com.codeka.warworlds.model;

import au.com.codeka.common.Cash;
import au.com.codeka.common.model.AllianceRequest;

public class AllianceRequestHelper {
    /**
     * Returns a user-readable description of this request.
     */
    public static String getDescription(AllianceRequest request) {
        switch (request.request_type) {
        case JOIN:
            return "Join";
        case LEAVE:
            return "Leave";
        case KICK:
            return "KICK";
        case DEPOSIT_CASH:
            return String.format("Despoit %s", Cash.format(request.amount));
        case WITHDRAW_CASH:
            return String.format("Withdraw: %s", Cash.format(request.amount));
        default:
            return "Unknown!";
        }
    }
}
