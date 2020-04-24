package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.common.proto.PatreonBeginRequest
import au.com.codeka.warworlds.server.Configuration
import au.com.codeka.warworlds.server.Configuration.PatreonConfig
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.proto.PatreonInfo
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.world.EmpireManager
import com.google.common.io.BaseEncoding
import com.patreon.PatreonOAuth

class ConnectToPatreonHandler : ProtobufRequestHandler() {
  /**
   * Handler for where the client gets redirected to after successfully doing the oauth handshake.
   */
  public override fun get() {
    val code = request.getParameter("code")
    val state = request.getParameter("state")
    val patreonConfig: PatreonConfig = Configuration.i.patreon
        ?: throw RequestException(500, "Patreon not configured.")
    val oauthClient =
        PatreonOAuth(patreonConfig.clientId, patreonConfig.clientSecret, patreonConfig.redirectUri)
    val tokens = oauthClient.getTokens(code)
    val req = PatreonBeginRequest.ADAPTER.decode(BaseEncoding.base64().decode(state))

    // Set up an empty PatreonInfo, that we'll then populate.
    val patreonInfo: PatreonInfo = PatreonInfo.Builder()
        .empire_id(req.empire_id)
        .access_token(tokens.accessToken)
        .refresh_token(tokens.refreshToken)
        .token_expiry_time(tokens.expiresIn.toLong())
        .token_type(tokens.tokenType)
        .token_scope(tokens.scope)
        .build()
    DataStore.i.empires().savePatreonInfo(req.empire_id, patreonInfo)
    val empire = EmpireManager.i.getEmpire(req.empire_id)
    EmpireManager.i.refreshPatreonInfo(empire, patreonInfo)
  }
}
