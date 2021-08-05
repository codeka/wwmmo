package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.proto.PatreonBeginRequest
import au.com.codeka.warworlds.common.proto.PatreonBeginResponse
import au.com.codeka.warworlds.server.Configuration
import au.com.codeka.warworlds.server.Configuration.PatreonConfig
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.html.AuthenticatedRequestHandler
import com.google.common.io.BaseEncoding

class PatreonBeginHandler : AuthenticatedRequestHandler() {
  public override fun post() {
    val req = readProtobuf(PatreonBeginRequest::class.java)
    if (req.empire_id != authenticatedEmpire!!.get().id) {
      throw RequestException(400, "Empire ID does not match.")
    }

    // We currently don't save this as a "begun" patreon request.. don't really care.
    val config: PatreonConfig = Configuration.i.patreon
        ?: throw RequestException(500, "Patreon not configured.")
    writeProtobuf(PatreonBeginResponse(
        client_id = config.clientId!!,
        redirect_uri = config.redirectUri!!,
        state = BaseEncoding.base64().encode(req.encode())))
  }
}
