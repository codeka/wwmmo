package au.com.codeka.warworlds.server

import au.com.codeka.warworlds.common.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.collect.Lists
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import java.io.ByteArrayInputStream
import java.io.FileReader
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

/**
 * The server's configuration parameters are read from a json object on startup and parsed into
 * this object via Gson.
 */
class Configuration private constructor() {
  @Expose
  val baseUrl: String? = null

  @Expose
  val listenPort = 0

  @Expose
  val smtp: SmtpConfig

  @Expose
  private val firebase: JsonElement? = null

  @Expose
  val patreon: PatreonConfig? = null

  @Expose
  val limits: LimitsConfig? = null
  private var firebaseCredentials: GoogleCredentials? = null

  /** Loads the [Configuration] from the given file and sets it to `Configuration.i`.  */
  @Throws(IOException::class)
  fun load() {
    var fileName = System.getProperty("ConfigFile")
    if (fileName == null) {
      // just try whatever in the current directory
      fileName = "config.json"
    }
    log.info("Loading config from: %s", fileName)
    val gson = GsonBuilder()
        .registerTypeAdapter(Configuration::class.java, InstanceCreator { type: Type? -> i } as InstanceCreator<Configuration>)
        .create()
    val jsonReader = JsonReader(FileReader(fileName))
    jsonReader.isLenient = true // allow comments (and a few other things)
    gson.fromJson<Any>(jsonReader, Configuration::class.java)
  }

  fun getFirebaseCredentials(): GoogleCredentials? {
    try {
      if (firebaseCredentials == null) {
        try {
          firebaseCredentials = GoogleCredentials.fromStream(
              ByteArrayInputStream(firebase.toString().toByteArray(StandardCharsets.UTF_8)))
              .createScoped(FIREBASE_SCOPES)
        } catch (e: UnsupportedEncodingException) {
          // Should never happen.
        }
      }
      firebaseCredentials!!.refreshIfExpired()
    } catch (e: IOException) {
      throw RuntimeException("Should never happen.", e)
    }
    return firebaseCredentials
  }

  class SmtpConfig {
    @Expose
    val host = "smtp-relay.gmail.com"

    @Expose
    val port = 587

    @Expose
    val userName: String? = null

    @Expose
    val password: String? = null

    @Expose
    val senderAddr = "noreply@war-worlds.com"

  }

  class PatreonConfig {
    @Expose
    val clientId: String? = null

    @Expose
    val clientSecret: String? = null

    @Expose
    val redirectUri: String? = null

  }

  class LimitsConfig {
    @Expose
    val maxEmpireNameLength = 0
  }

  companion object {
    val i = Configuration()
    private val log = Log("Configuration")
    private val FIREBASE_SCOPES: Collection<String> = Lists.newArrayList("https://www.googleapis.com/auth/firebase.messaging")
  }

  init {
    smtp = SmtpConfig()
  }
}