package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.LoginRequest
import au.com.codeka.warworlds.common.proto.LoginResponse
import au.com.codeka.warworlds.common.sim.DesignDefinitions
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler
import au.com.codeka.warworlds.server.net.ServerSocketManager
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.EmpireManager
import com.google.common.base.Strings

/**
 * This servlet is posted to in order to "log in". You'll get a pointer to the socket to connect
 * to for the actual main game connection.
 */
class LoginHandler : ProtobufRequestHandler() {
  private val log = Log("LoginHandler")

  public override fun post() {
    val req = readProtobuf(LoginRequest::class.java)
    if (Strings.isNullOrEmpty(req.cookie)) {
      log.warning("No cookie in request, not connected.")
      response.status = 403
      return
    }
    log.info("Login request received, cookie=%s", req.cookie)

    val account = DataStore.i.accounts()[req.cookie]
    if (account == null) {
      log.warning("No account for cookie, not connecting: %s", req.cookie)
      response.status = 401
      return
    }

    val empire = EmpireManager.i.getEmpire(account.empire_id)
    if (empire == null) {
      log.warning("No empire with ID %d", account.empire_id)
      response.status = 404
      return
    }

    // If they've given us an id_token, then verify that it's valid and that it's associated with
    // this empire.
    if (req.id_token != null) {
      val tokenInfo = TokenVerifier.verify(req.id_token)
      if (tokenInfo.email != account.email) {
        log.warning("User us signed in to an email address that is not associated with this " +
            " account. (token email=${tokenInfo.email}, account email=${account.email})")
        response.status = 400
        return
      }

      log.info("Account for \"${empire.get().display_name}\", email \"${account.email}\" " +
          "successfully authenticated (${tokenInfo.displayName}).")
    } else if (account.email_status == Account.EmailStatus.VERIFIED) {
      // If the account has an email address associated with it, and the user isn't signed in with
      // that email address, then that's a problem.
      log.warning("Account is associated with email address ${account.email} but user is not " +
          "signed in with that email address.")
      response.status = 401
      return
    } else {
      log.info("Account for \"${empire.get().display_name}\" is anonymous.")
    }

    DataStore.i.empires().saveDevice(empire.get(), req.device_info)
    DataStore.i.stats().addLoginEvent(req, account)
    val resp = LoginResponse.Builder().status(LoginResponse.LoginStatus.SUCCESS)
    // Tell the server socket to expect a connection from this client.
    ServerSocketManager.i.addPendingConnection(account, empire, null /* encryptionKey */)
    resp
        .port(8081)
        .empire(empire.get())
        .designs(DesignDefinitions.designs)
    writeProtobuf(resp.build())
  }
}
