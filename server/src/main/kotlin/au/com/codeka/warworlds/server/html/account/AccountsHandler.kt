package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.NewAccountRequest
import au.com.codeka.warworlds.common.proto.NewAccountResponse
import au.com.codeka.warworlds.server.Configuration
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.util.CookieHelper
import au.com.codeka.warworlds.server.util.NameValidator
import au.com.codeka.warworlds.server.world.EmpireManager
import au.com.codeka.warworlds.server.world.WatchableObject

/** Accounts servlet for creating new accounts on the server.  */
class AccountsHandler : ProtobufRequestHandler() {
  private val log = Log("EmpiresHandler")

  @Throws(RequestException::class)
  public override fun post() {
    val req = readProtobuf(NewAccountRequest::class.java)
    log.info("Creating new account: %s", req.empire_name)

    // Make sure the name is valid, unique, etc etc.
    if (req.empire_name.trim { it <= ' ' } == "") {
      writeProtobuf(
          NewAccountResponse.Builder()
              .message("You must give your empire a name.")
              .build())
      return
    }
    val nameStatus = NameValidator.validate(
        req.empire_name,
        Configuration.i.limits!!.maxEmpireNameLength)
    if (!nameStatus.isValid || nameStatus.name == null) {
      writeProtobuf(
          NewAccountResponse.Builder()
              .message(nameStatus.errorMsg)
              .build())
      return
    }
    val existingEmpires: List<WatchableObject<Empire>?> = EmpireManager.i.search(nameStatus.name)
    // The parameter to search is a query, so it'll find non-exact matches, but that's all we care
    // about, so we'll have to check manually.
    for (existingEmpire in existingEmpires) {
      if (existingEmpire!!.get().display_name.compareTo(nameStatus.name!!, ignoreCase = true) == 0) {
        writeProtobuf(
            NewAccountResponse.Builder()
                .message("An empire with that name already exists.")
                .build())
        return
      }
    }

    // Generate a cookie for the user to authenticate with in the future.
    val cookie = CookieHelper.generateCookie()

    // Create the empire itself.
    val empire: WatchableObject<Empire>? = EmpireManager.i.createEmpire(nameStatus.name)
    if (empire == null) {
      // Some kind of unexpected error creating the empire.
      writeProtobuf(
          NewAccountResponse.Builder()
              .message("An error occurred while creating your empire, please try again.")
              .build())
      return
    }

    // Make a new account with all the details.
    val acct = Account.Builder()
        .empire_id(empire.get().id)
        .build()
    DataStore.i.accounts().put(cookie, acct)
    writeProtobuf(
        NewAccountResponse.Builder()
            .cookie(cookie)
            .build())
  }
}