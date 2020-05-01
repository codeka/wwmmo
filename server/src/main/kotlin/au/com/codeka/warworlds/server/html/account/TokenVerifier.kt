package au.com.codeka.warworlds.server.html.account

import au.com.codeka.warworlds.server.handlers.RequestException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

// TODO: make this configurable.
const val DEBUG_CLIENT_ID
    = "809181406384-e5srtrrj32pt0v0kuhp0f4ife1jgd8go.apps.googleusercontent.com"

data class TokenInfo(private val idToken: GoogleIdToken) {
  val email: String
    get() = idToken.payload.email

  val audience: String
    get() = idToken.payload.audienceAsList.joinToString()

  val displayName: String
    get() = idToken.payload["name"] as String

  val pictureUrl: String
    get() = idToken.payload["picture"] as String
}

object TokenVerifier {
  fun verify(idTokenStr: String): TokenInfo {
    val transport = NetHttpTransport()
    val jsonFactory = GsonFactory()

    val idTokenVerifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
   //     .setAudience(Collections.singletonList(DEBUG_CLIENT_ID))
        .build()
    val idToken = idTokenVerifier.verify(idTokenStr)
        ?: throw RequestException(400, "Token invalid")
    return TokenInfo(idToken)
  }
}
