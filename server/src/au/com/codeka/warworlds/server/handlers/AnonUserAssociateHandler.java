package au.com.codeka.warworlds.server.handlers;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;

/**
 * Handles the .../anon-associate URL which lets an anonymous user associate their empire with a
 * real user account.
 */
public class AnonUserAssociateHandler extends RequestHandler {
  @Override
  public void post() throws RequestException {
    Messages.AnonUserAssociate pb = getRequestBody(Messages.AnonUserAssociate.class);

    // Check to make sure there's not already an empire with that email address.
    if (new EmpireController().getEmpireByEmail(pb.getUserEmail()) != null) {
      throw new RequestException(400, Messages.GenericError.ErrorCode.EmpireNameExists,
          "An empire is already associated with that account, you cannot associate this empire "
              + "with the same account.");
    }

    // Make sure the current empire is anonymous
    if (!getSession().getActualEmail().endsWith("@anon.war-worlds.com")) {
      throw new RequestException(400, Messages.GenericError.ErrorCode.NotAnonymous,
          "Your empire is already associated with an account, and cannot be associated again.");
    }

    new EmpireController().associateEmpire(getSession().getEmpireID(), pb.getUserEmail());
  }
}
