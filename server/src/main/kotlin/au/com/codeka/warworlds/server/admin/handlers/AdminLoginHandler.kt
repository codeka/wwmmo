package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.server.admin.SessionManager
import au.com.codeka.warworlds.server.handlers.RequestException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.JsonParser
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*
import javax.servlet.http.Cookie

class AdminLoginHandler : AdminHandler() {
  companion object {
    private val log = Log("AdminLoginHandler")
    private const val CLIENT_ID =
        "809181406384-obrefe4qb0hmaektbesqsc25rop0u72f.apps.googleusercontent.com"
  }

  /** We don't require any roles, because we're creating a session.  */
  override val requiredRoles: Collection<AdminRole>?
    get() = null

  override fun get() {
    val data = HashMap<String, Any>()
    data["client_id"] = CLIENT_ID
    render("login.html", data)
  }

  override fun post() {
    val emailAddr: String
    try {
      val authResult = request.getParameter("auth-result")
      val json = JsonParser.parseString(authResult).asJsonObject
      val idToken = json["id_token"].asString
      val parser = TokenParser(arrayOf(CLIENT_ID), CLIENT_ID)
      val payload = parser.parse(idToken) ?: throw RequestException(500, parser.problem())
      emailAddr = payload.email
      if (emailAddr == null) {
        var entries: String? = null
        for ((key, value) in payload) {
          if (entries == null) {
            entries = ""
          } else {
            entries += ", "
          }
          entries += String.format("%s = %s", key, value)
        }
        entries += """
          $authResult
          """.trimIndent()
        throw RequestException(500, "No email address: $entries")
      }
    } catch (e: Exception) {
      throw RequestException(e)
    }
    val session = SessionManager.i.authenticate(emailAddr)
    if (session == null) {
      // not a valid user
      redirect("/")
      return
    }
    log.info("Got cookie: %s for %s", session.cookie, emailAddr)
    val cookie = Cookie("SESSION", session.cookie)
    cookie.isHttpOnly = true
    cookie.path = "/admin"
    var continueUrl = request.getParameter("continue")
    if (continueUrl == null) {
      continueUrl = "/admin"
    }
    log.info("Continuing to: %s", continueUrl)
    response.addCookie(cookie)
    response.status = 302
    response.setHeader("Location", continueUrl)
  }

  inner class TokenParser(private val clientIDs: Array<String>, private val audience: String) {
    private val jsonFactory = GsonFactory()
    private val verifier = GoogleIdTokenVerifier(NetHttpTransport(), jsonFactory)
    private var problem = "Verification failed. (Time-out?)"

    fun parse(tokenString: String?): GoogleIdToken.Payload? {
      var payload: GoogleIdToken.Payload? = null
      try {
        val token = GoogleIdToken.parse(jsonFactory, tokenString)
        if (verifier.verify(token)) {
          val tempPayload = token.payload
          if (tempPayload.audience != audience)
            problem = "Audience mismatch, " + audience + " != " + tempPayload.audience
          else if (!clientIDs.contains(tempPayload.authorizedParty))
            problem = "Client ID mismatch"
          else
            payload = tempPayload
        }
      } catch (e: GeneralSecurityException) {
        problem = "Security issue: " + e.localizedMessage
      } catch (e: IOException) {
        problem = "Network problem: " + e.localizedMessage
      }
      return payload
    }

    fun problem(): String {
      return problem
    }
  }
}